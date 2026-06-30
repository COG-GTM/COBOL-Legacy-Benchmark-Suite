import { Link } from 'react-router-dom';
import { MetricCard } from '../../components/MetricCard';
import { getDashboardMetrics } from '../../services/dashboardService';
import {
  TRANSACTION_STATUS_LABELS,
  TRANSACTION_TYPE_LABELS,
} from '../../types/transaction';
import { formatCurrency } from '../../utils/decimal';
import { formatCobolDate } from '../../utils/date';

/**
 * Dashboard / home page. Replaces the legacy MENMAP main menu with a metrics
 * overview (total AUM, active portfolios, recent transactions) and quick links
 * into the primary sections.
 */
export function DashboardPage() {
  const metrics = getDashboardMetrics();

  return (
    <section className="page">
      <header className="page__header">
        <h1 className="page__title">Dashboard</h1>
        <p className="page__subtitle">
          Portfolio management overview at a glance.
        </p>
      </header>

      <div className="metric-grid">
        <MetricCard
          label="Total AUM"
          value={formatCurrency(metrics.totalAum)}
          hint="Assets under management"
        />
        <MetricCard
          label="Active Portfolios"
          value={metrics.activePortfolios}
          hint={`of ${metrics.totalPortfolios} total`}
        />
        <MetricCard
          label="Recent Transactions"
          value={metrics.recentTransactions.length}
          hint="In the latest activity window"
        />
      </div>

      <section className="panel">
        <div className="panel__header">
          <h2 className="panel__title">Recent Transactions</h2>
          <Link className="panel__action" to="/transactions">
            View all
          </Link>
        </div>
        <table className="data-table">
          <thead>
            <tr>
              <th scope="col">Date</th>
              <th scope="col">Portfolio</th>
              <th scope="col">Investment</th>
              <th scope="col">Type</th>
              <th scope="col" className="data-table__num">
                Amount
              </th>
              <th scope="col">Status</th>
            </tr>
          </thead>
          <tbody>
            {metrics.recentTransactions.map((txn) => (
              <tr key={`${txn.portfolioId}-${txn.sequenceNo}`}>
                <td>{formatCobolDate(txn.date)}</td>
                <td>{txn.portfolioId}</td>
                <td>{txn.investmentId}</td>
                <td>{TRANSACTION_TYPE_LABELS[txn.type]}</td>
                <td className="data-table__num">
                  {formatCurrency(txn.amount, txn.currency)}
                </td>
                <td>{TRANSACTION_STATUS_LABELS[txn.status]}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </section>
  );
}
