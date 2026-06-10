type StatusVariant = 'success' | 'error' | 'warning' | 'info' | 'neutral';

interface StatusBadgeProps {
  label: string;
  variant?: StatusVariant;
}

const variantStyles: Record<StatusVariant, string> = {
  success: 'bg-emerald-50 text-emerald-700 ring-emerald-600/20',
  error: 'bg-red-50 text-red-700 ring-red-600/20',
  warning: 'bg-amber-50 text-amber-700 ring-amber-600/20',
  info: 'bg-blue-50 text-blue-700 ring-blue-600/20',
  neutral: 'bg-slate-50 text-slate-700 ring-slate-600/20',
};

export function StatusBadge({ label, variant = 'neutral' }: StatusBadgeProps) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ring-1 ring-inset ${variantStyles[variant]}`}
    >
      {label}
    </span>
  );
}

export function getPortfolioStatusVariant(status: string): StatusVariant {
  switch (status) {
    case 'A': return 'success';
    case 'I': return 'warning';
    case 'C': return 'error';
    default: return 'neutral';
  }
}

export function getPortfolioStatusLabel(status: string): string {
  switch (status) {
    case 'A': return 'Active';
    case 'I': return 'Inactive';
    case 'C': return 'Closed';
    default: return status;
  }
}

export function getTransactionStatusVariant(status: string): StatusVariant {
  switch (status) {
    case 'C': return 'success';
    case 'P': return 'warning';
    case 'E': return 'error';
    default: return 'neutral';
  }
}

export function getTransactionStatusLabel(status: string): string {
  switch (status) {
    case 'C': return 'Completed';
    case 'P': return 'Pending';
    case 'E': return 'Error';
    default: return status;
  }
}

export function getBatchStatusVariant(status: string): StatusVariant {
  switch (status) {
    case 'C': return 'success';
    case 'P': return 'info';
    case 'W': return 'neutral';
    case 'E': return 'error';
    default: return 'neutral';
  }
}

export function getBatchStatusLabel(status: string): string {
  switch (status) {
    case 'C': return 'Completed';
    case 'P': return 'Processing';
    case 'W': return 'Waiting';
    case 'E': return 'Error';
    default: return status;
  }
}

export function getAuditStatusVariant(status: string): StatusVariant {
  switch (status) {
    case 'SUCC': return 'success';
    case 'FAIL': return 'error';
    default: return 'neutral';
  }
}

export function getSeverityVariant(severity: string): StatusVariant {
  switch (severity) {
    case 'Error': return 'error';
    case 'Warning': return 'warning';
    default: return 'neutral';
  }
}

export function getPositionStatusVariant(status: string): StatusVariant {
  switch (status) {
    case 'A': return 'success';
    case 'C': return 'error';
    default: return 'neutral';
  }
}

export function getPositionStatusLabel(status: string): string {
  switch (status) {
    case 'A': return 'Active';
    case 'C': return 'Closed';
    default: return status;
  }
}

export function getTransTypeLabel(type: string): string {
  switch (type) {
    case 'BY': return 'Buy';
    case 'SL': return 'Sell';
    case 'FE': return 'Fee';
    default: return type;
  }
}
