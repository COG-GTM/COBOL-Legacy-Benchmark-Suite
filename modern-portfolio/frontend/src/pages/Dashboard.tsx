// Dashboard/Main Menu (replaces MENMAP from INQSET.bms lines 7-19)
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { api } from '../lib/api';

interface Statistics {
  totalPortfolios: number;
  activePortfolios: number;
  totalPositions: number;
  totalTransactions: number;
  pendingTransactions: number;
  totalValue: number;
  recentActivity: { date: string; count: number }[];
}

export default function Dashboard() {
  const navigate = useNavigate();
  const { data, isLoading } = useQuery({
    queryKey: ['statistics'],
    queryFn: () => api.getStatistics() as Promise<{ data: Statistics }>,
  });

  const stats = data?.data;

  const cards = [
    { label: 'Total Portfolios', value: stats?.totalPortfolios ?? '-', color: 'bg-blue-500', onClick: () => navigate('/manage') },
    { label: 'Active Portfolios', value: stats?.activePortfolios ?? '-', color: 'bg-green-500', onClick: () => navigate('/inquiry') },
    { label: 'Total Positions', value: stats?.totalPositions ?? '-', color: 'bg-purple-500', onClick: () => navigate('/inquiry') },
    { label: 'Total Transactions', value: stats?.totalTransactions ?? '-', color: 'bg-amber-500', onClick: () => navigate('/transactions') },
    { label: 'Pending Transactions', value: stats?.pendingTransactions ?? '-', color: 'bg-red-500', onClick: () => navigate('/admin') },
    { label: 'Total Value', value: stats ? `$${stats.totalValue.toLocaleString()}` : '-', color: 'bg-teal-500', onClick: () => navigate('/reports') },
  ];

  const menuItems = [
    { num: '1', label: 'Portfolio Position Inquiry', desc: 'View portfolio positions and market values', path: '/inquiry' },
    { num: '2', label: 'Transaction History', desc: 'Browse and search transaction records', path: '/transactions' },
    { num: '3', label: 'Portfolio Management', desc: 'Create, update, and manage portfolios', path: '/manage' },
    { num: '4', label: 'Reports Dashboard', desc: 'Position reports, audit trails, and statistics', path: '/reports' },
    { num: '5', label: 'Admin Panel', desc: 'System monitoring and job management', path: '/admin' },
  ];

  if (isLoading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-800">Dashboard</h1>
        <p className="text-gray-500 mt-1">Investment Portfolio Management System</p>
      </div>

      {/* Summary cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {cards.map((card) => (
          <button
            key={card.label}
            onClick={card.onClick}
            className="bg-white rounded-lg shadow-sm border p-5 text-left hover:shadow-md transition-shadow"
          >
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-500">{card.label}</p>
                <p className="text-2xl font-bold text-gray-800 mt-1">{card.value}</p>
              </div>
              <div className={`${card.color} w-12 h-12 rounded-lg flex items-center justify-center text-white text-xl`}>
                {card.label[0]}
              </div>
            </div>
          </button>
        ))}
      </div>

      {/* Recent Activity Chart */}
      {stats?.recentActivity && (
        <div className="bg-white rounded-lg shadow-sm border p-5">
          <h2 className="text-lg font-semibold text-gray-800 mb-4">Recent Activity (Last 7 Days)</h2>
          <ResponsiveContainer width="100%" height={250}>
            <BarChart data={stats.recentActivity}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="date" tick={{ fontSize: 12 }} />
              <YAxis allowDecimals={false} />
              <Tooltip />
              <Bar dataKey="count" fill="#3b82f6" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      )}

      {/* Navigation Menu (replaces CICS MENMAP) */}
      <div className="bg-white rounded-lg shadow-sm border p-5">
        <h2 className="text-lg font-semibold text-gray-800 mb-4">Select Option</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
          {menuItems.map((item) => (
            <button
              key={item.num}
              onClick={() => navigate(item.path)}
              className="flex items-center p-4 border rounded-lg hover:bg-blue-50 hover:border-blue-300 transition-colors text-left"
            >
              <span className="flex-shrink-0 w-8 h-8 bg-blue-100 text-blue-700 rounded-full flex items-center justify-center font-bold text-sm mr-3">
                {item.num}
              </span>
              <div>
                <p className="font-medium text-gray-800">{item.label}</p>
                <p className="text-xs text-gray-500 mt-0.5">{item.desc}</p>
              </div>
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}
