import { cn } from '../lib/format';

type BadgeVariant = 'success' | 'warning' | 'danger' | 'info' | 'neutral';

interface StatusBadgeProps {
  label: string;
  variant?: BadgeVariant;
}

const variantStyles: Record<BadgeVariant, string> = {
  success: 'bg-gain-bg text-gain',
  warning: 'bg-warning-bg text-warning',
  danger: 'bg-loss-bg text-loss',
  info: 'bg-info-bg text-info',
  neutral: 'bg-surface-alt text-text-muted',
};

export default function StatusBadge({ label, variant = 'neutral' }: StatusBadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium',
        variantStyles[variant]
      )}
    >
      {label}
    </span>
  );
}

export function getTransactionStatusVariant(status: string): BadgeVariant {
  switch (status) {
    case 'Done': return 'success';
    case 'Pending': return 'warning';
    case 'Failed': return 'danger';
    case 'Reversed': return 'info';
    default: return 'neutral';
  }
}

export function getPortfolioStatusVariant(status: string): BadgeVariant {
  switch (status) {
    case 'Active': return 'success';
    case 'Suspended': return 'warning';
    case 'Closed': return 'danger';
    default: return 'neutral';
  }
}

export function getBatchStatusVariant(status: string): BadgeVariant {
  switch (status) {
    case 'Completed': return 'success';
    case 'Running': return 'info';
    case 'Failed': return 'danger';
    case 'Scheduled': return 'neutral';
    default: return 'neutral';
  }
}
