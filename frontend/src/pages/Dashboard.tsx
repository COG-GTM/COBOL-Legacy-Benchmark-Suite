import { Link } from 'react-router-dom';
import {
  Briefcase,
  TrendingUp,
  ArrowRightLeft,
  DollarSign,
  Search,
  History,
  PlusCircle,
  FileText,
} from 'lucide-react';
import { usePortfolio } from '../context/PortfolioContext';

export default function Dashboard() {
  const { portfolios, transactions, positions } = usePortfolio();

  const activePortfolios = portfolios.filter((p) => p.status === 'A').length;
  const totalAUM = portfolios.reduce((sum, p) => sum + p.totalValue, 0);
  const recentTransactions = transactions.slice(0, 5);
  const totalPositions = positions.length;

  const cards = [
    {
      label: 'Active Portfolios',
      value: activePortfolios,
      icon: Briefcase,
      color: 'bg-blue-500',
    },
    {
      label: 'Total AUM',
      value: `$${totalAUM.toLocaleString('en-US', { minimumFractionDigits: 2 })}`,
      icon: DollarSign,
      color: 'bg-green-500',
    },
    {
      label: 'Total Positions',
      value: totalPositions,
      icon: TrendingUp,
      color: 'bg-purple-500',
    },
    {
      label: 'Total Transactions',
      value: transactions.length,
      icon: ArrowRightLeft,
      color: 'bg-orange-500',
    },
  ];

  const quickLinks = [
    { to: '/positions', label: 'Position Inquiry', icon: Search },
    { to: '/history', label: 'Transaction History', icon: History },
    { to: '/transactions/new', label: 'New Transaction', icon: PlusCircle },
    { to: '/reports', label: 'View Reports', icon: FileText },
  ];

  const typeLabels: Record<string, string> = {
    BU: 'Buy',
    SL: 'Sell',
    TR: 'Transfer',
    FE: 'Fee',
  };

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-800 mb-6">Dashboard</h1>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        {cards.map(({ label, value, icon: Icon, color }) => (
          <div key={label} className="bg-white rounded-lg shadow-sm border p-5">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-500">{label}</p>
                <p className="text-2xl font-bold text-gray-800 mt-1">{value}</p>
              </div>
              <div className={`${color} p-3 rounded-lg`}>
                <Icon className="w-6 h-6 text-white" />
              </div>
            </div>
          </div>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-white rounded-lg shadow-sm border">
          <div className="p-4 border-b">
            <h2 className="text-lg font-semibold text-gray-800">Recent Transactions</h2>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-gray-50">
                <tr>
                  <th className="text-left px-4 py-2 text-gray-600">Date</th>
                  <th className="text-left px-4 py-2 text-gray-600">Portfolio</th>
                  <th className="text-left px-4 py-2 text-gray-600">Type</th>
                  <th className="text-right px-4 py-2 text-gray-600">Amount</th>
                </tr>
              </thead>
              <tbody>
                {recentTransactions.map((t, i) => (
                  <tr key={i} className="border-t">
                    <td className="px-4 py-2 text-gray-700">
                      {t.date.replace(/(\d{4})(\d{2})(\d{2})/, '$1-$2-$3')}
                    </td>
                    <td className="px-4 py-2 text-gray-700">{t.portfolioId}</td>
                    <td className="px-4 py-2">
                      <span
                        className={`px-2 py-0.5 rounded-full text-xs font-medium ${
                          t.type === 'BU'
                            ? 'bg-green-100 text-green-700'
                            : t.type === 'SL'
                              ? 'bg-red-100 text-red-700'
                              : t.type === 'TR'
                                ? 'bg-blue-100 text-blue-700'
                                : 'bg-gray-100 text-gray-700'
                        }`}
                      >
                        {typeLabels[t.type]}
                      </span>
                    </td>
                    <td className="px-4 py-2 text-right text-gray-700">
                      ${t.amount.toLocaleString('en-US', { minimumFractionDigits: 2 })}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        <div className="bg-white rounded-lg shadow-sm border">
          <div className="p-4 border-b">
            <h2 className="text-lg font-semibold text-gray-800">Quick Actions</h2>
          </div>
          <div className="p-4 grid grid-cols-2 gap-3">
            {quickLinks.map(({ to, label, icon: Icon }) => (
              <Link
                key={to}
                to={to}
                className="flex items-center gap-3 p-4 rounded-lg border border-gray-200 hover:bg-gray-50 hover:border-blue-300 transition-colors"
              >
                <Icon className="w-5 h-5 text-blue-600" />
                <span className="text-sm font-medium text-gray-700">{label}</span>
              </Link>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
