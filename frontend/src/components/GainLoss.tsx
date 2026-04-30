import { formatCurrency, formatPercent } from '../utils/format';
import { TrendingUp, TrendingDown, Minus } from 'lucide-react';

interface Props {
  value: number;
  percent?: number;
  showIcon?: boolean;
  size?: 'sm' | 'md' | 'lg';
}

export default function GainLoss({ value, percent, showIcon = true, size = 'md' }: Props) {
  const isPositive = value > 0;
  const isZero = value === 0;
  const color = isPositive ? 'text-green-400' : isZero ? 'text-slate-400' : 'text-red-400';
  const Icon = isPositive ? TrendingUp : isZero ? Minus : TrendingDown;
  const textSize = size === 'sm' ? 'text-sm' : size === 'lg' ? 'text-xl' : 'text-base';

  return (
    <span className={`inline-flex items-center gap-1 ${color} ${textSize}`}>
      {showIcon && <Icon size={size === 'sm' ? 14 : size === 'lg' ? 22 : 16} />}
      {formatCurrency(value)}
      {percent !== undefined && (
        <span className="text-xs opacity-75">({formatPercent(percent)})</span>
      )}
    </span>
  );
}
