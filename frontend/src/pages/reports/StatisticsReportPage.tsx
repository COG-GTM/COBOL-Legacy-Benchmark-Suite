import { useMemo } from 'react';
import { Briefcase, TrendingUp, ArrowLeftRight, DollarSign } from 'lucide-react';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card } from '@/components/ui/Card';
import { portfolios, positions, transactions, errorEntries } from '@/data/mockData';

const currencyFmt = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', minimumFractionDigits: 2 });
const numberFmt = new Intl.NumberFormat('en-US', { minimumFractionDigits: 0, maximumFractionDigits: 0 });
const sharesFmt = new Intl.NumberFormat('en-US', { minimumFractionDigits: 3, maximumFractionDigits: 3 });

const ERROR_DESCRIPTIONS: Record<string, string> = {
  E001: 'Invalid account number format',
  E002: 'Fund ID not found in master file',
  E003: 'Insufficient share balance for sale',
  E004: 'VSAM file I/O error on master update',
  W001: 'Transaction amount exceeds daily limit',
  W002: 'Duplicate transaction reference detected',
};

interface BarChartItem {
  label: string;
  value: number;
  color: string;
}

function HorizontalBarChart({ items, formatValue }: { items: BarChartItem[]; formatValue?: (v: number) => string }) {
  const maxValue = Math.max(...items.map((i) => i.value), 1);
  return (
    <div className="space-y-3">
      {items.map((item) => (
        <div key={item.label}>
          <div className="flex items-center justify-between mb-1">
            <span className="text-sm font-medium text-slate-700">{item.label}</span>
            <span className="text-sm font-semibold text-slate-900">
              {formatValue ? formatValue(item.value) : item.value.toLocaleString()}
            </span>
          </div>
          <div className="w-full bg-slate-100 rounded-full h-3">
            <div
              className={`h-3 rounded-full ${item.color}`}
              style={{ width: `${(item.value / maxValue) * 100}%` }}
            />
          </div>
        </div>
      ))}
    </div>
  );
}

function DonutChart({ items }: { items: BarChartItem[] }) {
  const total = items.reduce((s, i) => s + i.value, 0);
  if (total === 0) return <p className="text-sm text-slate-500 text-center">No data</p>;

  const segments = items.reduce<Array<BarChartItem & { pct: number; start: number }>>((acc, item) => {
    const pct = (item.value / total) * 100;
    const start = acc.length > 0 ? acc[acc.length - 1].start + acc[acc.length - 1].pct : 0;
    acc.push({ ...item, pct, start });
    return acc;
  }, []);

  const colorMap: Record<string, string> = {
    'bg-amber-400': '#fbbf24',
    'bg-emerald-500': '#10b981',
    'bg-red-500': '#ef4444',
  };

  const conicGradient = segments
    .map((s) => `${colorMap[s.color] ?? '#94a3b8'} ${s.start}% ${s.start + s.pct}%`)
    .join(', ');

  return (
    <div className="flex items-center gap-6">
      <div
        className="w-32 h-32 rounded-full flex-shrink-0"
        style={{
          background: `conic-gradient(${conicGradient})`,
          mask: 'radial-gradient(farthest-side, transparent 60%, black 60%)',
          WebkitMask: 'radial-gradient(farthest-side, transparent 60%, black 60%)',
        }}
      />
      <div className="space-y-2">
        {segments.map((s) => (
          <div key={s.label} className="flex items-center gap-2">
            <div className={`w-3 h-3 rounded-full ${s.color}`} />
            <span className="text-sm text-slate-700">{s.label}</span>
            <span className="text-sm font-semibold text-slate-900">{s.value}</span>
            <span className="text-xs text-slate-500">({s.pct.toFixed(1)}%)</span>
          </div>
        ))}
      </div>
    </div>
  );
}

export function StatisticsReportPage() {
  const kpis = useMemo(() => {
    const activePortfolios = portfolios.filter((p) => p.status === 'A').length;
    const activePositions = positions.filter((p) => p.status === 'A').length;
    const pending = transactions.filter((t) => t.status === 'P').length;
    const complete = transactions.filter((t) => t.status === 'C').length;
    const error = transactions.filter((t) => t.status === 'E').length;
    const totalValueUnderMgmt = portfolios
      .filter((p) => p.status === 'A')
      .reduce((s, p) => s + p.totalValue, 0);
    return { activePortfolios, activePositions, transCount: transactions.length, pending, complete, error, totalValueUnderMgmt };
  }, []);

  const txnByType = useMemo((): BarChartItem[] => {
    const buys = transactions.filter((t) => t.transType === 'BY').length;
    const sells = transactions.filter((t) => t.transType === 'SL').length;
    const fees = transactions.filter((t) => t.transType === 'FE').length;
    return [
      { label: 'Buy', value: buys, color: 'bg-emerald-500' },
      { label: 'Sell', value: sells, color: 'bg-blue-500' },
      { label: 'Fee', value: fees, color: 'bg-amber-500' },
    ];
  }, []);

  const txnStatusDist = useMemo((): BarChartItem[] => [
    { label: 'Pending', value: kpis.pending, color: 'bg-amber-400' },
    { label: 'Complete', value: kpis.complete, color: 'bg-emerald-500' },
    { label: 'Error', value: kpis.error, color: 'bg-red-500' },
  ], [kpis]);

  const topPortfolios = useMemo((): BarChartItem[] => {
    const colors = ['bg-blue-500', 'bg-violet-500', 'bg-emerald-500', 'bg-amber-500', 'bg-rose-500', 'bg-cyan-500', 'bg-indigo-500', 'bg-teal-500', 'bg-orange-500', 'bg-pink-500'];
    return [...portfolios]
      .filter((p) => p.status === 'A')
      .sort((a, b) => b.totalValue - a.totalValue)
      .slice(0, 10)
      .map((p, i) => ({
        label: p.name,
        value: p.totalValue,
        color: colors[i % colors.length],
      }));
  }, []);

  const txnStats = useMemo(() => {
    const types: Array<'BY' | 'SL' | 'FE'> = ['BY', 'SL', 'FE'];
    const labels: Record<string, string> = { BY: 'Buy', SL: 'Sell', FE: 'Fee' };
    return types.map((t) => {
      const txns = transactions.filter((tr) => tr.transType === t);
      const totalAmount = txns.reduce((s, tr) => s + tr.amount, 0);
      return {
        type: labels[t],
        count: txns.length,
        totalAmount,
        avgAmount: txns.length > 0 ? totalAmount / txns.length : 0,
      };
    });
  }, []);

  const positionStats = useMemo(() => {
    const activePos = positions.filter((p) => p.status === 'A');
    const totalShares = activePos.reduce((s, p) => s + p.shareBalance, 0);
    const totalCostBasis = activePos.reduce((s, p) => s + p.costBasis, 0);
    const avgCost = activePos.length > 0 ? totalCostBasis / activePos.length : 0;
    return { totalShares, totalCostBasis, avgCost };
  }, []);

  const errorStats = useMemo(() => {
    const countMap = new Map<string, number>();
    for (const e of errorEntries) {
      countMap.set(e.code, (countMap.get(e.code) ?? 0) + 1);
    }
    return Array.from(countMap.entries())
      .map(([code, count]) => ({ code, count, description: ERROR_DESCRIPTIONS[code] ?? code }))
      .sort((a, b) => a.code.localeCompare(b.code));
  }, []);

  const now = new Date();
  const reportPeriod = now.toLocaleDateString('en-US', { year: 'numeric', month: 'long' });

  const kpiCards = [
    { label: 'Active Portfolios', value: kpis.activePortfolios.toString(), icon: <Briefcase className="w-6 h-6" />, color: 'text-blue-600 bg-blue-50' },
    { label: 'Active Positions', value: kpis.activePositions.toString(), icon: <TrendingUp className="w-6 h-6" />, color: 'text-emerald-600 bg-emerald-50' },
    { label: 'Total Transactions', value: `${kpis.transCount} (${kpis.pending}P / ${kpis.complete}C / ${kpis.error}E)`, icon: <ArrowLeftRight className="w-6 h-6" />, color: 'text-violet-600 bg-violet-50' },
    { label: 'Value Under Management', value: currencyFmt.format(kpis.totalValueUnderMgmt), icon: <DollarSign className="w-6 h-6" />, color: 'text-amber-600 bg-amber-50' },
  ];

  return (
    <div>
      <PageHeader
        title="System Statistics"
        description={`Reporting period: ${reportPeriod}`}
      />

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        {kpiCards.map((card) => (
          <div key={card.label} className="bg-white rounded-lg border border-slate-200 shadow-sm p-5">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-slate-500">{card.label}</p>
                <p className="text-xl font-bold text-slate-900 mt-1">{card.value}</p>
              </div>
              <div className={`p-3 rounded-lg ${card.color}`}>{card.icon}</div>
            </div>
          </div>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
        <Card title="Transaction Volume by Type">
          <HorizontalBarChart items={txnByType} />
        </Card>

        <Card title="Transaction Status Distribution">
          <DonutChart items={txnStatusDist} />
        </Card>
      </div>

      <Card title="Portfolio Value Distribution — Top 10" className="mb-6">
        <HorizontalBarChart items={topPortfolios} formatValue={(v) => currencyFmt.format(v)} />
      </Card>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <Card title="Transaction Statistics">
          <div className="overflow-x-auto -m-6 mt-0">
            <table className="min-w-full divide-y divide-slate-200">
              <thead className="bg-slate-50">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase">Type</th>
                  <th className="px-4 py-3 text-right text-xs font-semibold text-slate-600 uppercase">Count</th>
                  <th className="px-4 py-3 text-right text-xs font-semibold text-slate-600 uppercase">Total Amount</th>
                  <th className="px-4 py-3 text-right text-xs font-semibold text-slate-600 uppercase">Avg Amount</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200">
                {txnStats.map((s) => (
                  <tr key={s.type} className="hover:bg-slate-50 transition-colors">
                    <td className="px-4 py-3 text-sm font-medium text-slate-900">{s.type}</td>
                    <td className="px-4 py-3 text-sm text-slate-700 text-right">{numberFmt.format(s.count)}</td>
                    <td className="px-4 py-3 text-sm text-slate-700 text-right">{currencyFmt.format(s.totalAmount)}</td>
                    <td className="px-4 py-3 text-sm text-slate-700 text-right">{currencyFmt.format(s.avgAmount)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>

        <Card title="Position Statistics">
          <div className="space-y-4">
            <div className="flex justify-between items-center py-2 border-b border-slate-100">
              <span className="text-sm text-slate-600">Total Shares</span>
              <span className="text-sm font-semibold text-slate-900">{sharesFmt.format(positionStats.totalShares)}</span>
            </div>
            <div className="flex justify-between items-center py-2 border-b border-slate-100">
              <span className="text-sm text-slate-600">Total Cost Basis</span>
              <span className="text-sm font-semibold text-slate-900">{currencyFmt.format(positionStats.totalCostBasis)}</span>
            </div>
            <div className="flex justify-between items-center py-2">
              <span className="text-sm text-slate-600">Avg Cost Basis per Position</span>
              <span className="text-sm font-semibold text-slate-900">{currencyFmt.format(positionStats.avgCost)}</span>
            </div>
          </div>
        </Card>

        <Card title="Error Statistics">
          <div className="overflow-x-auto -m-6 mt-0">
            <table className="min-w-full divide-y divide-slate-200">
              <thead className="bg-slate-50">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase">Code</th>
                  <th className="px-4 py-3 text-right text-xs font-semibold text-slate-600 uppercase">Count</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase">Description</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200">
                {errorStats.map((e) => (
                  <tr key={e.code} className="hover:bg-slate-50 transition-colors">
                    <td className="px-4 py-3 text-sm font-mono text-slate-900">{e.code}</td>
                    <td className="px-4 py-3 text-sm text-slate-700 text-right">{e.count}</td>
                    <td className="px-4 py-3 text-sm text-slate-600">{e.description}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      </div>
    </div>
  );
}
