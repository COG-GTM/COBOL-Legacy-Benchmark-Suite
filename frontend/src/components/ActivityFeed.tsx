import { transactions } from '../data/mockData';
import { formatCurrency, formatDate, cn } from '../lib/format';
import { ArrowUpRight, ArrowDownRight, ArrowRightLeft, Receipt } from 'lucide-react';

const typeIcons = {
  BUY: ArrowUpRight,
  SELL: ArrowDownRight,
  TRANSFER: ArrowRightLeft,
  FEE: Receipt,
};

const typeColors = {
  BUY: 'text-gain bg-gain-bg',
  SELL: 'text-loss bg-loss-bg',
  TRANSFER: 'text-info bg-info-bg',
  FEE: 'text-warning bg-warning-bg',
};

export default function ActivityFeed() {
  const recent = transactions.slice(0, 8);

  return (
    <div className="space-y-1">
      {recent.map((tx, index) => {
        const Icon = typeIcons[tx.type];
        return (
          <div
            key={tx.transactionId}
            className="flex items-center gap-3 px-3 py-2.5 rounded-lg hover:bg-surface-alt transition-colors animate-fade-in-up"
            style={{ animationDelay: `${index * 60}ms` }}
          >
            <div className={cn('p-1.5 rounded-lg', typeColors[tx.type])}>
              <Icon size={14} />
            </div>
            <div className="flex-1 min-w-0">
              <div className="flex items-center justify-between">
                <p className="text-sm font-medium text-text-primary truncate">{tx.symbol}</p>
                <p
                  className={cn(
                    'text-sm font-semibold tabular-nums',
                    tx.type === 'SELL' ? 'text-loss' : 'text-text-primary'
                  )}
                >
                  {tx.type === 'SELL' ? '-' : ''}
                  {formatCurrency(tx.amount)}
                </p>
              </div>
              <div className="flex items-center justify-between">
                <p className="text-xs text-text-muted">
                  {tx.type} &middot; {tx.quantity > 0 ? `${tx.quantity} shares` : 'Fee'}
                </p>
                <p className="text-xs text-text-muted">{formatDate(tx.date)}</p>
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
}
