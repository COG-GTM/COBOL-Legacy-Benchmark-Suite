import { STATUS_LABELS } from '../types';

interface Props {
  status: string;
  type?: 'portfolio' | 'transaction' | 'position';
}

export default function StatusBadge({ status }: Props) {
  const label = STATUS_LABELS[status] ?? status;
  const cls =
    status === 'A' || status === 'D' ? 'badge-success' :
    status === 'P' ? 'badge-warning' :
    status === 'F' || status === 'C' ? 'badge-danger' :
    status === 'R' || status === 'S' ? 'badge-info' :
    'badge-info';
  return <span className={cls}>{label}</span>;
}
