import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { StatusBadge } from '../../components/StatusBadge';
import { usePortfolioService } from '../../services/servicesContext';
import {
  CLIENT_TYPE_LABELS,
  PORTFOLIO_STATUS_LABELS,
  PORTFOLIO_STATUSES,
  type Portfolio,
  type PortfolioQuery,
  type PortfolioStatus,
} from '../../types/portfolio';
import { formatCurrency } from '../../utils/decimal';

const EMPTY_QUERY: Required<PortfolioQuery> = {
  accountNo: '',
  clientName: '',
  status: '',
};

export function PortfolioListPage() {
  const service = usePortfolioService();
  const navigate = useNavigate();
  const [filters, setFilters] = useState<Required<PortfolioQuery>>(EMPTY_QUERY);
  const [portfolios, setPortfolios] = useState<Portfolio[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(
    async (query: PortfolioQuery) => {
      setLoading(true);
      setError(null);
      try {
        setPortfolios(await service.list(query));
      } catch {
        setError('Unable to load portfolios. Please try again.');
      } finally {
        setLoading(false);
      }
    },
    [service],
  );

  useEffect(() => {
    void load(EMPTY_QUERY);
  }, [load]);

  const onSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    void load(filters);
  };

  const onReset = () => {
    setFilters(EMPTY_QUERY);
    void load(EMPTY_QUERY);
  };

  return (
    <section>
      <div className="page-header">
        <div>
          <h1 className="page-header__title">Portfolios</h1>
          <p className="page-header__subtitle">
            Manage client portfolios (PORTMSTR)
          </p>
        </div>
        <Link to="/portfolios/new" className="btn btn--primary">
          + New Portfolio
        </Link>
      </div>

      <form className="filters" onSubmit={onSubmit} aria-label="Search portfolios">
        <div className="field">
          <label htmlFor="filter-account">Account number</label>
          <input
            id="filter-account"
            type="text"
            value={filters.accountNo}
            onChange={(e) =>
              setFilters((f) => ({ ...f, accountNo: e.target.value }))
            }
            placeholder="e.g. ACCT100001"
          />
        </div>
        <div className="field">
          <label htmlFor="filter-name">Client name</label>
          <input
            id="filter-name"
            type="text"
            value={filters.clientName}
            onChange={(e) =>
              setFilters((f) => ({ ...f, clientName: e.target.value }))
            }
            placeholder="e.g. Chen"
          />
        </div>
        <div className="field">
          <label htmlFor="filter-status">Status</label>
          <select
            id="filter-status"
            value={filters.status}
            onChange={(e) =>
              setFilters((f) => ({
                ...f,
                status: e.target.value as PortfolioStatus | '',
              }))
            }
          >
            <option value="">All</option>
            {PORTFOLIO_STATUSES.map((s) => (
              <option key={s} value={s}>
                {PORTFOLIO_STATUS_LABELS[s]}
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

      <div className="card">
        {loading ? (
          <p className="state-msg">Loading portfolios…</p>
        ) : portfolios.length === 0 ? (
          <p className="state-msg" data-testid="empty-state">
            No portfolios match your search.
          </p>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Portfolio ID</th>
                <th>Account No.</th>
                <th>Client Name</th>
                <th>Type</th>
                <th>Status</th>
                <th className="num">Total Value</th>
                <th className="num">Cash Balance</th>
              </tr>
            </thead>
            <tbody>
              {portfolios.map((p) => (
                <tr
                  key={p.portId}
                  className="table__row--clickable"
                  onClick={() => navigate(`/portfolios/${p.portId}`)}
                >
                  <td>
                    <Link
                      to={`/portfolios/${p.portId}`}
                      onClick={(e) => e.stopPropagation()}
                    >
                      {p.portId}
                    </Link>
                  </td>
                  <td>{p.accountNo}</td>
                  <td>{p.clientName}</td>
                  <td>{CLIENT_TYPE_LABELS[p.clientType]}</td>
                  <td>
                    <StatusBadge status={p.status} />
                  </td>
                  <td className="num">{formatCurrency(p.totalValue)}</td>
                  <td className="num">{formatCurrency(p.cashBalance)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {!loading && portfolios.length > 0 && (
        <p className="result-count">
          {portfolios.length} portfolio{portfolios.length === 1 ? '' : 's'}
        </p>
      )}
    </section>
  );
}
