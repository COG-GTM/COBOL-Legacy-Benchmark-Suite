interface StatusBadgeProps {
  status: string;
  label: string;
}

const STATUS_COLORS: Record<string, string> = {
  A: 'bg-emerald-100 text-emerald-700',
  Active: 'bg-emerald-100 text-emerald-700',
  C: 'bg-slate-100 text-slate-600',
  Closed: 'bg-slate-100 text-slate-600',
  S: 'bg-amber-100 text-amber-700',
  Suspended: 'bg-amber-100 text-amber-700',
  P: 'bg-blue-100 text-blue-700',
  Pending: 'bg-blue-100 text-blue-700',
  D: 'bg-emerald-100 text-emerald-700',
  Done: 'bg-emerald-100 text-emerald-700',
  F: 'bg-red-100 text-red-700',
  Failed: 'bg-red-100 text-red-700',
  R: 'bg-indigo-100 text-indigo-700',
  Ready: 'bg-indigo-100 text-indigo-700',
  Reversed: 'bg-purple-100 text-purple-700',
  W: 'bg-amber-100 text-amber-700',
  Waiting: 'bg-amber-100 text-amber-700',
  E: 'bg-red-100 text-red-700',
  Error: 'bg-red-100 text-red-700',
  SUCC: 'bg-emerald-100 text-emerald-700',
  FAIL: 'bg-red-100 text-red-700',
  WARN: 'bg-amber-100 text-amber-700',
};

export default function StatusBadge({ status, label }: StatusBadgeProps) {
  const color = STATUS_COLORS[status] ?? 'bg-slate-100 text-slate-600';
  return (
    <span
      className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${color}`}
    >
      {label}
    </span>
  );
}
