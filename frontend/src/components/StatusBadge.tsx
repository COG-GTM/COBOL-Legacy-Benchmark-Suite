import {
  PORTFOLIO_STATUS_LABELS,
  type PortfolioStatus,
} from '../types/portfolio';

const STATUS_CLASS: Record<PortfolioStatus, string> = {
  A: 'badge badge--active',
  C: 'badge badge--closed',
  S: 'badge badge--suspended',
};

/** Visual indicator for PORT-STATUS (Active / Closed / Suspended). */
export function StatusBadge({ status }: { status: PortfolioStatus }) {
  return (
    <span className={STATUS_CLASS[status]} data-testid="status-badge">
      <span className="badge__dot" aria-hidden="true" />
      {PORTFOLIO_STATUS_LABELS[status]}
    </span>
  );
}
