import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react';
import { api as defaultApi, usingMockData } from './api';
import { ApiError, type AccountDetail, type AccountSummary, type PortfolioApi } from './api/types';
import { Overview } from './components/Overview';
import { Positions } from './components/Positions';
import { SearchIcon } from './components/shared';
import { Transactions } from './components/Transactions';
import { formatAccountNo, parseAccountInput, resolveAccount } from './lib/account';
import { formatDate } from './lib/format';

const TABS = [
  { id: 'overview', label: 'Overview', key: '1' },
  { id: 'positions', label: 'Positions', key: '2' },
  { id: 'transactions', label: 'Transactions', key: '3' },
] as const;
type Tab = (typeof TABS)[number]['id'];
type Theme = 'light' | 'dark';
const THEME_KEY = 'portfolio-desk.theme';

const tabFromHash = (): Tab => {
  const h = window.location.hash.replace('#', '');
  return TABS.some((t) => t.id === h) ? (h as Tab) : 'overview';
};

type View =
  | { status: 'loading'; accountNo?: string }
  | { status: 'ready'; account: AccountDetail }
  | { status: 'error'; accountNo: string; error: ApiError };

function initialTheme(): Theme {
  const saved = localStorage.getItem(THEME_KEY);
  if (saved === 'light' || saved === 'dark') return saved;
  return typeof window.matchMedia === 'function' && window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

export default function App({ api = defaultApi }: { api?: PortfolioApi }) {
  const [accounts, setAccounts] = useState<AccountSummary[]>([]);
  const [view, setView] = useState<View>({ status: 'loading' });
  const [tab, setTabState] = useState<Tab>(tabFromHash);
  const [input, setInput] = useState('');
  const [lookupMsg, setLookupMsg] = useState<string | null>(null);
  const [theme, setTheme] = useState<Theme>(initialTheme);
  const requestId = useRef(0);
  const lookupRef = useRef<HTMLInputElement>(null);
  const posSearch = useRef<HTMLInputElement>(null);
  const txSearch = useRef<HTMLInputElement>(null);

  const setTab = useCallback((t: Tab) => {
    setTabState(t);
    if (window.location.hash !== `#${t}`) window.history.replaceState(null, '', `#${t}`);
  }, []);

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
  }, [theme]);

  const load = useCallback(async (accountNo: string, fromLookup = false) => {
    const id = ++requestId.current;
    setLookupMsg(null);
    setView((v) => (fromLookup && v.status === 'ready' ? v : { status: 'loading', accountNo }));
    try {
      const account = await api.getAccount(accountNo);
      if (id !== requestId.current) return;
      setView({ status: 'ready', account });
      setInput('');
    } catch (e) {
      if (id !== requestId.current) return;
      const error = e instanceof ApiError ? e : new ApiError('SYSTEM', 'Unexpected error loading account', 'UNKNOWN');
      if (error.kind === 'NOT_FOUND') {
        setLookupMsg(`No account found for ${formatAccountNo(accountNo)}.`);
        setView((v) => (v.status === 'loading' && !fromLookup ? { status: 'error', accountNo, error } : v));
      } else {
        setView({ status: 'error', accountNo, error });
      }
    }
  }, [api]);

  useEffect(() => {
    let cancelled = false;
    api.listAccounts().then(
      (list) => {
        if (cancelled) return;
        setAccounts(list);
        if (list[0]) load(list[0].accountNo);
        else setView({ status: 'error', accountNo: '', error: new ApiError('NOT_FOUND', 'No accounts available') });
      },
      () => !cancelled && setView({ status: 'error', accountNo: '', error: new ApiError('SYSTEM', 'Unable to load accounts', 'ACCOUNTS') }),
    );
    return () => { cancelled = true; };
  }, [api, load]);

  useEffect(() => {
    const onHash = () => setTabState(tabFromHash());
    window.addEventListener('hashchange', onHash);
    return () => window.removeEventListener('hashchange', onHash);
  }, []);

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const el = e.target as HTMLElement;
      if (el.closest('input,select,textarea')) {
        if (e.key === 'Escape') (el as HTMLElement).blur();
        return;
      }
      if (e.metaKey || e.ctrlKey || e.altKey) return;
      const t = TABS.find((x) => x.key === e.key);
      if (t) { setTab(t.id); return; }
      if (e.key === '/') {
        e.preventDefault();
        (tab === 'positions' ? posSearch.current : tab === 'transactions' ? txSearch.current : lookupRef.current)?.focus();
      }
    };
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [tab, setTab]);

  function onLookup(e: FormEvent) {
    e.preventDefault();
    const parsed = parseAccountInput(input);
    if (!parsed.ok) { setLookupMsg(parsed.message); return; }
    const resolved = resolveAccount(parsed.digits, accounts.map((a) => a.accountNo));
    if (!resolved) { setLookupMsg('Account numbers are 10 digits, like 4471-0028-93.'); return; }
    load(resolved, true);
  }

  const account = view.status === 'ready' ? view.account : null;
  const selectedNo = view.status === 'ready' ? view.account.accountNo : view.accountNo;

  return (
    <>
      <header className="top">
        <div className="wrap">
          <div className="top-row">
            <div className="brand"><span className="mark">PFL</span>Portfolio Desk</div>
            <button className="btn theme-toggle" type="button" aria-label={`Switch to ${theme === 'dark' ? 'light' : 'dark'} theme`}
              onClick={() => setTheme((t) => { const n = t === 'dark' ? 'light' : 'dark'; localStorage.setItem(THEME_KEY, n); return n; })}>
              {theme === 'dark' ? '☀' : '☾'}
            </button>
            <form className="lookup" onSubmit={onLookup} autoComplete="off" noValidate>
              <label htmlFor="acct-input">Account number</label>
              <div className="field">
                <SearchIcon />
                <input
                  id="acct-input" ref={lookupRef} list="acct-list" inputMode="numeric" value={input}
                  placeholder="Account number, e.g. 4471-0055-12"
                  aria-invalid={lookupMsg ? true : undefined} aria-describedby={lookupMsg ? 'lookup-msg' : undefined}
                  onChange={(e) => { setInput(e.target.value); setLookupMsg(null); }}
                />
                <datalist id="acct-list">
                  {accounts.map((a) => <option key={a.accountNo} value={formatAccountNo(a.accountNo)}>{a.name}</option>)}
                </datalist>
              </div>
              <button className="btn primary" type="submit">Look up</button>
              {lookupMsg && <div className="lookup-msg" id="lookup-msg" role="alert">{lookupMsg}</div>}
            </form>
          </div>
          <nav className="tabs" role="tablist" aria-label="Screens">
            {TABS.map((t) => (
              <button key={t.id} role="tab" id={`tab-${t.id}`} aria-controls={`view-${t.id}`} aria-selected={tab === t.id}
                tabIndex={tab === t.id ? 0 : -1} type="button" onClick={() => setTab(t.id)}
                onKeyDown={(e) => {
                  if (e.key !== 'ArrowRight' && e.key !== 'ArrowLeft') return;
                  const i = TABS.findIndex((x) => x.id === tab);
                  const next = TABS[(i + (e.key === 'ArrowRight' ? 1 : TABS.length - 1)) % TABS.length];
                  setTab(next.id);
                  document.getElementById(`tab-${next.id}`)?.focus();
                }}>
                {t.label} <kbd>{t.key}</kbd>
              </button>
            ))}
          </nav>
        </div>
      </header>
      <div className="wrap">
        <main>
          <section className="acct-head" aria-live="polite">
            <div>
              {account ? (
                <>
                  <div className="eyebrow">{account.accountType}</div>
                  <h1>{account.name}</h1>
                  <div className="acct-meta">
                    <span>Account <span className="mono">{formatAccountNo(account.accountNo)}</span></span>
                    <span>Opened {formatDate(account.openedDate)}</span>
                    <span>Prices as of {formatDate(account.asOfDate)}</span>
                  </div>
                </>
              ) : (
                <h1>{view.status === 'loading' ? 'Loading account…' : 'Account unavailable'}</h1>
              )}
            </div>
            {accounts.length > 0 && (
              <div className="switch" role="group" aria-label="Sample accounts">
                {accounts.map((a) => (
                  <button key={a.accountNo} type="button" className="chip" aria-pressed={selectedNo === a.accountNo} onClick={() => load(a.accountNo)}>
                    {a.name.split(' ')[0]} ·{a.accountNo.slice(-4)}
                  </button>
                ))}
              </div>
            )}
          </section>

          {view.status === 'loading' && (
            <div className="panel state" role="status"><div className="spinner" aria-hidden="true" />Loading portfolio…</div>
          )}
          {view.status === 'error' && <ErrorPanel accountNo={view.accountNo} error={view.error} onRetry={() => view.accountNo && load(view.accountNo)} />}
          {account && (
            <div key={account.accountNo} style={{ display: 'contents' }}>
              <section id="view-overview" role="tabpanel" aria-labelledby="tab-overview" hidden={tab !== 'overview'}>
                <Overview account={account} onViewTransactions={() => setTab('transactions')} />
              </section>
              <section id="view-positions" role="tabpanel" aria-labelledby="tab-positions" hidden={tab !== 'positions'}>
                <Positions account={account} searchRef={posSearch} />
              </section>
              <section id="view-transactions" role="tabpanel" aria-labelledby="tab-transactions" hidden={tab !== 'transactions'}>
                <Transactions account={account} searchRef={txSearch} active={tab === 'transactions'} />
              </section>
            </div>
          )}
        </main>
        <footer>
          {usingMockData && <span>Sample data for development. Funds and accounts are fictional.</span>}
          <span>Shortcuts: <kbd>1</kbd><kbd>2</kbd><kbd>3</kbd> screens · <kbd>/</kbd> search · <kbd>[</kbd><kbd>]</kbd> page</span>
          <span className="mono">Replaces CICS PINQ · MENMAP / POSMAP / HISMAP</span>
        </footer>
      </div>
    </>
  );
}

function ErrorPanel({ accountNo, error, onRetry }: { accountNo: string; error: ApiError; onRetry: () => void }) {
  if (error.kind === 'ACCESS_DENIED') {
    return (
      <div className="panel state error" role="alert">
        <h2>Access denied</h2>
        <p>You are not authorized to view account {formatAccountNo(accountNo)}. Contact your administrator if you need access.</p>
      </div>
    );
  }
  if (error.kind === 'NOT_FOUND') {
    return (
      <div className="panel state error" role="alert">
        <h2>Account not found</h2>
        <p>{error.message}. Check the number and try again.</p>
      </div>
    );
  }
  return (
    <div className="panel state error" role="alert">
      <h2>System error</h2>
      <p>The portfolio service could not complete this request. No data was changed.</p>
      <dl className="errgrid">
        <dt>Error code</dt><dd className="mono">{error.code ?? '—'}</dd>
        <dt>Details</dt><dd>{error.message}</dd>
        <dt>Trace ID</dt><dd className="mono">{error.traceId ?? '—'}</dd>
      </dl>
      <button className="btn primary" type="button" onClick={onRetry}>Retry</button>
    </div>
  );
}
