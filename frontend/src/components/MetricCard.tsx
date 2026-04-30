import type { ReactNode } from 'react';
import { cn } from '../lib/format';

interface MetricCardProps {
  title: string;
  value: string;
  change?: string;
  changeType?: 'gain' | 'loss' | 'neutral';
  icon: ReactNode;
  iconColor?: string;
}

export default function MetricCard({
  title,
  value,
  change,
  changeType = 'neutral',
  icon,
  iconColor = 'bg-accent-1/15 text-accent-1',
}: MetricCardProps) {
  return (
    <div className="bg-surface rounded-xl border border-border p-5 hover:border-accent-1/30 transition-all duration-300">
      <div className="flex items-start justify-between">
        <div className="space-y-3">
          <p className="text-sm text-text-muted font-medium">{title}</p>
          <p className="text-2xl font-bold tracking-tight text-text-primary">{value}</p>
          {change && (
            <p
              className={cn(
                'text-sm font-medium',
                changeType === 'gain' && 'text-gain',
                changeType === 'loss' && 'text-loss',
                changeType === 'neutral' && 'text-text-secondary'
              )}
            >
              {change}
            </p>
          )}
        </div>
        <div className={cn('p-2.5 rounded-lg', iconColor)}>
          {icon}
        </div>
      </div>
    </div>
  );
}
