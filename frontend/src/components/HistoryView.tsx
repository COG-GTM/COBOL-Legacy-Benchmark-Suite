import { useState, useEffect, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { apiClient, HistoryEntry } from "../api/client";
import { usePagination } from "../hooks/usePagination";
import { useErrorContext } from "../contexts/ErrorContext";

export default function HistoryView() {
  const navigate = useNavigate();
  const { error, setError, clearError } = useErrorContext();
  const pagination = usePagination();
  const [accountNo, setAccountNo] = useState("");
  const [entries, setEntries] = useState<HistoryEntry[]>([]);
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);

  const fetchHistory = useCallback(
    async (account: string, page: number) => {
      setLoading(true);
      clearError();

      try {
        const response = await apiClient.getHistory(account, page);
        setEntries(response.data.entries);
        pagination.setTotalPages(response.data.totalPages);
      } catch (err) {
        const message =
          err instanceof Error
            ? err.message
            : "Error accessing transaction history";
        setError({ code: "HIST_ERR", message });
        setEntries([]);
      } finally {
        setLoading(false);
      }
    },
    [clearError, setError, pagination]
  );

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!accountNo.trim()) return;
    pagination.reset();
    setSearched(true);
    await fetchHistory(accountNo.trim(), 1);
  };

  useEffect(() => {
    if (searched && accountNo.trim()) {
      fetchHistory(accountNo.trim(), pagination.currentPage);
    }
  }, [pagination.currentPage]);

  const formatCurrency = (value: number) =>
    new Intl.NumberFormat("en-US", {
      style: "currency",
      currency: "USD",
    }).format(value);

  const formatUnits = (value: number) =>
    new Intl.NumberFormat("en-US", {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(value);

  return (
    <div className="screen">
      <h1 className="screen-title">Transaction History Inquiry</h1>

      <form onSubmit={handleSearch} className="search-form">
        <div className="field-row">
          <label htmlFor="hisAccountNo">Account:</label>
          <input
            id="hisAccountNo"
            type="text"
            value={accountNo}
            onChange={(e) => setAccountNo(e.target.value)}
            maxLength={10}
            autoFocus
          />
          <button type="submit" disabled={loading} className="btn btn-primary">
            {loading ? "Loading..." : "Search"}
          </button>
        </div>
      </form>

      {searched && (
        <div className="history-table-container">
          <table className="history-table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Type</th>
                <th>Units</th>
                <th>Price</th>
                <th>Amount</th>
              </tr>
            </thead>
            <tbody>
              {entries.length > 0 ? (
                entries.map((entry, index) => (
                  <tr key={index}>
                    <td>{entry.date}</td>
                    <td>{entry.type}</td>
                    <td>{formatUnits(entry.units)}</td>
                    <td>{formatCurrency(entry.price)}</td>
                    <td>{formatCurrency(entry.amount)}</td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={5} className="empty-row">
                    No history found
                  </td>
                </tr>
              )}
            </tbody>
          </table>

          <div className="page-info">
            Page {pagination.currentPage} of {pagination.totalPages}
          </div>
        </div>
      )}

      <div className="action-bar">
        <button className="btn" onClick={() => navigate("/")}>
          PF3=Exit
        </button>
        <button
          className="btn"
          onClick={pagination.goToPrevious}
          disabled={!pagination.hasPrevious || loading}
        >
          PF7=Previous
        </button>
        <button
          className="btn"
          onClick={pagination.goToNext}
          disabled={!pagination.hasNext || loading}
        >
          PF8=Next
        </button>
      </div>

      {error && (
        <div className="error-bar" role="alert">
          {error.message}
          <button onClick={clearError} className="btn-dismiss">
            Dismiss
          </button>
        </div>
      )}
    </div>
  );
}
