import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Briefcase,
  DollarSign,
  TrendingUp,
  ArrowLeftRight,
} from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { StatusBadge, getTransactionStatusVariant, getTransactionStatusLabel, getTransTypeLabel } from '@/components/ui/StatusBadge';
import { PageHeader } from '@/components/ui/PageHeader';
import { LoadingSpinner } from '@/components/ui/LoadingSpinner';
import { EmptyState } from '@/components/ui/EmptyState';
import { usePortfolios } from '@/context/PortfolioContext';
import { positions, transactions, fundPrices } from '@/data/mockData';

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', minimumFractionDigits: 0, maximumFractionDigits: 0 }).format(value);
}

function formatAmount(value: number): string {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value);
}

function formatGrowthPct(value: number): string {
  const formatted = new Intl.NumberFormat('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value);
  return value >= 0 ? `+${formatted}%` : `${formatted}%`;
}

interface AccountGrowth {
  accountNo: string;
  marketValue: number;
  costBasis: number;
  gainLoss: number;
  growthPct: number;
}

export function DashboardPage() {
  const { portfolios } = usePortfolios();
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const timer = setTimeout(() => setLoading(false), 400);
    return () => clearTimeout(timer);
  }, []);

  const stats = useMemo(() => {
    const totalPortfolios = portfolios.length;
    const totalMarketValue = portfolios
      .filter((p) => p.status === 'A')
      .reduce((sum, p) => sum + p.totalValue, 0);
    const totalPositions = positions.length;
    const recentTransactionCount = transactions.length;
    return { totalPortfolios, totalMarketValue, totalPositions, recentTransactionCount };
  }, [portfolios]);

  const accountGrowth = useMemo<AccountGrowth[]>(() => {
    const byAccount = new Map<string, { marketValue: number; costBasis: number }>();
    for (const pos of positions) {
      if (pos.status !== 'A') continue;
      const price = fundPrices[pos.fundId] ?? pos.avgCost;
      const marketValue = pos.shareBalance * price;
      const acc = byAccount.get(pos.accountNo) ?? { marketValue: 0, costBasis: 0 };
      acc.marketValue += marketValue;
      acc.costBasis += pos.costBasis;
      byAccount.set(pos.accountNo, acc);
    }
    return [...byAccount.entries()]
      .filter(([, v]) => v.costBasis > 0)
      .map(([accountNo, v]) => ({
        accountNo,
        marketValue: v.marketValue,
        costBasis: v.costBasis,
        gainLoss: v.marketValue - v.costBasis,
        growthPct: ((v.marketValue - v.costBasis) / v.costBasis) * 100,
      }))
      .sort((a, b) => b.growthPct - a.growthPct);
  }, []);

  const recentTransactions = useMemo(
    () =>
      [...transactions]
        .sort((a, b) => b.transDate.localeCompare(a.transDate))
        .slice(0, 6),
    [],
  );

  const summaryCards = [
    { label: 'Total Portfolios', value: stats.totalPortfolios.toString(), icon: <Briefcase className="w-6 h-6" />, color: 'text-blue-600 bg-blue-50' },
    { label: 'Total Market Value', value: formatCurrency(stats.totalMarketValue), icon: <DollarSign className="w-6 h-6" />, color: 'text-emerald-600 bg-emerald-50' },
    { label: 'Total Positions', value: stats.totalPositions.toString(), icon: <TrendingUp className="w-6 h-6" />, color: 'text-violet-600 bg-violet-50' },
    { label: 'Recent Transactions', value: stats.recentTransactionCount.toString(), icon: <ArrowLeftRight className="w-6 h-6" />, color: 'text-amber-600 bg-amber-50' },
  ];

  if (loading) {
    return (
      <div>
        <PageHeader
          title="Dashboard"
          description="Overview of your investment portfolio management system"
        />
        <LoadingSpinner size="lg" message="Loading dashboard..." />
      </div>
    );
  }

  return (
    <div>
      <PageHeader
        title="Dashboard"
        description="Overview of your investment portfolio management system"
      />

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        {summaryCards.map((card) => (
          <div
            key={card.label}
            className="bg-white rounded-lg border border-slate-200 shadow-sm p-5"
          >
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-slate-500">{card.label}</p>
                <p className="text-2xl font-bold text-slate-900 mt-1">{card.value}</p>
              </div>
              <div className={`p-3 rounded-lg ${card.color}`}>{card.icon}</div>
            </div>
          </div>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card title="Account Growth">
          {accountGrowth.length === 0 ? (
            <EmptyState title="No account data" message="No active positions available to compute growth." />
          ) : (
            <div className="overflow-x-auto -m-6 mt-0">
              <table className="min-w-full divide-y divide-slate-200">
                <thead className="bg-slate-50">
                  <tr>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase">Rank</th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase">Account</th>
                    <th className="px-4 py-3 text-right text-xs font-semibold text-slate-600 uppercase">Market Value</th>
                    <th className="px-4 py-3 text-right text-xs font-semibold text-slate-600 uppercase">Cost Basis</th>
                    <th className="px-4 py-3 text-right text-xs font-semibold text-slate-600 uppercase">Gain/Loss</th>
                    <th className="px-4 py-3 text-right text-xs font-semibold text-slate-600 uppercase">Growth</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200">
                  {accountGrowth.map((acc, idx) => (
                    <tr key={acc.accountNo} className="hover:bg-slate-50 transition-colors">
                      <td className="px-4 py-3 text-sm text-slate-500">{idx + 1}</td>
                      <td className="px-4 py-3 text-sm font-mono text-slate-900">{acc.accountNo}</td>
                      <td className="px-4 py-3 text-sm text-slate-900 text-right font-medium">{formatAmount(acc.marketValue)}</td>
                      <td className="px-4 py-3 text-sm text-slate-600 text-right">{formatAmount(acc.costBasis)}</td>
                      <td className={`px-4 py-3 text-sm text-right font-medium ${acc.gainLoss >= 0 ? 'text-emerald-600' : 'text-red-600'}`}>
                        {formatAmount(acc.gainLoss)}
                      </td>
                      <td className={`px-4 py-3 text-sm text-right font-semibold ${acc.growthPct >= 0 ? 'text-emerald-600' : 'text-red-600'}`}>
                        {formatGrowthPct(acc.growthPct)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Card>

        <Card title="Recent Activity">
          {recentTransactions.length === 0 ? (
            <EmptyState title="No recent activity" message="There are no recent transactions to display." />
          ) : (
            <div className="divide-y divide-slate-200 -m-6 mt-0">
              {recentTransactions.map((txn) => (
                <Link
                  key={txn.transId}
                  to="/transactions"
                  className="flex items-center justify-between gap-3 px-4 py-3 hover:bg-slate-50 transition-colors group"
                >
                  <div className="min-w-0">
                    <p className="text-sm font-mono font-medium text-slate-900 group-hover:text-blue-600 transition-colors">
                      {txn.transId}
                    </p>
                    <p className="text-xs text-slate-500">
                      {getTransTypeLabel(txn.transType)} · {txn.fundId} · Acct {txn.accountNo} · {txn.transDate}
                    </p>
                  </div>
                  <div className="flex items-center gap-3 shrink-0">
                    <span className="text-sm font-medium text-slate-900">{formatAmount(txn.amount)}</span>
                    <StatusBadge
                      label={getTransactionStatusLabel(txn.status)}
                      variant={getTransactionStatusVariant(txn.status)}
                    />
                  </div>
                </Link>
              ))}
            </div>
          )}
        </Card>
      </div>
    </div>
  );
}
