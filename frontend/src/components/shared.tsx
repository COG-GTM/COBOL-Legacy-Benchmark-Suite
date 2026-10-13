import type { ReactNode } from 'react';
import type { PositionStatus, TransactionStatus, TransactionType } from '../api/types';

export function SearchIcon() {
  return (
    <svg width="15" height="15" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.6" aria-hidden="true">
      <circle cx="7" cy="7" r="5" />
      <path d="m11 11 3.5 3.5" strokeLinecap="round" />
    </svg>
  );
}

export function StatusPill({ status }: { status: PositionStatus | TransactionStatus }) {
  return <span className={`pill s-${status.toLowerCase()}`}>{status}</span>;
}

const TYPE_ICON: Record<TransactionType, string> = { Buy: '+', Sell: '−', Transfer: '⇄', Fee: '$' };

export function TypeCell({ type, direction }: { type: TransactionType; direction: 1 | -1 }) {
  const label = type === 'Transfer' ? (direction > 0 ? 'Transfer in' : 'Transfer out') : type;
  return (
    <span className={`type ${type.toLowerCase()}`}>
      <span className="ic" aria-hidden="true">{TYPE_ICON[type]}</span>
      {label}
    </span>
  );
}

export function SortHeader<K extends string>({
  label, sortKey, sort, onSort, align,
}: {
  label: string;
  sortKey: K;
  sort: { key: K; dir: 'asc' | 'desc' };
  onSort: (key: K) => void;
  align?: 'r';
}) {
  const active = sort.key === sortKey;
  return (
    <th className={align} aria-sort={active ? (sort.dir === 'asc' ? 'ascending' : 'descending') : undefined} scope="col">
      <button type="button" onClick={() => onSort(sortKey)}>
        {label}
        <span className="arr" aria-hidden="true">{active ? (sort.dir === 'asc' ? '▲' : '▼') : '↕'}</span>
      </button>
    </th>
  );
}

export function Empty({ title, children }: { title: string; children?: ReactNode }) {
  return (
    <div className="empty" role="status">
      <b>{title}</b>
      {children}
    </div>
  );
}
