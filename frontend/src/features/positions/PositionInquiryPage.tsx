import { useCallback, useMemo, useRef, useState } from 'react';
import { Pagination } from '../../components/Pagination';
import { StatusBadge } from '../../components/StatusBadge';
import { usePositionService } from '../../services/servicesContext';
import {
  POSITION_STATUS_LABELS,
  POSITION_STATUSES,
  type Position,
  type PositionStatus,
} from '../../types/position';
import { formatCurrency, formatQuantity } from '../../utils/decimal';
import { summarizePositions } from './valuation';

const PAGE_SIZE = 10;

export function PositionInquiryPage() {
  const service = usePositionService();

  const [accountInput, setAccountInput] = useState('');
  const [statusInput, setStatusInput] = useState<PositionStatus | ''>('');

  const [positions, setPositions] = useState<Position[]>([]);
  const [searchedAccount, setSearchedAccount] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(1);
  // Guards against out-of-order responses: only the latest search may commit.
  const requestId = useRef(0);

  const load = useCallback(
    async (accountNo: string, status: PositionStatus | '') => {
      const id = ++requestId.current;
      setLoading(true);
      setError(null);
      setPage(1);
      try {
        const results = await service.listByAccount(accountNo, { status });
        if (id !== requestId.current) return;
        setPositions(results);
        setSearchedAccount(accountNo);
      } catch {
        if (id !== requestId.current) return;
        setError('Unable to load positions. Please try again.');
      } finally {
        if (id === requestId.current) setLoading(false);
      }
    },
    [service],
  );

  const onSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const account = accountInput.trim();
    if (!account) {
      setError('Enter an account number to search.');
      return;
    }
    void load(account, statusInput);
  };

  const onReset = () => {
    requestId.current += 1;
    setAccountInput('');
    setStatusInput('');
    setPositions([]);
    setSearchedAccount(null);
    setError(null);
    setPage(1);
  };

  const valuation = useMemo(
    () => summarizePositions(positions),
    [positions],
  );

  const pageCount = Math.max(1, Math.ceil(positions.length / PAGE_SIZE));
  const currentPage = Math.min(page, pageCount);
  const pagePositions = positions.slice(
    (currentPage - 1) * PAGE_SIZE,
    currentPage * PAGE_SIZE,
  );

  return (
    <section>
      <div className="page-header">
        <div>
          <h1 className="page-header__title">Position Inquiry</h1>
          <p className="page-header__subtitle">
            Search account holdings (INQPORT / POSFILE)
          </p>
        </div>
      </div>

      <form
        className="filters"
        onSubmit={onSubmit}
        aria-label="Search positions"
      >
        <div className="field">
          <label htmlFor="position-account">Account number</label>
          <input
            id="position-account"
            type="text"
            value={accountInput}
            onChange={(e) => setAccountInput(e.target.value)}
            placeholder="e.g. ACCT100001"
          />
        </div>
        <div className="field">
          <label htmlFor="position-status">Status</label>
          <select
            id="position-status"
            value={statusInput}
            onChange={(e) =>
              setStatusInput(e.target.value as PositionStatus | '')
            }
          >
            <option value="">All</option>
            {POSITION_STATUSES.map((s) => (
              <option key={s} value={s}>
                {POSITION_STATUS_LABELS[s]}
              </option>
            ))}
          </select>
        </div>
        <div className="filters__actions">
          <button type="submit" className="btn btn--primary">
            Search
          </button>
          <button type="button" className="btn btn--ghost" onClick={onReset}>
            Reset
          </button>
        </div>
      </form>

      {error && (
        <div className="alert alert--error" role="alert">
          {error}
        </div>
      )}

      {searchedAccount && !loading && positions.length > 0 && (
        <div className="valuation" data-testid="valuation-summary">
          <ValuationCard
            label="Total Market Value"
            value={formatCurrency(valuation.totalMarketValue)}
          />
          <ValuationCard
            label="Total Cost Basis"
            value={formatCurrency(valuation.totalCostBasis)}
          />
          <ValuationCard
            label="Gain / Loss"
            value={formatCurrency(valuation.gainLoss)}
            tone={gainLossTone(valuation.gainLoss)}
          />
        </div>
      )}

      <div className="card">
        {!searchedAccount ? (
          <p className="state-msg">
            Enter an account number and search to view its positions.
          </p>
        ) : loading ? (
          <p className="state-msg">Loading positions…</p>
        ) : positions.length === 0 ? (
          <p className="state-msg" data-testid="empty-state">
            No positions found for account {searchedAccount}.
          </p>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Fund ID</th>
                <th>Fund Name</th>
                <th>Status</th>
                <th className="num">Units</th>
                <th className="num">Cost Basis</th>
                <th className="num">Market Value</th>
              </tr>
            </thead>
            <tbody>
              {pagePositions.map((position) => (
                <tr
                  key={`${position.portfolioId}-${position.date}-${position.investmentId}`}
                >
                  <td>{position.investmentId}</td>
                  <td>{position.fundName}</td>
                  <td>
                    <StatusBadge status={position.status} />
                  </td>
                  <td className="num">{formatQuantity(position.quantity)}</td>
                  <td className="num">{formatCurrency(position.costBasis)}</td>
                  <td className="num">
                    {formatCurrency(position.marketValue)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {searchedAccount && !loading && positions.length > 0 && (
        <>
          <Pagination
            page={currentPage}
            pageCount={pageCount}
            onPageChange={setPage}
          />
          <p className="result-count">
            {positions.length} position{positions.length === 1 ? '' : 's'}
          </p>
        </>
      )}
    </section>
  );
}

/** Break-even (0.00) is neutral; only true gains/losses get color. */
function gainLossTone(gainLoss: string): 'positive' | 'negative' | undefined {
  if (gainLoss.startsWith('-')) return 'negative';
  if (/^0(\.0+)?$/.test(gainLoss)) return undefined;
  return 'positive';
}

function ValuationCard({
  label,
  value,
  tone,
}: {
  label: string;
  value: string;
  tone?: 'positive' | 'negative';
}) {
  const toneClass =
    tone === 'positive'
      ? ' valuation-card__value--positive'
      : tone === 'negative'
        ? ' valuation-card__value--negative'
        : '';
  return (
    <div className="card valuation-card">
      <span className="valuation-card__label">{label}</span>
      <span className={`valuation-card__value${toneClass}`}>{value}</span>
    </div>
  );
}
