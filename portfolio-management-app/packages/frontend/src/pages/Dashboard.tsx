import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { Briefcase, ArrowLeftRight, TrendingUp, AlertCircle, Activity } from 'lucide-react';
import { getStatistics, getSystemHealth } from '../lib/api';
import StatusBadge from '../components/StatusBadge';

export default function Dashboard() {
  const { data: stats } = useQuery({
    queryKey: ['statistics'],
    queryFn: getStatistics,
    refetchInterval: 30000,
  });

  const { data: health } = useQuery({
    queryKey: ['health'],
    queryFn: getSystemHealth,
    refetchInterval: 15000,
  });

  const metrics = health?.data?.metrics;
  const report = stats?.data;

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
          <p className="text-gray-500">Portfolio Management System Overview</p>
        </div>
        {health?.data && (
          <div className="flex items-center gap-2">
            <Activity size={16} className={health.data.status === 'healthy' ? 'text-green-500' : 'text-red-500'} />
            <StatusBadge status={health.data.status === 'healthy' ? 'A' : 'E'} />
          </div>
        )}
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        <StatCard
          icon={<Briefcase className="text-indigo-600" />}
          label="Total Portfolios"
          value={metrics?.totalPortfolios ?? 0}
          subLabel={`${metrics?.activePortfolios ?? 0} active`}
        />
        <StatCard
          icon={<ArrowLeftRight className="text-blue-600" />}
          label="Total Transactions"
          value={metrics?.totalTransactions ?? 0}
          subLabel={`${metrics?.pendingTransactions ?? 0} pending`}
          highlight={metrics?.pendingTransactions ? metrics.pendingTransactions > 0 : false}
        />
        <StatCard
          icon={<TrendingUp className="text-green-600" />}
          label="Batch Jobs Today"
          value={metrics?.batchJobsToday ?? 0}
          subLabel={metrics?.lastBatchRun ? `Last: ${new Date(metrics.lastBatchRun).toLocaleTimeString()}` : 'No runs today'}
        />
        <StatCard
          icon={<AlertCircle className="text-yellow-600" />}
          label="System Status"
          value={health?.data?.uptime ? formatUptime(health.data.uptime) : '--'}
          subLabel={`DB: ${health?.data?.database ?? 'unknown'}`}
        />
      </div>

      {/* Portfolio Breakdown & Recent Batch */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {report && (
          <div className="bg-white rounded-xl shadow-sm border p-6">
            <h2 className="text-lg font-semibold mb-4">Portfolio Breakdown</h2>
            <div className="space-y-3">
              <BreakdownRow label="Active" value={report.portfolios.active} total={report.portfolios.total} color="bg-green-500" />
              <BreakdownRow label="Closed" value={report.portfolios.closed} total={report.portfolios.total} color="bg-gray-400" />
              <BreakdownRow label="Suspended" value={report.portfolios.suspended} total={report.portfolios.total} color="bg-yellow-500" />
            </div>
            <div className="mt-4 pt-4 border-t">
              <div className="flex justify-between text-sm">
                <span>Transaction Success Rate</span>
                <span className="font-medium">
                  {report.transactions.total > 0
                    ? ((report.transactions.done / report.transactions.total) * 100).toFixed(1)
                    : '100'}%
                </span>
              </div>
            </div>
          </div>
        )}

        <div className="bg-white rounded-xl shadow-sm border p-6">
          <h2 className="text-lg font-semibold mb-4">Quick Actions</h2>
          <div className="grid grid-cols-2 gap-3">
            <Link to="/portfolios" className="p-4 border rounded-lg hover:bg-indigo-50 transition-colors text-center">
              <Briefcase size={24} className="mx-auto text-indigo-600 mb-2" />
              <span className="text-sm font-medium">View Portfolios</span>
            </Link>
            <Link to="/transactions/new" className="p-4 border rounded-lg hover:bg-green-50 transition-colors text-center">
              <ArrowLeftRight size={24} className="mx-auto text-green-600 mb-2" />
              <span className="text-sm font-medium">New Transaction</span>
            </Link>
            <Link to="/batch" className="p-4 border rounded-lg hover:bg-blue-50 transition-colors text-center">
              <Activity size={24} className="mx-auto text-blue-600 mb-2" />
              <span className="text-sm font-medium">Run Batch</span>
            </Link>
            <Link to="/reports" className="p-4 border rounded-lg hover:bg-purple-50 transition-colors text-center">
              <TrendingUp size={24} className="mx-auto text-purple-600 mb-2" />
              <span className="text-sm font-medium">Reports</span>
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}

function StatCard({ icon, label, value, subLabel, highlight }: {
  icon: React.ReactNode;
  label: string;
  value: number | string;
  subLabel: string;
  highlight?: boolean;
}) {
  return (
    <div className={`bg-white rounded-xl shadow-sm border p-6 ${highlight ? 'ring-2 ring-yellow-300' : ''}`}>
      <div className="flex items-center gap-3 mb-3">
        {icon}
        <span className="text-sm text-gray-500">{label}</span>
      </div>
      <p className="text-3xl font-bold text-gray-900">{typeof value === 'number' ? value.toLocaleString() : value}</p>
      <p className="text-sm text-gray-500 mt-1">{subLabel}</p>
    </div>
  );
}

function BreakdownRow({ label, value, total, color }: {
  label: string; value: number; total: number; color: string;
}) {
  const pct = total > 0 ? (value / total) * 100 : 0;
  return (
    <div>
      <div className="flex justify-between text-sm mb-1">
        <span>{label}</span>
        <span className="font-medium">{value}</span>
      </div>
      <div className="w-full bg-gray-100 rounded-full h-2">
        <div className={`${color} h-2 rounded-full`} style={{ width: `${pct}%` }} />
      </div>
    </div>
  );
}

function formatUptime(seconds: number): string {
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  if (h > 0) return `${h}h ${m}m`;
  return `${m}m`;
}
