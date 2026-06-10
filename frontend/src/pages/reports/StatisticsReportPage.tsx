import { useEffect, useMemo, useState } from 'react';
import { Briefcase, ArrowLeftRight, DollarSign, TrendingUp } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { PageHeader } from '@/components/ui/PageHeader';
import { LoadingSpinner } from '@/components/ui/LoadingSpinner';
import { portfolios, positions, transactions } from '@/data/mockData';
import {
  buildPositionReportRows,
  formatCurrency,
  formatGainLoss,
  gainLossColor,
} from './reportUtils';

interface BarSegment {
  label: string;
  count: number;
  amount?: number;
  color: string;
}

function BarBreakdown({ segments, total }: { segments: BarSegment[]; total: number }) {
  return (
    <div className="space-y-3">
      <div className="flex h-3 w-full overflow-hidden rounded-full bg-slate-100">
        {segments.map(
          (s) =>
            s.count > 0 && (
              <div key={s.label} className={s.color} style={{ width: `${(s.count / total) * 100}%` }} />
            ),
        )}
      </div>
      <div className="space-y-2">
        {segments.map((s) => (
          <div key={s.label} className="flex items-center justify-between text-sm">
            <div className="flex items-center gap-2">
              <span className={`inline-block h-2.5 w-2.5 rounded-full ${s.color}`} />
              <span className="text-slate-700">{s.label}</span>
            </div>
            <div className="flex items-center gap-4">
              {s.amount !== undefined && <span className="text-slate-500">{formatCurrency(s.amount)}</span>}
              <span className="font-medium text-slate-900 w-8 text-right">{s.count}</span>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

export function StatisticsReportPage() {
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const timer = setTimeout(() => setLoading(false), 400);
    return () => clearTimeout(timer);
  }, []);

  const stats = useMemo(() => {
    const portfolioByStatus: BarSegment[] = [
      { label: 'Active', count: portfolios.filter((p) => p.status === 'A').length, color: 'bg-emerald-500' },
      { label: 'Inactive', count: portfolios.filter((p) => p.status === 'I').length, color: 'bg-amber-500' },
      { label: 'Closed', count: portfolios.filter((p) => p.status === 'C').length, color: 'bg-red-500' },
    ];

    const txnByType: BarSegment[] = [
      {
        label: 'Buy',
        count: transactions.filter((t) => t.transType === 'BY').length,
        amount: transactions.filter((t) => t.transType === 'BY').reduce((sum, t) => sum + t.amount, 0),
        color: 'bg-blue-500',
      },
      {
        label: 'Sell',
        count: transactions.filter((t) => t.transType === 'SL').length,
        amount: transactions.filter((t) => t.transType === 'SL').reduce((sum, t) => sum + t.amount, 0),
        color: 'bg-violet-500',
      },
      {
        label: 'Fee',
        count: transactions.filter((t) => t.transType === 'FE').length,
        amount: transactions.filter((t) => t.transType === 'FE').reduce((sum, t) => sum + t.amount, 0),
        color: 'bg-slate-400',
      },
    ];

    const txnByStatus: BarSegment[] = [
      { label: 'Completed', count: transactions.filter((t) => t.status === 'C').length, color: 'bg-emerald-500' },
      { label: 'Pending', count: transactions.filter((t) => t.status === 'P').length, color: 'bg-amber-500' },
      { label: 'Error', count: transactions.filter((t) => t.status === 'E').length, color: 'bg-red-500' },
    ];

    const positionRows = buildPositionReportRows();
    const totalCostBasis = positionRows.reduce((sum, r) => sum + r.costBasis, 0);
    const totalMarketValue = positionRows.reduce((sum, r) => sum + r.marketValue, 0);
    const totalGainLoss = totalMarketValue - totalCostBasis;
    const totalVolume = transactions.reduce((sum, t) => sum + t.amount, 0);

    return {
      portfolioByStatus,
      txnByType,
      txnByStatus,
      totalCostBasis,
      totalMarketValue,
      totalGainLoss,
      totalVolume,
      activePositions: positions.filter((p) => p.status === 'A').length,
      totalPositions: positions.length,
    };
  }, []);

  if (loading) {
    return (
      <div>
        <PageHeader title="Statistics Report" description="Portfolio and transaction processing statistics" />
        <LoadingSpinner message="Generating statistics report..." />
      </div>
    );
  }

  const summaryCards = [
    {
      label: 'Total Portfolios',
      value: String(portfolios.length),
      sub: `${stats.portfolioByStatus[0].count} active`,
      icon: <Briefcase className="w-6 h-6" />,
      color: 'text-blue-600 bg-blue-50',
    },
    {
      label: 'Total Transactions',
      value: String(transactions.length),
      sub: `${formatCurrency(stats.totalVolume)} total volume`,
      icon: <ArrowLeftRight className="w-6 h-6" />,
      color: 'text-violet-600 bg-violet-50',
    },
    {
      label: 'Total Market Value',
      value: formatCurrency(stats.totalMarketValue),
      sub: `${stats.activePositions} active positions`,
      icon: <DollarSign className="w-6 h-6" />,
      color: 'text-emerald-600 bg-emerald-50',
    },
    {
      label: 'Unrealized Gain/Loss',
      value: formatGainLoss(stats.totalGainLoss),
      sub: `vs ${formatCurrency(stats.totalCostBasis)} cost basis`,
      icon: <TrendingUp className="w-6 h-6" />,
      color: 'text-amber-600 bg-amber-50',
      valueClass: gainLossColor(stats.totalGainLoss),
    },
  ];

  return (
    <div>
      <PageHeader title="Statistics Report" description="Portfolio and transaction processing statistics" />
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        {summaryCards.map((card) => (
          <Card key={card.label}>
            <div className="flex items-start gap-4">
              <div className={`rounded-lg p-2.5 ${card.color}`}>{card.icon}</div>
              <div className="min-w-0">
                <p className="text-sm text-slate-500">{card.label}</p>
                <p className={`text-xl font-bold ${card.valueClass ?? 'text-slate-900'}`}>{card.value}</p>
                <p className="text-xs text-slate-400 mt-0.5">{card.sub}</p>
              </div>
            </div>
          </Card>
        ))}
      </div>
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <Card title="Portfolios by Status">
          <BarBreakdown segments={stats.portfolioByStatus} total={portfolios.length} />
        </Card>
        <Card title="Transaction Volume by Type">
          <BarBreakdown segments={stats.txnByType} total={transactions.length} />
        </Card>
        <Card title="Transactions by Status">
          <BarBreakdown segments={stats.txnByStatus} total={transactions.length} />
        </Card>
      </div>
    </div>
  );
}
