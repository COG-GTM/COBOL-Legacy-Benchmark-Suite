import { useState, useMemo } from 'react';
import { Search, ArrowLeft, Briefcase, TrendingUp, TrendingDown } from 'lucide-react';
import { portfolios } from '../data/mockData';
import { useLivePositions, useTotalPortfolioValue, useAnimatedValue } from '../hooks/useLiveData';
import StatusBadge, { getPortfolioStatusVariant } from '../components/StatusBadge';
import MiniSparkline from '../components/MiniSparkline';
import { formatCurrency, formatPercent, formatDate, cn } from '../lib/format';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts';

const COLORS = ['#22d3ee', '#60a5fa', '#818cf8', '#a78bfa', '#4ade80', '#fbbf24', '#f87171', '#fb923c'];

export default function PortfolioInquiry() {
  const [accountInput, setAccountInput] = useState('');
  const [selectedPortfolioId, setSelectedPortfolioId] = useState<string | null>(null);
  const [searchError, setSearchError] = useState<string | null>(null);

  const selectedPortfolio = portfolios.find(p => p.portfolioId === selectedPortfolioId);
  const livePositions = useLivePositions(selectedPortfolioId ?? undefined, 4000);
  const liveTotal = useTotalPortfolioValue(livePositions);
  const animatedTotal = useAnimatedValue(liveTotal);

  const handleSearch = () => {
    setSearchError(null);
    const trimmed = accountInput.trim();

    const found = portfolios.find(
      p => p.accountNumber === trimmed || p.portfolioId.toUpperCase() === trimmed.toUpperCase()
    );

    if (found) {
      setSelectedPortfolioId(found.portfolioId);
    } else {
      setSearchError(`No portfolio found for "${trimmed}". Try: ${portfolios.map(p => p.accountNumber).join(', ')}`);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') handleSearch();
  };

  const allocationData = useMemo(() => {
    return livePositions.map(p => ({
      name: p.symbol,
      value: p.marketValue,
      fullName: p.name,
    }));
  }, [livePositions]);

  const sparkDataMap = useMemo(() => {
    const map: Record<string, number[]> = {};
    livePositions.forEach(p => {
      const points: number[] = [];
      let v = p.costBasis / p.quantity;
      for (let i = 0; i < 20; i++) {
        v += (Math.random() - 0.47) * v * 0.005;
        points.push(v);
      }
      points.push(p.currentPrice);
      map[p.investmentId] = points;
    });
    return map;
  }, [livePositions]);

  if (!selectedPortfolio) {
    return (
      <div className="space-y-6">
        <div>
          <h2 className="text-2xl font-bold text-text-primary">Portfolio Inquiry</h2>
          <p className="text-text-secondary text-sm mt-1">
            Search by account number or portfolio ID
          </p>
        </div>

        {/* Search */}
        <div className="bg-surface rounded-xl border border-border p-8 max-w-xl mx-auto">
          <div className="text-center mb-6">
            <div className="w-14 h-14 rounded-full bg-accent-1/15 flex items-center justify-center mx-auto mb-4">
              <Search size={24} className="text-accent-1" />
            </div>
            <h3 className="text-lg font-semibold text-text-primary">Look Up Portfolio</h3>
            <p className="text-sm text-text-muted mt-1">
              Enter an account number (e.g. 1000000001) or portfolio ID (e.g. PORT0001)
            </p>
          </div>

          <div className="flex gap-2">
            <input
              type="text"
              value={accountInput}
              onChange={e => setAccountInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Account number or portfolio ID..."
              className="flex-1 px-4 py-2.5 rounded-lg border border-border bg-surface-secondary
                         text-sm text-text-primary focus:outline-none focus:ring-2 focus:ring-accent-1 focus:border-transparent
                         placeholder:text-text-muted"
              autoFocus
            />
            <button
              onClick={handleSearch}
              disabled={!accountInput.trim()}
              className="px-5 py-2.5 bg-accent-1 text-surface-dark text-sm font-medium rounded-lg
                         hover:bg-accent-1/90 disabled:opacity-40 disabled:cursor-not-allowed
                         transition-colors shadow-sm"
            >
              Search
            </button>
          </div>

          {searchError && (
            <div className="mt-4 p-3 rounded-lg bg-loss-bg text-loss text-sm">
              {searchError}
            </div>
          )}

          {/* Quick access cards */}
          <div className="mt-8">
            <p className="text-xs text-text-muted uppercase tracking-wider font-medium mb-3">
              Quick Access
            </p>
            <div className="space-y-2">
              {portfolios.slice(0, 4).map(p => (
                <button
                  key={p.portfolioId}
                  onClick={() => setSelectedPortfolioId(p.portfolioId)}
                  className="w-full flex items-center justify-between px-4 py-3 rounded-lg border border-border
                             bg-surface-alt hover:border-accent-1/30 transition-all text-left group"
                >
                  <div className="flex items-center gap-3">
                    <Briefcase size={16} className="text-text-muted group-hover:text-accent-1 transition-colors" />
                    <div>
                      <p className="text-sm font-medium text-text-primary">{p.clientName}</p>
                      <p className="text-xs text-text-muted">{p.accountNumber}</p>
                    </div>
                  </div>
                  <div className="text-right">
                    <p className="text-sm font-semibold tabular-nums text-text-primary">{formatCurrency(p.totalValue, true)}</p>
                    <StatusBadge label={p.status} variant={getPortfolioStatusVariant(p.status)} />
                  </div>
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>
    );
  }

  const totalCostBasis = livePositions.reduce((s, p) => s + p.costBasis, 0);
  const totalGainLoss = liveTotal - totalCostBasis;
  const totalGainLossPercent = totalCostBasis > 0 ? (totalGainLoss / totalCostBasis) * 100 : 0;

  return (
    <div className="space-y-6 animate-fade-in-up">
      {/* Header */}
      <div className="flex items-center gap-4">
        <button
          onClick={() => {
            setSelectedPortfolioId(null);
            setAccountInput('');
          }}
          className="p-2 rounded-lg border border-border bg-surface hover:bg-surface-alt transition-colors"
        >
          <ArrowLeft size={18} className="text-text-secondary" />
        </button>
        <div>
          <h2 className="text-2xl font-bold text-text-primary">{selectedPortfolio.clientName}</h2>
          <div className="flex items-center gap-3 mt-1">
            <span className="text-sm text-text-secondary">
              {selectedPortfolio.accountNumber}
            </span>
            <StatusBadge
              label={selectedPortfolio.status}
              variant={getPortfolioStatusVariant(selectedPortfolio.status)}
            />
            <span className="text-xs text-text-muted">
              {selectedPortfolio.clientType} &middot; Opened {formatDate(selectedPortfolio.createDate)}
            </span>
          </div>
        </div>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 stagger-children">
        <div className="bg-surface rounded-xl border border-border p-5">
          <p className="text-sm text-text-muted mb-1">Market Value</p>
          <p className="text-2xl font-bold tabular-nums text-text-primary">{formatCurrency(animatedTotal)}</p>
          <p className="text-xs text-text-muted mt-1">Live &middot; Updates every 4s</p>
        </div>
        <div className="bg-surface rounded-xl border border-border p-5">
          <p className="text-sm text-text-muted mb-1">Unrealized G/L</p>
          <p className={cn('text-2xl font-bold tabular-nums', totalGainLoss >= 0 ? 'text-gain' : 'text-loss')}>
            {totalGainLoss >= 0 ? '+' : ''}{formatCurrency(totalGainLoss)}
          </p>
          <p className={cn('text-xs font-medium', totalGainLoss >= 0 ? 'text-gain' : 'text-loss')}>
            {formatPercent(totalGainLossPercent)}
          </p>
        </div>
        <div className="bg-surface rounded-xl border border-border p-5">
          <p className="text-sm text-text-muted mb-1">Cash Balance</p>
          <p className="text-2xl font-bold tabular-nums text-text-primary">
            {formatCurrency(selectedPortfolio.cashBalance)}
          </p>
          <p className="text-xs text-text-muted mt-1">{selectedPortfolio.currency}</p>
        </div>
      </div>

      {/* Holdings + Allocation */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Holdings Table */}
        <div className="lg:col-span-2 bg-surface rounded-xl border border-border p-5">
          <h3 className="font-semibold text-lg text-text-primary mb-4">Holdings ({livePositions.length})</h3>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border">
                  <th className="text-left py-2 px-3 text-text-heading font-medium text-xs uppercase tracking-wider">Asset</th>
                  <th className="text-right py-2 px-3 text-text-heading font-medium text-xs uppercase tracking-wider">Price</th>
                  <th className="text-right py-2 px-3 text-text-heading font-medium text-xs uppercase tracking-wider">Qty</th>
                  <th className="text-right py-2 px-3 text-text-heading font-medium text-xs uppercase tracking-wider">Mkt Value</th>
                  <th className="text-right py-2 px-3 text-text-heading font-medium text-xs uppercase tracking-wider">G/L</th>
                  <th className="text-center py-2 px-3 text-text-heading font-medium text-xs uppercase tracking-wider">Trend</th>
                </tr>
              </thead>
              <tbody>
                {livePositions.map((pos, idx) => {
                  const gl = pos.marketValue - pos.costBasis;
                  const glPct = pos.costBasis > 0 ? (gl / pos.costBasis) * 100 : 0;
                  const priceChange = pos.currentPrice - pos.previousPrice;
                  const spark = sparkDataMap[pos.investmentId];

                  return (
                    <tr
                      key={pos.investmentId}
                      className={cn(
                        'border-b border-border/50 hover:bg-surface-alt/50 transition-colors',
                        idx % 2 === 1 && 'bg-surface-alt/30'
                      )}
                    >
                      <td className="py-3 px-3">
                        <p className="font-semibold text-text-primary">{pos.symbol}</p>
                        <p className="text-xs text-text-muted truncate max-w-[140px]">{pos.name}</p>
                      </td>
                      <td className="py-3 px-3 text-right tabular-nums">
                        <p className="font-medium text-text-primary">{formatCurrency(pos.currentPrice)}</p>
                        <p className={cn('text-xs', priceChange >= 0 ? 'text-gain' : 'text-loss')}>
                          {priceChange >= 0 ? '+' : ''}{formatCurrency(priceChange)}
                        </p>
                      </td>
                      <td className="py-3 px-3 text-right tabular-nums text-text-secondary">{pos.quantity.toLocaleString()}</td>
                      <td className="py-3 px-3 text-right tabular-nums font-medium text-text-primary">
                        {formatCurrency(pos.marketValue)}
                      </td>
                      <td className="py-3 px-3 text-right">
                        <div className="flex items-center justify-end gap-1">
                          {gl >= 0 ? (
                            <TrendingUp size={14} className="text-gain" />
                          ) : (
                            <TrendingDown size={14} className="text-loss" />
                          )}
                          <span className={cn('tabular-nums text-sm font-medium', gl >= 0 ? 'text-gain' : 'text-loss')}>
                            {formatPercent(glPct)}
                          </span>
                        </div>
                      </td>
                      <td className="py-3 px-3 flex justify-center">
                        {spark && <MiniSparkline data={spark} width={64} height={24} />}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>

        {/* Allocation Chart */}
        <div className="bg-surface rounded-xl border border-border p-5">
          <h3 className="font-semibold text-lg text-text-primary mb-4">Allocation</h3>
          <ResponsiveContainer width="100%" height={220}>
            <PieChart>
              <Pie
                data={allocationData}
                cx="50%"
                cy="50%"
                innerRadius={55}
                outerRadius={85}
                paddingAngle={3}
                dataKey="value"
                stroke="#0f172a"
                strokeWidth={1}
              >
                {allocationData.map((_, index) => (
                  <Cell key={index} fill={COLORS[index % COLORS.length]} />
                ))}
              </Pie>
              <Tooltip
                contentStyle={{
                  backgroundColor: '#1e293b',
                  border: '1px solid #334155',
                  borderRadius: '8px',
                  color: '#ffffff',
                  fontSize: '12px',
                }}
                formatter={(value: number) => [formatCurrency(value), 'Value']}
              />
            </PieChart>
          </ResponsiveContainer>
          <div className="space-y-2 mt-2">
            {allocationData.map((item, i) => {
              const pct = liveTotal > 0 ? (item.value / liveTotal) * 100 : 0;
              return (
                <div key={item.name} className="flex items-center justify-between text-sm">
                  <div className="flex items-center gap-2">
                    <div
                      className="w-3 h-3 rounded-sm"
                      style={{ backgroundColor: COLORS[i % COLORS.length] }}
                    />
                    <span className="font-medium text-text-secondary">{item.name}</span>
                  </div>
                  <span className="text-text-muted tabular-nums">{pct.toFixed(1)}%</span>
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
}
