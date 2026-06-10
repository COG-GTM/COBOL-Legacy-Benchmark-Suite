import { useMemo } from 'react';
import { Link } from 'react-router-dom';
import {
  Briefcase,
  DollarSign,
  TrendingUp,
  Clock,
  ArrowLeftRight,
  FileText,
  Activity,
  AlertTriangle,
} from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { StatusBadge, getTransactionStatusVariant, getTransactionStatusLabel, getTransTypeLabel } from '@/components/ui/StatusBadge';
import { PageHeader } from '@/components/ui/PageHeader';
import { usePortfolios } from '@/context/PortfolioContext';
import { positions, transactions } from '@/data/mockData';

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', minimumFractionDigits: 0, maximumFractionDigits: 0 }).format(value);
}

function formatAmount(value: number): string {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value);
}

export function DashboardPage() {
  const { portfolios } = usePortfolios();

  const stats = useMemo(() => {
    const totalPortfolios = portfolios.filter((p) => p.status === 'A').length;
    const totalValue = portfolios.filter((p) => p.status === 'A').reduce((sum, p) => sum + p.totalValue, 0);
    const activePositions = positions.filter((p) => p.status === 'A').length;
    const pendingTransactions = transactions.filter((t) => t.status === 'P').length;
    return { totalPortfolios, totalValue, activePositions, pendingTransactions };
  }, [portfolios]);

  const recentTransactions = useMemo(
    () =>
      [...transactions]
        .sort((a, b) => b.transDate.localeCompare(a.transDate))
        .slice(0, 5),
    [],
  );

  const summaryCards = [
    { label: 'Total Portfolios', value: stats.totalPortfolios.toString(), icon: <Briefcase className="w-6 h-6" />, color: 'text-blue-600 bg-blue-50' },
    { label: 'Total Market Value', value: formatCurrency(stats.totalValue), icon: <DollarSign className="w-6 h-6" />, color: 'text-emerald-600 bg-emerald-50' },
    { label: 'Active Positions', value: stats.activePositions.toString(), icon: <TrendingUp className="w-6 h-6" />, color: 'text-violet-600 bg-violet-50' },
    { label: 'Pending Transactions', value: stats.pendingTransactions.toString(), icon: <Clock className="w-6 h-6" />, color: 'text-amber-600 bg-amber-50' },
  ];

  const quickLinks = [
    { label: 'Portfolios', description: 'Manage investment portfolios', icon: <Briefcase className="w-5 h-5" />, to: '/portfolios', color: 'text-blue-600 bg-blue-50' },
    { label: 'Transactions', description: 'View transaction history', icon: <ArrowLeftRight className="w-5 h-5" />, to: '/transactions', color: 'text-emerald-600 bg-emerald-50' },
    { label: 'Reports', description: 'Generate financial reports', icon: <FileText className="w-5 h-5" />, to: '/reports/positions', color: 'text-violet-600 bg-violet-50' },
    { label: 'Batch Monitor', description: 'Monitor batch job status', icon: <Activity className="w-5 h-5" />, to: '/batch', color: 'text-amber-600 bg-amber-50' },
    { label: 'Error Log', description: 'Review system errors', icon: <AlertTriangle className="w-5 h-5" />, to: '/errors', color: 'text-red-600 bg-red-50' },
  ];

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

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2">
          <Card title="Recent Transactions">
            <div className="overflow-x-auto -m-6 mt-0">
              <table className="min-w-full divide-y divide-slate-200">
                <thead className="bg-slate-50">
                  <tr>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase">Trans ID</th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase">Account</th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase">Type</th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase">Date</th>
                    <th className="px-4 py-3 text-right text-xs font-semibold text-slate-600 uppercase">Amount</th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200">
                  {recentTransactions.map((txn) => (
                    <tr key={txn.transId} className="hover:bg-slate-50 transition-colors">
                      <td className="px-4 py-3 text-sm font-mono text-slate-900">{txn.transId}</td>
                      <td className="px-4 py-3 text-sm text-slate-600">{txn.accountNo}</td>
                      <td className="px-4 py-3 text-sm text-slate-600">{getTransTypeLabel(txn.transType)}</td>
                      <td className="px-4 py-3 text-sm text-slate-600">{txn.transDate}</td>
                      <td className="px-4 py-3 text-sm text-slate-900 text-right font-medium">{formatAmount(txn.amount)}</td>
                      <td className="px-4 py-3">
                        <StatusBadge
                          label={getTransactionStatusLabel(txn.status)}
                          variant={getTransactionStatusVariant(txn.status)}
                        />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </Card>
        </div>

        <div>
          <Card title="Quick Navigation">
            <div className="space-y-3">
              {quickLinks.map((link) => (
                <Link
                  key={link.label}
                  to={link.to}
                  className="flex items-center gap-3 p-3 rounded-lg hover:bg-slate-50 transition-colors group"
                >
                  <div className={`p-2 rounded-lg ${link.color}`}>{link.icon}</div>
                  <div>
                    <p className="text-sm font-medium text-slate-900 group-hover:text-blue-600 transition-colors">
                      {link.label}
                    </p>
                    <p className="text-xs text-slate-500">{link.description}</p>
                  </div>
                </Link>
              ))}
            </div>
          </Card>
        </div>
      </div>
    </div>
  );
}
