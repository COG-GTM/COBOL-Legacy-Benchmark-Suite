import { cn } from '@/lib/utils';
import { ArrowUp, ArrowDown, Minus } from 'lucide-react';

interface MetricCardProps {
  title: string;
  value: string | number;
  unit?: string;
  trend?: 'up' | 'down' | 'flat';
  trendValue?: string;
  color?: 'green' | 'yellow' | 'red' | 'blue' | 'default';
  subtitle?: string;
}

const trendIcons = {
  up: ArrowUp,
  down: ArrowDown,
  flat: Minus,
};

const trendColors = {
  up: 'text-green-500',
  down: 'text-red-500',
  flat: 'text-gray-400',
};

const valueColors = {
  green: 'text-green-600',
  yellow: 'text-yellow-600',
  red: 'text-red-600',
  blue: 'text-blue-600',
  default: 'text-gray-900',
};

export default function MetricCard({ title, value, unit, trend, trendValue, color = 'default', subtitle }: MetricCardProps) {
  const TrendIcon = trend ? trendIcons[trend] : null;

  return (
    <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-4">
      <p className="text-sm font-medium text-gray-500">{title}</p>
      <div className="mt-1 flex items-baseline gap-2">
        <span className={cn('text-2xl font-semibold', valueColors[color])}>
          {value}
          {unit && <span className="text-sm font-normal text-gray-500 ml-0.5">{unit}</span>}
        </span>
        {trend && TrendIcon && (
          <span className={cn('inline-flex items-center text-sm', trendColors[trend])}>
            <TrendIcon className="h-4 w-4" />
            {trendValue && <span className="ml-0.5">{trendValue}</span>}
          </span>
        )}
      </div>
      {subtitle && <p className="mt-1 text-xs text-gray-400">{subtitle}</p>}
    </div>
  );
}
