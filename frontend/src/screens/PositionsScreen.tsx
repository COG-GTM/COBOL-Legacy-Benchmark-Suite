import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, Navigate, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import type { Position, PositionPage } from '../api/types';
import { AccountSearchForm } from '../components/AccountSearchForm';
import { Loading } from '../components/Loading';
import { MessageRegion, type Message } from '../components/MessageRegion';
import { NotAvailable } from '../components/NotAvailable';
import { PageHeading } from '../components/PageHeading';
import { Pager } from '../components/Pager';
import { totalPagesFor } from '../state/paging';
import { ScreenNav, ShortcutsHint } from '../components/ScreenNav';
import { StatusBadge } from '../components/StatusBadge';
import { formatIsoDate, formatTimestamp } from '../format/date';
import { formatMoney, formatPositionUnits } from '../format/decimal';
import { messages } from '../i18n/messages';
import { useSession } from '../state/SessionContext';
import { useApiErrorRouting } from '../state/useApiErrorRouting';
import { useApiQuery } from '../state/useApiQuery';
import { useDocumentTitle } from '../state/useDocumentTitle';
import { normaliseAccountNo, parsePageParam, validateAccountNo } from '../state/validation';

const M = messages.positions;

function FundName({ name }: { name: string | null }) {
  if (!name) return <NotAvailable />;
  return (
    <span className="truncate" title={name}>
      {name.length > 30 ? `${name.slice(0, 30)}…` : name}
    </span>
  );
}

function PositionSummary({ position }: { position: Position }) {
  const rows: Array<[string, React.ReactNode, string?]> = [
    [M.fundId, <span className="mono">{position.investmentId}</span>],
    [M.fundName, <FundName name={position.fundName} />],
    [M.units, formatPositionUnits(position.units), 'num'],
    [M.costBasis, formatMoney(position.costBasis, position.currency), 'num'],
    [M.marketValue, formatMoney(position.marketValue, position.currency), 'num'],
    [M.portfolioId, <span className="mono">{position.portfolioId}</span>],
    [M.positionDate, formatIsoDate(position.positionDate)],
    [M.status, <StatusBadge status={position.status} />],
    [M.currency, position.currency],
    [
      M.lastMaintained,
      position.lastMaintainedAt ? `${formatTimestamp(position.lastMaintainedAt)} · ${position.lastMaintainedBy ?? ''}` : <NotAvailable />,
    ],
  ];
  return (
    <section className={`card result ${position.status === 'C' ? 'is-closed' : ''}`} aria-labelledby="position-summary-title" data-testid="position-summary">
      <h2 id="position-summary-title" className="result__title">
        {M.summaryHeading(position.accountNo)}
      </h2>
      <dl className="detail-grid">
        {rows.map(([label, value, cls]) => (
          <div key={label} className="detail-grid__item">
            <dt>{label}</dt>
            <dd className={cls}>{value}</dd>
          </div>
        ))}
      </dl>
    </section>
  );
}

function PositionTable({ accountNo, page }: { accountNo: string; page: PositionPage }) {
  const header = (label: string) => label.replace(/:$/, '');
  return (
    <div className="table-wrap" tabIndex={0} role="region" aria-labelledby="positions-caption">
      <table className="table" data-testid="positions-table">
        <caption id="positions-caption">{M.tableCaption(accountNo)}</caption>
        <thead>
          <tr>
            <th scope="col">{header(M.fundId)}</th>
            <th scope="col">{header(M.fundName)}</th>
            <th scope="col" className="num">{header(M.units)}</th>
            <th scope="col" className="num">{header(M.costBasis)}</th>
            <th scope="col" className="num">{header(M.marketValue)}</th>
            <th scope="col">{header(M.portfolioId)}</th>
            <th scope="col">{header(M.positionDate)}</th>
            <th scope="col">{header(M.status)}</th>
          </tr>
        </thead>
        <tbody>
          {page.items.map((p) => (
            <tr key={`${p.portfolioId}-${p.investmentId}-${p.positionDate}`} className={p.status === 'C' ? 'is-closed' : undefined}>
              <th scope="row" className="mono">{p.investmentId}</th>
              <td><FundName name={p.fundName} /></td>
              <td className="num">{formatPositionUnits(p.units)}</td>
              <td className="num">{formatMoney(p.costBasis, p.currency)}</td>
              <td className="num">{formatMoney(p.marketValue, p.currency)}</td>
              <td className="mono">{p.portfolioId}</td>
              <td>{formatIsoDate(p.positionDate)}</td>
              <td><StatusBadge status={p.status} /></td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

/** S2 Portfolio Position Inquiry (POSMAP / INQPORT). */
export function PositionsScreen() {
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
    (signal: AbortSignal) => api.getPositions(activeAccount ?? '', page, signal),
    [api, activeAccount, page],
  );
  const { state } = useApiQuery<PositionPage>(activeAccount ? `${activeAccount}|${page}|${nonce}` : null, fetcher, routeError);

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
    if (acct === activeAccount && page === 1) setNonce((n) => n + 1);
    else navigate(`/positions/${acct}`);
  }

  // FR-008: arriving without an account re-runs the inquiry for the last valid account.
  if (routeAccount === undefined && selectedAccountNo) {
    return <Navigate to={`/positions/${selectedAccountNo}`} replace />;
  }

  const data = state.status === 'success' ? state.data : null;
  const totalPages = data ? totalPagesFor(data.totalCount, data.pageSize) : 1;
  const goPage = (p: number) => setSearchParams(p > 1 ? { page: String(p) } : {});

  let message: Message | null = null;
  if (validationError) message = { text: validationError, severity: 'WARNING' };
  else if (state.status === 'error')
    message = {
      text: state.error.message,
      severity: state.error.severity,
      traceId: state.httpStatus >= 500 ? state.error.traceId : undefined,
    };

  return (
    <div className="screen">
      <ScreenNav
        current={M.breadcrumb}
        onPrevious={data?.hasPrevious ? () => goPage(page - 1) : undefined}
        onNext={data?.hasNext ? () => goPage(page + 1) : undefined}
      />
      <section className="card" aria-labelledby="positions-title">
        <div id="positions-title">
          <PageHeading>{M.title}</PageHeading>
        </div>
        <AccountSearchForm
          ref={inputRef}
          idPrefix="positions"
          value={input}
          onChange={setInput}
          onSubmit={submit}
          invalid={!!validationError || state.status === 'error' && state.error.code === 'INVALID_ACCOUNT_FORMAT'}
          messageId="positions-message"
        />
        <MessageRegion id="positions-message" message={message} />
      </section>

      {state.status === 'loading' && <Loading />}

      {data && data.items.length === 1 && data.totalCount === 1 && data.items[0] && <PositionSummary position={data.items[0]} />}
      {data && (data.items.length > 1 || data.totalCount > 1) && (
        <section className="card result" aria-label={M.tableCaption(activeAccount ?? '')}>
          <PositionTable accountNo={activeAccount ?? ''} page={data} />
          {data.totalCount > data.pageSize && (
            <Pager
              page={data.page}
              totalPages={totalPages}
              hasPrevious={data.hasPrevious}
              hasNext={data.hasNext}
              onPrevious={() => goPage(page - 1)}
              onNext={() => goPage(page + 1)}
            />
          )}
        </section>
      )}
      {data && activeAccount && (
        <p className="cross-link">
          <Link to={`/history/${activeAccount}?page=1`}>{M.viewHistory} →</Link>
        </p>
      )}
      <ShortcutsHint paging={!!data && data.totalCount > data.pageSize} />
    </div>
  );
}
