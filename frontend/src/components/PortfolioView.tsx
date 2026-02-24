import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { Portfolio } from '../types';
import { api } from '../api/client';

/**
 * Portfolio View component - replaces BMS POSMAP (Portfolio Display screen).
 * Source: src/maps/INQSET.bms POSMAP definition
 * Backend: GET /api/portfolios/{id} (replaces INQPORT.cbl P200-GET-POSITION)
 */
function PortfolioView() {
  const { id } = useParams<{ id: string }>();
  const [portfolio, setPortfolio] = useState<Portfolio | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!id) return;
    const fetchPortfolio = async () => {
      try {
        const data = await api.getPortfolio(id);
        setPortfolio(data);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load portfolio');
      } finally {
        setLoading(false);
      }
    };
    fetchPortfolio();
  }, [id]);

  const formatCurrency = (value: number | null, currency = 'USD') => {
    if (value === null || value === undefined) return '$0.00';
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency,
    }).format(value);
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'A': return <span className="badge badge-active">Active</span>;
      case 'C': return <span className="badge badge-closed">Closed</span>;
      case 'P': return <span className="badge badge-suspended">Pending</span>;
      default: return <span className="badge">{status}</span>;
    }
  };

  if (loading) {
    return (
      <div className="container detail-page">
        <div className="loading">
          <div className="loading-spinner" />
          <p>Loading portfolio...</p>
        </div>
      </div>
    );
  }

  if (error || !portfolio) {
    return (
      <div className="container detail-page">
        <Link to="/" className="back-link">&larr; Back to Dashboard</Link>
        <div className="error-display">
          <h3>Portfolio Not Found</h3>
          <p>{error || 'The requested portfolio could not be found.'}</p>
        </div>
      </div>
    );
  }

  const gainLoss = portfolio.totalGainLoss ?? 0;

  return (
    <div className="container detail-page">
      <Link to="/" className="back-link">&larr; Back to Dashboard</Link>

      <div className="detail-header">
        <div>
          <h2>{portfolio.portfolioName}</h2>
          <span style={{ color: '#718096', fontSize: '0.9rem' }}>
            {portfolio.portfolioId} | Client: {portfolio.clientId}
          </span>
        </div>
        <div>
          {getStatusBadge(portfolio.status)}
          <Link
            to={`/portfolios/${portfolio.portfolioId}/history`}
            className="btn btn-primary"
            style={{ marginLeft: '12px' }}
          >
            View History
          </Link>
        </div>
      </div>

      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-label">Market Value</div>
          <div className="stat-value">
            {formatCurrency(portfolio.totalMarketValue, portfolio.currencyCode)}
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Cost Basis</div>
          <div className="stat-value">
            {formatCurrency(portfolio.totalCostBasis, portfolio.currencyCode)}
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Gain/Loss</div>
          <div className={`stat-value ${gainLoss >= 0 ? 'positive' : 'negative'}`}>
            {formatCurrency(portfolio.totalGainLoss, portfolio.currencyCode)}
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Positions</div>
          <div className="stat-value">{portfolio.positions?.length ?? 0}</div>
        </div>
      </div>

      <div className="card" style={{ marginBottom: '24px' }}>
        <div className="card-header">Portfolio Details</div>
        <div className="card-body">
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '16px' }}>
            <div><strong>Account Type:</strong> {portfolio.accountType}</div>
            <div><strong>Branch:</strong> {portfolio.branchId}</div>
            <div><strong>Currency:</strong> {portfolio.currencyCode}</div>
            <div><strong>Risk Level:</strong> {portfolio.riskLevel === 'H' ? 'High' : portfolio.riskLevel === 'M' ? 'Medium' : 'Low'}</div>
            <div><strong>Open Date:</strong> {portfolio.openDate}</div>
            <div><strong>Last Updated:</strong> {portfolio.lastMaintDate}</div>
          </div>
        </div>
      </div>

      <div className="card">
        <div className="card-header">Investment Positions</div>
        <div className="card-body">
          <div className="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>Investment ID</th>
                  <th>Position Date</th>
                  <th>Quantity</th>
                  <th>Cost Basis</th>
                  <th>Market Value</th>
                  <th>Gain/Loss</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {!portfolio.positions || portfolio.positions.length === 0 ? (
                  <tr>
                    <td colSpan={7} style={{ textAlign: 'center', color: '#a0aec0' }}>
                      No positions found
                    </td>
                  </tr>
                ) : (
                  portfolio.positions.map((pos) => (
                    <tr key={`${pos.investmentId}-${pos.positionDate}`}>
                      <td><strong>{pos.investmentId}</strong></td>
                      <td>{pos.positionDate}</td>
                      <td className="amount">{pos.quantity.toLocaleString()}</td>
                      <td className="amount">{formatCurrency(pos.costBasis, pos.currencyCode)}</td>
                      <td className="amount">{formatCurrency(pos.marketValue, pos.currencyCode)}</td>
                      <td className={`amount ${pos.unrealizedGainLoss >= 0 ? 'positive' : 'negative'}`}>
                        {formatCurrency(pos.unrealizedGainLoss, pos.currencyCode)}
                      </td>
                      <td>{getStatusBadge(pos.status)}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}

export default PortfolioView;
