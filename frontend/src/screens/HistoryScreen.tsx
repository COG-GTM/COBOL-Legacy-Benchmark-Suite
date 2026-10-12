import { Fragment, useCallback, useEffect, useRef, useState } from 'react';
import { Link, Navigate, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import type { Transaction, TransactionPage } from '../api/types';
import { AccountSearchForm } from '../components/AccountSearchForm';
import { Loading } from '../components/Loading';
import { MessageRegion, type Message } from '../components/MessageRegion';
import { PageHeading } from '../components/PageHeading';
import { Pager } from '../components/Pager';
import { totalPagesFor } from '../state/paging';
import { ScreenNav, ShortcutsHint } from '../components/ScreenNav';
import { formatIsoDate, formatTime } from '../format/date';
import { formatHistoryQuantity, formatMoney } from '../format/decimal';
import { transactionTypeLabel } from '../format/labels';
import { messages } from '../i18n/messages';
import { useSession } from '../state/SessionContext';
import { useApiErrorRouting } from '../state/useApiErrorRouting';
import { useApiQuery } from '../state/useApiQuery';
import { useDocumentTitle } from '../state/useDocumentTitle';
import { normaliseAccountNo, parsePageParam, validateAccountNo } from '../state/validation';

const M = messages.history;
const COLUMN_COUNT = 6;

function TransactionDetails({ t, id }: { t: Transaction; id: string }) {
  const items: Array<[string, string, boolean?]> = [
    [M.time, formatTime(t.tradeTime)],
    [M.securityId, t.securityId],
    [M.portfolioId, t.portfolioId],
    [M.fees, formatMoney(t.fees), true],
    [M.totalAmount, formatMoney(t.totalAmount), true],
    [M.costBasis, formatMoney(t.costBasis), true],
    [M.gainLoss, formatMoney(t.gainLoss), true],
  ];
  return (
    <dl className="detail-grid detail-grid--compact" id={id}>
      {items.map(([label, value, num]) => (
        <div key={label} className="detail-grid__item">
          <dt>{label}</dt>
          <dd className={num ? 'num' : 'mono'}>{value}</dd>
        </div>
      ))}
    </dl>
  );
}

function HistoryTable({ accountNo, items }: { accountNo: string; items: Transaction[] }) {
  // Transactions have no unique ID, so expansion is keyed by row index (the table remounts per page).
  const [expanded, setExpanded] = useState<Set<number>>(new Set());
  const toggle = (key: number) =>
    setExpanded((prev) => {
      const next = new Set(prev);
      if (next.has(key)) next.delete(key);
      else next.add(key);
      return next;
    });

  return (
    <div className="table-wrap table-wrap--sticky" tabIndex={0} role="region" aria-labelledby="history-caption">
      <table className="table" data-testid="history-table">
        <caption id="history-caption">{M.tableCaption(accountNo)}</caption>
        <thead>
          <tr>
            <th scope="col">{M.colDate}</th>
            <th scope="col">{M.colType}</th>
            <th scope="col" className="num">{M.colUnits}</th>
            <th scope="col" className="num">{M.colPrice}</th>
            <th scope="col" className="num">{M.colAmount}</th>
            <th scope="col"><span className="visually-hidden">{M.colDetails}</span></th>
          </tr>
        </thead>
        <tbody>
          {items.map((t, index) => {
            const key = index;
            const isOpen = expanded.has(key);
            const detailId = `txn-detail-${index}`;
            const typeLabel = transactionTypeLabel(t.type);
            return (
              <Fragment key={key}>
                <tr data-testid="history-row">
                  <th scope="row">{formatIsoDate(t.tradeDate)}</th>
                  <td>{typeLabel}</td>
                  <td className="num">{formatHistoryQuantity(t.units)}</td>
                  <td className="num">{formatHistoryQuantity(t.price)}</td>
                  <td className="num">{formatMoney(t.amount)}</td>
                  <td className="actions">
                    <button
                      type="button"
                      className="btn btn--small btn--ghost"
                      aria-expanded={isOpen}
                      aria-controls={isOpen ? detailId : undefined}
                      onClick={() => toggle(key)}
                    >
                      {isOpen ? M.hideDetails : M.showDetails}
                      <span className="visually-hidden"> {M.detailsFor(formatIsoDate(t.tradeDate), typeLabel)}</span>
                    </button>
                  </td>
                </tr>
                {isOpen && (
                  <tr className="detail-row">
                    <td colSpan={COLUMN_COUNT}>
                      <TransactionDetails t={t} id={detailId} />
                    </td>
                  </tr>
                )}
              </Fragment>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}

/** S3 Transaction History Inquiry (HISMAP / INQHIST), 10 rows per page, PF7/PF8 paging. */
export function HistoryScreen() {
  useDocumentTitle(M.title);
  const { accountNo: routeAccount } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const { api, selectedAccountNo, setSelectedAccountNo } = useSession();
  const routeError = useApiErrorRouting();
  const inputRef = useRef<HTMLInputElement>(null);

  const page = parsePageParam(searchParams.get('page'));
  const [input, setInput] = useState(routeAccount ?? selectedAccountNo ?? '');
  const [validationError, setValidationError] = useState<string | null>(null);
  const [nonce, setNonce] = useState(0);

  const routeValidation = routeAccount !== undefined ? validateAccountNo(routeAccount) : null;
  const activeAccount = routeAccount !== undefined && !routeValidation ? normaliseAccountNo(routeAccount) : null;

  useEffect(() => {
    if (routeAccount !== undefined) setInput(routeAccount);
    setValidationError(routeValidation);
    if (activeAccount) setSelectedAccountNo(activeAccount);
  }, [routeAccount, routeValidation, activeAccount, setSelectedAccountNo]);

  const fetcher = useCallback(
    (signal: AbortSignal) => api.getTransactions(activeAccount ?? '', page, signal),
    [api, activeAccount, page],
  );
  const { state, retry } = useApiQuery<TransactionPage>(
    activeAccount ? `${activeAccount}|${page}|${nonce}` : null,
    fetcher,
    routeError,
  );

  function submit(raw: string) {
    const error = validateAccountNo(raw);
    if (error) {
      setValidationError(error);
      inputRef.current?.focus();
      return;
    }
    const acct = normaliseAccountNo(raw);
    setValidationError(null);
    setSelectedAccountNo(acct);
    // FR-025: a new search always resets to page 1.
    if (acct === activeAccount && page === 1) setNonce((n) => n + 1);
    else navigate(`/history/${acct}?page=1`);
  }

  const outOfRange = state.status === 'success' && state.data.items.length === 0 && state.data.totalCount > 0;
  const lastPage = state.status === 'success' ? totalPagesFor(state.data.totalCount, state.data.pageSize) : 1;
  useEffect(() => {
    // A stale or mistyped ?page beyond the end is clamped to the last page instead of stranding the user.
    if (outOfRange) setSearchParams({ page: String(lastPage) }, { replace: true });
  }, [outOfRange, lastPage, setSearchParams]);

  // FR-008: arriving without an account re-runs the inquiry for the last valid account.
  if (routeAccount === undefined && selectedAccountNo) {
    return <Navigate to={`/history/${selectedAccountNo}?page=1`} replace />;
  }

  const data = state.status === 'success' ? state.data : null;
  const totalPages = data ? totalPagesFor(data.totalCount, data.pageSize) : 1;
  const goPage = (p: number) => setSearchParams({ page: String(p) });

  let message: Message | null = null;
  let action = null;
  if (validationError) message = { text: validationError, severity: 'WARNING' };
  else if (state.status === 'error') {
    message = {
      text: state.error.message,
      severity: state.error.severity,
      traceId: state.httpStatus >= 500 ? state.error.traceId : undefined,
    };
    if (state.error.code === 'DB_UNAVAILABLE') {
      action = (
        <button type="button" className="btn" onClick={retry}>
          {messages.errors.retry}
        </button>
      );
    }
  } else if (data && data.totalCount === 0) message = { text: messages.errors.noTransactions, severity: 'INFO' };

  return (
    <div className="screen">
      <ScreenNav
        current={M.breadcrumb}
        onPrevious={data?.hasPrevious ? () => goPage(page - 1) : undefined}
        onNext={data?.hasNext ? () => goPage(page + 1) : undefined}
      />
      <section className="card" aria-labelledby="history-title">
        <div id="history-title">
          <PageHeading>{M.title}</PageHeading>
        </div>
        <AccountSearchForm
          ref={inputRef}
          idPrefix="history"
          value={input}
          onChange={setInput}
          onSubmit={submit}
          invalid={!!validationError || (state.status === 'error' && state.error.code === 'INVALID_ACCOUNT_FORMAT')}
          messageId="history-message"
        />
        <MessageRegion id="history-message" message={message} action={action} />
      </section>

      {state.status === 'loading' && <Loading />}

      {data && data.items.length > 0 && (
        <section className="card result" aria-label={M.tableCaption(activeAccount ?? '')}>
          <HistoryTable key={`${activeAccount}-${page}`} accountNo={activeAccount ?? ''} items={data.items} />
          <Pager
            page={data.page}
            totalPages={totalPages}
            hasPrevious={data.hasPrevious}
            hasNext={data.hasNext}
            onPrevious={() => goPage(page - 1)}
            onNext={() => goPage(page + 1)}
          />
        </section>
      )}
      {data && activeAccount && (
        <p className="cross-link">
          <Link to={`/positions/${activeAccount}`}>{M.viewPositions} →</Link>
        </p>
      )}
      <ShortcutsHint paging={!!data && data.items.length > 0} />
    </div>
  );
}
