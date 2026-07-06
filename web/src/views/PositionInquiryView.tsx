import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { AccountForm } from '../components/AccountForm';
import { Message } from '../components/Message';
import { ApiError, getPortfolio } from '../api/client';
import {
  CLIENT_TYPE_LABELS,
  PORTFOLIO_STATUS_LABELS,
  POSITION_STATUS_LABELS,
  type PortfolioResponse,
} from '../types/portfolio';
import { formatCobolDate, formatCurrency, formatUnits } from '../lib/format';

/**
 * Portfolio Position Inquiry — replaces BMS POSMAP / program INQPORT.
 * Enter an account number; displays fund ID, fund name, units, cost basis and
 * market value for each position. "Position not found" mirrors INQPORT's
 * P900-NOT-FOUND flow.
 */
export function PositionInquiryView() {
  const [searchParams, setSearchParams] = useSearchParams();
  const account = searchParams.get('account') ?? '';

  const [data, setData] = useState<PortfolioResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!account) {
      setData(null);
      setError(null);
      return;
    }

    const controller = new AbortController();
    setLoading(true);
    setError(null);
    setData(null);

    getPortfolio(account, controller.signal)
      .then(setData)
      .catch((err: unknown) => {
        if (err instanceof DOMException && err.name === 'AbortError') return;
        setError(err instanceof ApiError ? err.message : 'Unexpected error');
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });

    return () => controller.abort();
  }, [account]);

  return (
    <section className="card">
      <h1 className="view-title">Portfolio Position Inquiry</h1>
      <AccountForm
        label="Account:"
        initialValue={account}
        onSubmit={(value) => setSearchParams({ account: value })}
      />

      {loading && <Message variant="info">Reading position…</Message>}
      {error && <Message variant="error">{error}</Message>}

      {data && (
        <>
          <dl className="summary">
            <div>
              <dt>Account</dt>
              <dd>{data.portfolio.accountNo}</dd>
            </div>
            <div>
              <dt>Portfolio ID</dt>
              <dd>{data.portfolio.portfolioId}</dd>
            </div>
            <div>
              <dt>Client Name</dt>
              <dd>{data.portfolio.clientName}</dd>
            </div>
            <div>
              <dt>Client Type</dt>
              <dd>{CLIENT_TYPE_LABELS[data.portfolio.clientType]}</dd>
            </div>
            <div>
              <dt>Status</dt>
              <dd>{PORTFOLIO_STATUS_LABELS[data.portfolio.status]}</dd>
            </div>
            <div>
              <dt>Created</dt>
              <dd>{formatCobolDate(data.portfolio.createDate)}</dd>
            </div>
            <div>
              <dt>Total Value</dt>
              <dd>{formatCurrency(data.portfolio.totalValue)}</dd>
            </div>
            <div>
              <dt>Cash Balance</dt>
              <dd>{formatCurrency(data.portfolio.cashBalance)}</dd>
            </div>
          </dl>

          <h2 className="section-title">Positions</h2>
          {data.positions.length === 0 ? (
            <Message variant="info">No positions on file for this account.</Message>
          ) : (
            <div className="table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Fund ID</th>
                    <th>Fund Name</th>
                    <th className="num">Units</th>
                    <th className="num">Cost Basis</th>
                    <th className="num">Market Value</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {data.positions.map((pos) => (
                    <tr key={`${pos.portfolioId}-${pos.investmentId}`}>
                      <td className="mono">{pos.investmentId}</td>
                      <td>{pos.fundName}</td>
                      <td className="num">{formatUnits(pos.quantity)}</td>
                      <td className="num">{formatCurrency(pos.costBasis)}</td>
                      <td className="num">{formatCurrency(pos.marketValue)}</td>
                      <td>{POSITION_STATUS_LABELS[pos.status]}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}
    </section>
  );
}
