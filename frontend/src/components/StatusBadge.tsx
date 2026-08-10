import {
  PORTFOLIO_STATUS_LABELS,
  type PortfolioStatus,
} from '../types/portfolio';
import {
  POSITION_STATUS_LABELS,
  type PositionStatus,
} from '../types/position';

/**
 * Any single-character status code carried by a domain record. Portfolios use
 * PORT-STATUS (A/C/S); positions use POS-STATUS (A/C/P). The character sets
 * overlap in meaning (A=Active, C=Closed) and are disjoint otherwise, so a
 * single badge component can render either.
 */
export type BadgeStatus = PortfolioStatus | PositionStatus;

const STATUS_LABELS: Record<BadgeStatus, string> = {
  ...PORTFOLIO_STATUS_LABELS,
  ...POSITION_STATUS_LABELS,
};

const STATUS_CLASS: Record<BadgeStatus, string> = {
  A: 'badge badge--active',
  C: 'badge badge--closed',
  S: 'badge badge--suspended',
  P: 'badge badge--pending',
};

/** Visual indicator for a record status (PORT-STATUS or POS-STATUS). */
export function StatusBadge({ status }: { status: BadgeStatus }) {
  return (
    <span className={STATUS_CLASS[status]} data-testid="status-badge">
      <span className="badge__dot" aria-hidden="true" />
      {STATUS_LABELS[status]}
    </span>
  );
}
