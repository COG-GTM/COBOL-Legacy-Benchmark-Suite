import { useState, useMemo } from 'react';
import { DollarSign, TrendingUp, BarChart3, Activity } from 'lucide-react';
import MetricCard from '../components/MetricCard';
import PerformanceChart from '../components/PerformanceChart';
import ActivityFeed from '../components/ActivityFeed';
import MiniSparkline from '../components/MiniSparkline';
import { useLivePositions, useTotalPortfolioValue, useAnimatedValue } from '../hooks/useLiveData';
import { portfolios, transactions } from '../data/mockData';
import { formatCurrency, formatPercent, cn } from '../lib/format';

type TimeRange = '1M' | '3M' | '6M' | '1Y';

export default function Dashboard() {
  const [timeRange, setTimeRange] = useState<TimeRange>('6M');
  const livePositions = useLivePositions(undefined, 4000);
  const totalValue = useTotalPortfolioValue(livePositions);
  const animatedTotal = useAnimatedValue(totalValue);

  const totalCostBasis = livePositions.reduce((s, p) => s + p.costBasis, 0);
  const totalGainLoss = totalValue - totalCostBasis;
  const totalGainLossPercent = totalCostBasis > 0 ? (totalGainLoss / totalCostBasis) * 100 : 0;
  const animatedGL = useAnimatedValue(totalGainLoss);

  const activePortfolios = portfolios.filter(p => p.status === 'Active').length;

  const sparkData = useMemo(() => {
    const uniqueSymbols = [...new Set(livePositions.map(p => p.symbol))];
    return uniqueSymbols.slice(0, 6).map(sym => {
      const base = livePositions.find(p => p.symbol === sym)?.currentPrice ?? 100;
      const points: number[] = [];
      let v = base * 0.95;
      for (let i = 0; i < 20; i++) {
        v += (Math.random() - 0.48) * base * 0.008;
        points.push(v);
      }
      return { symbol: sym, data: points };
    });
  }, [livePositions]);

  const topHoldings = useMemo(() => {
    const sorted = [...livePositions].sort((a, b) => b.marketValue - a.marketValue);
    return sorted.slice(0, 6);
  }, [livePositions]);

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold text-text-primary">Dashboard</h2>
        <p className="text-text-secondary text-sm mt-1">
          Real-time portfolio overview across {portfolios.length} accounts
        </p>
      </div>

      {/* Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 stagger-children">
        <MetricCard
          title="Total Portfolio Value"
          value={formatCurrency(animatedTotal)}
          change={`${formatPercent(totalGainLossPercent)} all time`}
          changeType={totalGainLoss >= 0 ? 'gain' : 'loss'}
          icon={<DollarSign size={20} />}
          iconColor="bg-accent-1/15 text-accent-1"
        />
        <MetricCard
          title="Unrealized Gain/Loss"
          value={formatCurrency(animatedGL)}
          change={formatPercent(totalGainLossPercent)}
          changeType={totalGainLoss >= 0 ? 'gain' : 'loss'}
          icon={<TrendingUp size={20} />}
          iconColor={totalGainLoss >= 0 ? 'bg-gain-bg text-gain' : 'bg-loss-bg text-loss'}
        />
        <MetricCard
          title="Active Portfolios"
          value={String(activePortfolios)}
          change={`${portfolios.length} total accounts`}
          changeType="neutral"
          icon={<BarChart3 size={20} />}
          iconColor="bg-info-bg text-info"
        />
        <MetricCard
          title="Recent Transactions"
          value={String(transactions.length)}
          change="Last 30 days"
          changeType="neutral"
          icon={<Activity size={20} />}
          iconColor="bg-warning-bg text-warning"
        />
      </div>

      {/* Charts + Activity */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Performance Chart */}
        <div className="lg:col-span-2 bg-surface rounded-xl border border-border p-5">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h3 className="font-semibold text-lg text-text-primary">Portfolio Performance</h3>
              <p className="text-sm text-text-muted">vs. S&P 500 Benchmark</p>
            </div>
            <div className="flex gap-1 bg-surface-secondary rounded-lg p-1">
              {(['1M', '3M', '6M', '1Y'] as TimeRange[]).map(range => (
                <button
                  key={range}
                  onClick={() => setTimeRange(range)}
                  className={cn(
                    'px-3 py-1 text-xs font-medium rounded-md transition-all',
                    timeRange === range
                      ? 'bg-accent-1 text-surface-dark shadow-sm'
                      : 'text-text-muted hover:text-text-primary'
                  )}
                >
                  {range}
                </button>
              ))}
            </div>
          </div>
          <PerformanceChart timeRange={timeRange} />
        </div>

        {/* Activity Feed */}
        <div className="bg-surface rounded-xl border border-border p-5">
          <h3 className="font-semibold text-lg text-text-primary mb-4">Recent Activity</h3>
          <ActivityFeed />
        </div>
      </div>

      {/* Top Holdings */}
      <div className="bg-surface rounded-xl border border-border p-5">
        <h3 className="font-semibold text-lg text-text-primary mb-4">Top Holdings</h3>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 stagger-children">
          {topHoldings.map((pos, i) => {
            const gainLoss = pos.marketValue - pos.costBasis;
            const gainLossPercent = pos.costBasis > 0 ? (gainLoss / pos.costBasis) * 100 : 0;
            const priceChange = pos.currentPrice - pos.previousPrice;
            const spark = sparkData.find(s => s.symbol === pos.symbol);

            return (
              <div
                key={pos.investmentId}
                className="border border-border rounded-xl p-4 bg-surface-alt hover:border-accent-1/30 transition-all duration-300"
                style={{ animationDelay: `${i * 80}ms` }}
              >
                <div className="flex items-start justify-between mb-3">
                  <div>
                    <p className="font-semibold text-sm text-text-primary">{pos.symbol}</p>
                    <p className="text-xs text-text-muted truncate max-w-[140px]">{pos.name}</p>
                  </div>
                  {spark && <MiniSparkline data={spark.data} />}
                </div>
                <div className="flex items-end justify-between">
                  <div>
                    <p className="text-lg font-bold tabular-nums text-text-primary">
                      {formatCurrency(pos.currentPrice)}
                    </p>
                    <p
                      className={cn(
                        'text-xs font-medium tabular-nums',
                        priceChange >= 0 ? 'text-gain' : 'text-loss'
                      )}
                    >
                      {priceChange >= 0 ? '+' : ''}
                      {formatCurrency(priceChange)} today
                    </p>
                  </div>
                  <div className="text-right">
                    <p className="text-sm text-text-secondary tabular-nums">
                      {formatCurrency(pos.marketValue, true)}
                    </p>
                    <p
                      className={cn(
                        'text-xs font-medium tabular-nums',
                        gainLoss >= 0 ? 'text-gain' : 'text-loss'
                      )}
                    >
                      {formatPercent(gainLossPercent)}
                    </p>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
