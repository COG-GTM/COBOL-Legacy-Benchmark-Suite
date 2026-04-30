import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts';
import { Briefcase, TrendingUp, ArrowLeftRight, DollarSign } from 'lucide-react';
import { fetchStatistics, fetchPortfolios, fetchPositionReport } from '../services/api';
import { formatCurrency, formatNumber } from '../utils/format';
import StatCard from '../components/StatCard';
import GainLoss from '../components/GainLoss';
import Loading from '../components/Loading';
import ErrorMessage from '../components/ErrorMessage';

const COLORS = ['#3b82f6', '#22c55e', '#f59e0b', '#ef4444', '#a855f7', '#06b6d4', '#ec4899', '#f97316'];

export default function Dashboard() {
  const stats = useQuery({ queryKey: ['statistics'], queryFn: fetchStatistics });
  const portfolios = useQuery({ queryKey: ['portfolios'], queryFn: () => fetchPortfolios('A') });
  const report = useQuery({ queryKey: ['positionReport'], queryFn: fetchPositionReport });

  if (stats.isLoading) return <Loading text="Loading dashboard..." />;
  if (stats.error) return <ErrorMessage message={(stats.error as Error).message} />;

  const s = stats.data!;

  const portfolioChartData = (portfolios.data?.portfolios ?? []).map(p => ({
    name: p.portfolio_name || p.portfolio_id,
    value: p.total_value,
  }));

  const holdingsData = (report.data?.items ?? []).reduce<Record<string, number>>((acc, item) => {
    acc[item.symbol] = (acc[item.symbol] ?? 0) + item.market_value;
    return acc;
  }, {});
  const pieData = Object.entries(holdingsData)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 8)
    .map(([name, value]) => ({ name, value: Math.round(value) }));

  return (
    <div className="space-y-8 animate-fade-in">
      <div>
        <h1 className="text-3xl font-bold">Dashboard</h1>
        <p className="text-slate-400 mt-1">Investment Portfolio Management System — modernized from COBOL/CICS</p>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="Total Market Value"
          value={formatCurrency(s.total_market_value)}
          icon={<DollarSign size={24} />}
          trend={<GainLoss value={s.total_gain_loss} size="sm" />}
        />
        <StatCard
          title="Active Portfolios"
          value={s.active_portfolios}
          subtitle={`${s.total_portfolios} total`}
          icon={<Briefcase size={24} />}
        />
        <StatCard
          title="Total Positions"
          value={s.total_positions}
          subtitle={`Avg: ${formatCurrency(s.avg_portfolio_value)}/portfolio`}
          icon={<TrendingUp size={24} />}
        />
        <StatCard
          title="Transactions"
          value={s.total_transactions}
          subtitle={`${s.transactions_today} today`}
          icon={<ArrowLeftRight size={24} />}
        />
      </div>

      {/* Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="card">
          <h2 className="text-lg font-semibold mb-4">Portfolio Values</h2>
          {portfolioChartData.length > 0 ? (
            <ResponsiveContainer width="100%" height={280}>
              <BarChart data={portfolioChartData}>
                <XAxis dataKey="name" tick={{ fill: '#94a3b8', fontSize: 12 }} />
                <YAxis tick={{ fill: '#94a3b8', fontSize: 12 }} tickFormatter={v => `$${(v / 1000).toFixed(0)}k`} />
                <Tooltip
                  contentStyle={{ background: '#1e293b', border: '1px solid #334155', borderRadius: 8 }}
                  labelStyle={{ color: '#f8fafc' }}
                  formatter={(v: number) => [formatCurrency(v), 'Value']}
                />
                <Bar dataKey="value" fill="#3b82f6" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          ) : (
            <p className="text-slate-500 text-center py-10">No portfolio data</p>
          )}
        </div>

        <div className="card">
          <h2 className="text-lg font-semibold mb-4">Holdings Breakdown</h2>
          {pieData.length > 0 ? (
            <ResponsiveContainer width="100%" height={280}>
              <PieChart>
                <Pie data={pieData} cx="50%" cy="50%" outerRadius={100} dataKey="value" label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}>
                  {pieData.map((_, i) => (
                    <Cell key={i} fill={COLORS[i % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip
                  contentStyle={{ background: '#1e293b', border: '1px solid #334155', borderRadius: 8 }}
                  formatter={(v: number) => [formatCurrency(v), 'Value']}
                />
              </PieChart>
            </ResponsiveContainer>
          ) : (
            <p className="text-slate-500 text-center py-10">No holdings data</p>
          )}
        </div>
      </div>

      {/* Quick Links */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Link to="/portfolios" className="card-hover group">
          <h3 className="font-semibold group-hover:text-blue-400 transition-colors">Portfolio Inquiry</h3>
          <p className="text-sm text-slate-400 mt-1">View positions and performance details</p>
        </Link>
        <Link to="/transactions" className="card-hover group">
          <h3 className="font-semibold group-hover:text-blue-400 transition-colors">Transaction History</h3>
          <p className="text-sm text-slate-400 mt-1">Browse and submit transactions</p>
        </Link>
        <Link to="/reports" className="card-hover group">
          <h3 className="font-semibold group-hover:text-blue-400 transition-colors">Reports</h3>
          <p className="text-sm text-slate-400 mt-1">Position, audit, and statistics reports</p>
        </Link>
      </div>

      {/* System Status */}
      <div className="card">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="font-semibold">System Health</h3>
            <p className="text-sm text-slate-400">All systems operational</p>
          </div>
          <span className="badge-success">{s.system_health}</span>
        </div>
      </div>
    </div>
  );
}
