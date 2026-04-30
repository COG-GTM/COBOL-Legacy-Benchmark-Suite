import { useLiveMarketTicks } from '../hooks/useLiveData';
import { formatCurrency, formatPercent, cn } from '../lib/format';
import { Radio } from 'lucide-react';

export default function LiveTicker() {
  const ticks = useLiveMarketTicks(2500);

  return (
    <div className="bg-surface-dark text-white h-9 flex items-center overflow-hidden relative z-50">
      <div className="flex items-center gap-2 px-3 border-r border-border-dark h-full shrink-0 bg-surface-dark-secondary">
        <Radio size={12} className="text-gain animate-pulse" />
        <span className="text-[11px] font-medium text-text-muted uppercase tracking-wider">Live</span>
      </div>

      <div className="overflow-hidden flex-1">
        <div className="animate-ticker flex whitespace-nowrap">
          {[...ticks, ...ticks].map((tick, i) => (
            <div
              key={`${tick.symbol}-${i}`}
              className="inline-flex items-center gap-2 px-4"
            >
              <span className="font-semibold text-xs">{tick.symbol}</span>
              <span className="text-xs text-text-dark">
                {formatCurrency(tick.price)}
              </span>
              <span
                className={cn(
                  'text-xs font-medium',
                  tick.change >= 0 ? 'text-gain' : 'text-loss'
                )}
              >
                {formatPercent(tick.changePercent)}
              </span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
