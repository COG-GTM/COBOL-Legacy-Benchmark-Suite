import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { Transaction, PageResponse } from '../types';
import { api } from '../api/client';

/**
 * Transaction History component - replaces BMS HISMAP (History Display screen).
 * Source: src/maps/INQSET.bms HISMAP definition
 * Backend: GET /api/portfolios/{id}/history (replaces INQHIST.cbl cursor-based fetch)
 *
 * The original INQHIST used HISTORY_CURSOR with a 3000-byte array fetch.
 * This component uses standard REST pagination via Spring Data Pageable.
 */
function TransactionHistory() {
  const { id } = useParams<{ id: string }>();
  const [page, setPage] = useState<PageResponse<Transaction> | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const pageSize = 10;

  useEffect(() => {
    if (!id) return;
    const fetchHistory = async () => {
      setLoading(true);
      try {
        const data = await api.getHistory(id, currentPage, pageSize);
        setPage(data);
        setError('');
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load history');
      } finally {
        setLoading(false);
      }
    };
    fetchHistory();
  }, [id, currentPage]);

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(value);
  };

  const getTypeBadge = (type: string) => {
    switch (type) {
      case 'BU': return <span className="badge badge-buy">Buy</span>;
      case 'SL': return <span className="badge badge-sell">Sell</span>;
      case 'TR': return <span className="badge badge-transfer">Transfer</span>;
      case 'FE': return <span className="badge badge-fee">Fee</span>;
      default: return <span className="badge">{type}</span>;
    }
  };

  return (
    <div className="container detail-page">
      <Link to={id ? `/portfolios/${id}` : '/'} className="back-link">
        &larr; Back to Portfolio
      </Link>

      <div className="detail-header">
        <div>
          <h2>Transaction History</h2>
          <span style={{ color: '#718096', fontSize: '0.9rem' }}>
            Portfolio: {id}
            {page && ` | ${page.totalElements} total transactions`}
          </span>
        </div>
      </div>

      {error && (
        <div className="error-display">
          <h3>Error</h3>
          <p>{error}</p>
        </div>
      )}

      {loading ? (
        <div className="loading">
          <div className="loading-spinner" />
          <p>Loading transaction history...</p>
        </div>
      ) : (
        <>
          <div className="card">
            <div className="card-header">Transactions</div>
            <div className="card-body">
              <div className="table-wrapper">
                <table>
                  <thead>
                    <tr>
                      <th>Transaction ID</th>
                      <th>Date</th>
                      <th>Time</th>
                      <th>Investment</th>
                      <th>Type</th>
                      <th>Quantity</th>
                      <th>Price</th>
                      <th>Amount</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {!page || page.content.length === 0 ? (
                      <tr>
                        <td colSpan={9} style={{ textAlign: 'center', color: '#a0aec0' }}>
                          No transactions found
                        </td>
                      </tr>
                    ) : (
                      page.content.map((txn) => (
                        <tr key={txn.transactionId}>
                          <td><strong>{txn.transactionId}</strong></td>
                          <td>{txn.transactionDate}</td>
                          <td>{txn.transactionTime}</td>
                          <td>{txn.investmentId}</td>
                          <td>{getTypeBadge(txn.transactionType)}</td>
                          <td className="amount">{txn.quantity.toLocaleString()}</td>
                          <td className="amount">{formatCurrency(txn.price)}</td>
                          <td className="amount">{formatCurrency(txn.amount)}</td>
                          <td>
                            <span className={`badge ${txn.status === 'P' ? 'badge-active' : txn.status === 'F' ? 'badge-closed' : 'badge-suspended'}`}>
                              {txn.status === 'P' ? 'Processed' : txn.status === 'F' ? 'Failed' : txn.status === 'D' ? 'Done' : txn.status}
                            </span>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          {page && page.totalPages > 1 && (
            <div className="pagination">
              <button
                onClick={() => setCurrentPage(p => Math.max(0, p - 1))}
                disabled={page.first}
              >
                Previous
              </button>
              <span>
                Page {page.number + 1} of {page.totalPages}
              </span>
              <button
                onClick={() => setCurrentPage(p => p + 1)}
                disabled={page.last}
              >
                Next
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}

export default TransactionHistory;
