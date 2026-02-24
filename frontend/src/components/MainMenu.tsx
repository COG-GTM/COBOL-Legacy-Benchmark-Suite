import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Portfolio } from '../types';
import { api } from '../api/client';

/**
 * Main Menu component - replaces BMS MENMAP (Main Menu screen).
 * Source: src/maps/INQSET.bms MENMAP definition
 *
 * Provides navigation to portfolio inquiry and history inquiry,
 * replacing CICS INQONLN routing (P300/P400 paragraphs).
 */
function MainMenu() {
  const [portfolios, setPortfolios] = useState<Portfolio[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchPortfolios = async () => {
      try {
        const data = await api.getPortfolios();
        setPortfolios(data);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load portfolios');
      } finally {
        setLoading(false);
      }
    };
    fetchPortfolios();
  }, []);

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'A': return <span className="badge badge-active">Active</span>;
      case 'C': return <span className="badge badge-closed">Closed</span>;
      case 'S': return <span className="badge badge-suspended">Suspended</span>;
      default: return <span className="badge">{status}</span>;
    }
  };

  const getRiskLabel = (risk: string) => {
    switch (risk) {
      case 'H': return 'High';
      case 'M': return 'Medium';
      case 'L': return 'Low';
      default: return risk;
    }
  };

  if (loading) {
    return (
      <div className="container main-menu">
        <div className="loading">
          <div className="loading-spinner" />
          <p>Loading portfolios...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="container main-menu">
      <h2>Dashboard</h2>
      <p className="subtitle">Investment Portfolio Management System - Migrated from COBOL/CICS</p>

      <div className="menu-grid">
        <Link to="/" className="menu-card">
          <h3>Portfolio Inquiry</h3>
          <p>View portfolio details and current positions. Replaces CICS INQPORT transaction.</p>
        </Link>
        <Link to="/" className="menu-card">
          <h3>Transaction History</h3>
          <p>View transaction history with pagination. Replaces CICS INQHIST transaction.</p>
        </Link>
        <Link to="/" className="menu-card">
          <h3>Batch Operations</h3>
          <p>Trigger end-of-day batch processing. Replaces COBOL batch JCL pipeline.</p>
        </Link>
      </div>

      {error && (
        <div className="error-display">
          <h3>Error</h3>
          <p>{error}</p>
        </div>
      )}

      <div className="card">
        <div className="card-header">Portfolios</div>
        <div className="card-body">
          <div className="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>Portfolio ID</th>
                  <th>Name</th>
                  <th>Client ID</th>
                  <th>Currency</th>
                  <th>Risk Level</th>
                  <th>Status</th>
                  <th>Open Date</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {portfolios.length === 0 ? (
                  <tr>
                    <td colSpan={8} style={{ textAlign: 'center', color: '#a0aec0' }}>
                      No portfolios found
                    </td>
                  </tr>
                ) : (
                  portfolios.map((p) => (
                    <tr key={p.portfolioId}>
                      <td><strong>{p.portfolioId}</strong></td>
                      <td>{p.portfolioName}</td>
                      <td>{p.clientId}</td>
                      <td>{p.currencyCode}</td>
                      <td>{getRiskLabel(p.riskLevel)}</td>
                      <td>{getStatusBadge(p.status)}</td>
                      <td>{p.openDate}</td>
                      <td>
                        <Link to={`/portfolios/${p.portfolioId}`} className="btn btn-secondary" style={{ marginRight: '8px', padding: '4px 12px', fontSize: '0.8rem' }}>
                          View
                        </Link>
                        <Link to={`/portfolios/${p.portfolioId}/history`} className="btn btn-secondary" style={{ padding: '4px 12px', fontSize: '0.8rem' }}>
                          History
                        </Link>
                      </td>
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

export default MainMenu;
