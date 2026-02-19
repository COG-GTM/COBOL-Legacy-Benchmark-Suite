import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { apiClient, PortfolioPosition } from "../api/client";
import { useErrorContext } from "../contexts/ErrorContext";

export default function PortfolioView() {
  const navigate = useNavigate();
  const { error, setError, clearError } = useErrorContext();
  const [accountNo, setAccountNo] = useState("");
  const [position, setPosition] = useState<PortfolioPosition | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!accountNo.trim()) return;

    setLoading(true);
    clearError();
    setPosition(null);

    try {
      const response = await apiClient.getPortfolio(accountNo.trim());
      setPosition(response.data);
    } catch (err) {
      const message =
        err instanceof Error ? err.message : "Error accessing position data";
      setError({ code: "PORT_ERR", message });
    } finally {
      setLoading(false);
    }
  };

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
      <h1 className="screen-title">Portfolio Position Inquiry</h1>

      <form onSubmit={handleSearch} className="search-form">
        <div className="field-row">
          <label htmlFor="accountNo">Account:</label>
          <input
            id="accountNo"
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

      {position && (
        <div className="position-details">
          <div className="detail-row">
            <span className="detail-label">Fund ID:</span>
            <span className="detail-value highlight">{position.fundId}</span>
            <span className="detail-label" style={{ marginLeft: "2rem" }}>
              Fund Name:
            </span>
            <span className="detail-value highlight">{position.fundName}</span>
          </div>
          <div className="detail-row">
            <span className="detail-label">Units:</span>
            <span className="detail-value highlight">
              {formatUnits(position.units)}
            </span>
          </div>
          <div className="detail-row">
            <span className="detail-label">Cost Basis:</span>
            <span className="detail-value highlight">
              {formatCurrency(position.costBasis)}
            </span>
          </div>
          <div className="detail-row">
            <span className="detail-label">Market Value:</span>
            <span className="detail-value highlight">
              {formatCurrency(position.marketValue)}
            </span>
          </div>
        </div>
      )}

      <div className="action-bar">
        <button className="btn" onClick={() => navigate("/")}>
          PF3=Exit
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
