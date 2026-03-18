import { TrendingUp, TrendingDown, Minus } from 'lucide-react';
import { cn } from '@/lib/utils';

interface ReportSummaryCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  trend?: 'up' | 'down' | 'flat';
  trendValue?: string;
  color?: 'green' | 'red' | 'yellow' | 'blue' | 'gray';
}

const colorMap = {
  green: {
    bg: 'bg-[#4ADE80]/10',
    text: 'text-[#4ADE80]',
    icon: 'text-[#4ADE80]',
  },
  red: {
    bg: 'bg-[#F87171]/10',
    text: 'text-[#F87171]',
    icon: 'text-[#F87171]',
  },
  yellow: {
    bg: 'bg-amber-500/10',
    text: 'text-amber-400',
    icon: 'text-amber-400',
  },
  blue: {
    bg: 'bg-[#22D3EE]/10',
    text: 'text-[#22D3EE]',
    icon: 'text-[#22D3EE]',
  },
  gray: {
    bg: 'bg-[#94A3B8]/10',
    text: 'text-[#94A3B8]',
    icon: 'text-[#94A3B8]',
  },
};

const TrendIcon = {
  up: TrendingUp,
  down: TrendingDown,
  flat: Minus,
};

export function ReportSummaryCard({
  title,
  value,
  subtitle,
  trend,
  trendValue,
  color = 'blue',
}: ReportSummaryCardProps) {
  const colors = colorMap[color];

  return (
    <div className={cn('rounded-xl border border-[#334155] bg-[#1E293B] p-5')}>
      <p className="text-sm font-medium text-[#94A3B8]">{title}</p>
      <p className={cn('mt-2 text-2xl font-bold', colors.text)}>{value}</p>
      {(subtitle || trend) && (
        <div className="mt-2 flex items-center gap-2">
          {trend && (
            <span className={cn('flex items-center gap-1', colors.icon)}>
              {(() => {
                const Icon = TrendIcon[trend];
                return <Icon className="h-3.5 w-3.5" />;
              })()}
              {trendValue && <span className="text-xs font-medium">{trendValue}</span>}
            </span>
          )}
          {subtitle && <span className="text-xs text-[#94A3B8]">{subtitle}</span>}
        </div>
      )}
    </div>
  );
}
