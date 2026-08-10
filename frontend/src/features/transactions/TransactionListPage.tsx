import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Pagination } from '../../components/Pagination';
import { StatusBadge } from '../../components/StatusBadge';
import { useTransactionService } from '../../services/servicesContext';
import {
  TRANSACTION_STATUSES,
  TRANSACTION_STATUS_LABELS,
  TRANSACTION_TYPES,
  TRANSACTION_TYPE_LABELS,
  type Transaction,
  type TransactionStatus,
  type TransactionType,
} from '../../types/transaction';
import { formatCobolDate } from '../../utils/date';
import { formatCurrency, formatQuantity } from '../../utils/decimal';

const PAGE_SIZE = 10;

/** Router state set by the submission wizard after a successful write. */
interface SubmittedState {
  submitted?: Transaction;
}

/**
 * Transaction status view — the pending/completed/failed queue that replaces
 * browsing TRANFILE. Filters mirror the TRN-PORTFOLIO-ID, TRN-STATUS and
 * TRN-TYPE fields of the record.
 */
export function TransactionListPage() {
  const service = useTransactionService();
  const { state } = useLocation() as { state: SubmittedState | null };
  const submitted = state?.submitted;

  const [portfolioInput, setPortfolioInput] = useState('');
  const [statusInput, setStatusInput] = useState<TransactionStatus | ''>('');
  const [typeInput, setTypeInput] = useState<TransactionType | ''>('');

  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(1);
  // Guards against out-of-order responses: only the latest query may commit.
  const requestId = useRef(0);

  const load = useCallback(
    async (
      portfolioId: string,
      status: TransactionStatus | '',
      type: TransactionType | '',
    ) => {
      const id = ++requestId.current;
      setLoading(true);
      setError(null);
      try {
        const results = await service.list({ portfolioId, status, type });
        if (id !== requestId.current) return;
        setTransactions(results);
      } catch {
        if (id !== requestId.current) return;
        setError('Unable to load transactions. Please try again.');
      } finally {
        if (id === requestId.current) setLoading(false);
      }
    },
    [service],
  );

  useEffect(() => {
    void load('', '', '');
  }, [load]);

  const onSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(1);
    void load(portfolioInput.trim(), statusInput, typeInput);
  };

  const onReset = () => {
    setPortfolioInput('');
    setStatusInput('');
    setTypeInput('');
    setPage(1);
    void load('', '', '');
  };

  const pageCount = Math.max(1, Math.ceil(transactions.length / PAGE_SIZE));
  const currentPage = Math.min(page, pageCount);
  const pageTransactions = transactions.slice(
    (currentPage - 1) * PAGE_SIZE,
    currentPage * PAGE_SIZE,
  );

  return (
    <section>
      <div className="page-header">
        <div>
          <h1 className="page-header__title">Transactions</h1>
          <p className="page-header__subtitle">
            Submitted transactions and their settlement status (PORTTRAN /
            TRANFILE)
          </p>
        </div>
        <div className="page-header__actions">
          <Link to="/transactions/new" className="btn btn--primary">
            New Transaction
          </Link>
        </div>
      </div>

      {submitted && (
        <div className="alert alert--success" role="status">
          Transaction {submitted.sequenceNo} for {submitted.portfolioId} was
          submitted and is pending settlement.
        </div>
      )}

      <form
        className="filters"
        onSubmit={onSubmit}
        aria-label="Filter transactions"
      >
        <div className="field">
          <label htmlFor="transaction-portfolio">Portfolio ID</label>
          <input
            id="transaction-portfolio"
            type="text"
            value={portfolioInput}
            onChange={(e) => setPortfolioInput(e.target.value.toUpperCase())}
            placeholder="e.g. PORT0001"
          />
        </div>
        <div className="field">
          <label htmlFor="transaction-status">Status</label>
          <select
            id="transaction-status"
            value={statusInput}
            onChange={(e) =>
              setStatusInput(e.target.value as TransactionStatus | '')
            }
          >
            <option value="">All</option>
            {TRANSACTION_STATUSES.map((s) => (
              <option key={s} value={s}>
                {TRANSACTION_STATUS_LABELS[s]}
              </option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor="transaction-type">Type</label>
          <select
            id="transaction-type"
            value={typeInput}
            onChange={(e) =>
              setTypeInput(e.target.value as TransactionType | '')
            }
          >
            <option value="">All</option>
            {TRANSACTION_TYPES.map((t) => (
              <option key={t} value={t}>
                {TRANSACTION_TYPE_LABELS[t]}
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
          <p className="state-msg">Loading transactions…</p>
        ) : transactions.length === 0 ? (
          <p className="state-msg" data-testid="empty-state">
            No transactions match the current filters.
          </p>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Portfolio</th>
                <th>Investment</th>
                <th>Type</th>
                <th>Status</th>
                <th className="num">Quantity</th>
                <th className="num">Price</th>
                <th className="num">Amount</th>
              </tr>
            </thead>
            <tbody>
              {pageTransactions.map((transaction) => (
                <tr
                  key={`${transaction.date}-${transaction.time}-${transaction.portfolioId}-${transaction.sequenceNo}`}
                >
                  <td>{formatCobolDate(transaction.date)}</td>
                  <td>{transaction.portfolioId}</td>
                  <td>{transaction.investmentId}</td>
                  <td>{TRANSACTION_TYPE_LABELS[transaction.type]}</td>
                  <td>
                    <StatusBadge status={transaction.status} />
                  </td>
                  <td className="num">
                    {formatQuantity(transaction.quantity)}
                  </td>
                  <td className="num">
                    {transaction.type === 'TR'
                      ? '—'
                      : formatCurrency(transaction.price, transaction.currency)}
                  </td>
                  <td className="num">
                    {formatCurrency(transaction.amount, transaction.currency)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {!loading && transactions.length > 0 && (
        <>
          <Pagination
            page={currentPage}
            pageCount={pageCount}
            onPageChange={setPage}
          />
          <p className="result-count">
            {transactions.length} transaction
            {transactions.length === 1 ? '' : 's'}
          </p>
        </>
      )}
    </section>
  );
}
