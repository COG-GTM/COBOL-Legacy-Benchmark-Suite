import {
  PORTFOLIO_STATUS_LABELS,
  type PortfolioStatus,
} from '../types/portfolio';
import {
  POSITION_STATUS_LABELS,
  type PositionStatus,
} from '../types/position';
import {
  TRANSACTION_STATUS_LABELS,
  type TransactionStatus,
} from '../types/transaction';

/**
 * Any single-character status code carried by a domain record. Portfolios use
 * PORT-STATUS (A/C/S); positions use POS-STATUS (A/C/P); transactions use
 * TRN-STATUS (P/D/F/R). The character sets overlap in meaning (A=Active,
 * C=Closed, P=Pending) and are disjoint otherwise, so a single badge component
 * can render any of them.
 */
export type BadgeStatus = PortfolioStatus | PositionStatus | TransactionStatus;

const STATUS_LABELS: Record<BadgeStatus, string> = {
  ...PORTFOLIO_STATUS_LABELS,
  ...POSITION_STATUS_LABELS,
  ...TRANSACTION_STATUS_LABELS,
};

const STATUS_CLASS: Record<BadgeStatus, string> = {
  A: 'badge badge--active',
  C: 'badge badge--closed',
  S: 'badge badge--suspended',
  P: 'badge badge--pending',
  D: 'badge badge--done',
  F: 'badge badge--failed',
  R: 'badge badge--reversed',
};

/** Visual indicator for a record status (PORT-STATUS, POS-STATUS or TRN-STATUS). */
export function StatusBadge({ status }: { status: BadgeStatus }) {
  return (
    <span className={STATUS_CLASS[status]} data-testid="status-badge">
      <span className="badge__dot" aria-hidden="true" />
      {STATUS_LABELS[status]}
    </span>
  );
}
