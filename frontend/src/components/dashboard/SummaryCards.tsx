import { Users, TrendingUp, ArrowRightLeft, Activity } from 'lucide-react';
import { summaryMetrics } from '@/mock/dashboardData';
import { formatNumber } from '@/lib/utils';

const cards = [
  {
    label: 'Total Accounts',
    value: formatNumber(summaryMetrics.totalAccounts),
    trend: '+3.2% from yesterday',
    icon: Users,
    iconColor: 'text-blue-600',
    iconBg: 'bg-blue-50',
  },
  {
    label: 'Total Positions',
    value: formatNumber(summaryMetrics.totalPositions),
    trend: '+1.8% from yesterday',
    icon: TrendingUp,
    iconColor: 'text-emerald-600',
    iconBg: 'bg-emerald-50',
  },
  {
    label: "Today's Transactions",
    value: formatNumber(summaryMetrics.todayTransactions),
    trend: '+12 since last hour',
    icon: ArrowRightLeft,
    iconColor: 'text-indigo-600',
    iconBg: 'bg-indigo-50',
  },
  {
    label: 'System Status',
    value: 'Operational',
    trend: 'All systems nominal',
    icon: Activity,
    iconColor: 'text-green-600',
    iconBg: 'bg-green-50',
    statusDot: true,
  },
];

export default function SummaryCards() {
  return (
    <section>
      <h2 className="text-lg font-semibold text-gray-900 mb-4">Overview</h2>
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {cards.map((card) => (
          <div
            key={card.label}
            className="bg-white rounded-lg shadow-sm border border-gray-200 p-6"
          >
            <div className="flex items-center justify-between mb-3">
              <span className="text-sm font-medium text-gray-500">{card.label}</span>
              <div className={`h-8 w-8 rounded-lg ${card.iconBg} flex items-center justify-center`}>
                <card.icon className={`h-4 w-4 ${card.iconColor}`} />
              </div>
            </div>
            <div className="flex items-center gap-2">
              {card.statusDot && (
                <span className="h-2.5 w-2.5 rounded-full bg-green-500 animate-pulse" />
              )}
              <span className="text-2xl font-bold text-gray-900">{card.value}</span>
            </div>
            <p className="text-xs text-gray-500 mt-1">{card.trend}</p>
          </div>
        ))}
      </div>
    </section>
  );
}
