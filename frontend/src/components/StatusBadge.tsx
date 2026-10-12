import { positionStatusLabel } from '../format/labels';

/** BR-11 status badge. Closed positions are de-emphasised (Q-04 default: show all). */
export function StatusBadge({ status }: { status: string }) {
  return <span className={`badge badge--${status.toLowerCase()}`}>{positionStatusLabel(status)}</span>;
}
