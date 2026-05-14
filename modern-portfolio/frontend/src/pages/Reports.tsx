// Reports Dashboard (replaces RPTPOS00, RPTAUD00, RPTSTA00)
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { PieChart, Pie, Cell, Tooltip, ResponsiveContainer, BarChart, Bar, XAxis, YAxis, CartesianGrid, Legend } from 'recharts';
import { api } from '../lib/api';
import DataTable from '../components/DataTable';

interface PositionReport {
  portfolioId: string;
  portfolioName: string;
  totalCostBasis: number;
  totalMarketValue: number;
  totalGainLoss: number;
  positions: { investmentId: string; quantity: number; costBasis: number; marketValue: number }[];
}

interface AuditEntry {
  id: string;
  recordType: string;
  action: string;
  message: string;
  userId: string;
  programId: string;
  createdAt: string;
  portfolio?: { portfolioId: string; clientName: string };
}

type Tab = 'positions' | 'audit' | 'statistics';

export default function Reports() {
  const [tab, setTab] = useState<Tab>('positions');

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-800">Reports Dashboard</h1>
        <p className="text-gray-500 mt-1">Position reports, audit trails, and statistics</p>
      </div>

      {/* Tab navigation */}
      <div className="flex space-x-1 bg-gray-100 rounded-lg p-1 w-fit">
        {(['positions', 'audit', 'statistics'] as Tab[]).map((t) => (
          <button
            key={t}
            onClick={() => setTab(t)}
            className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${
              tab === t ? 'bg-white text-blue-700 shadow-sm' : 'text-gray-600 hover:text-gray-800'
            }`}
          >
            {t.charAt(0).toUpperCase() + t.slice(1)}
          </button>
        ))}
      </div>

      {tab === 'positions' && <PositionReportView />}
      {tab === 'audit' && <AuditReportView />}
      {tab === 'statistics' && <StatisticsView />}
    </div>
  );
}

function PositionReportView() {
  const { data, isLoading } = useQuery({
    queryKey: ['report-positions'],
    queryFn: () => api.getPositionReport() as Promise<{ data: PositionReport[] }>,
  });

  if (isLoading) return <div className="flex justify-center py-12"><div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600" /></div>;

  const reports = data?.data || [];
  const COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899'];

  const chartData = reports.map((r) => ({
    name: r.portfolioId,
    costBasis: r.totalCostBasis,
    marketValue: r.totalMarketValue,
  }));

  return (
    <div className="space-y-6">
      {chartData.length > 0 && (
        <div className="bg-white rounded-lg shadow-sm border p-5">
          <h2 className="text-lg font-semibold mb-4">Portfolio Values Comparison</h2>
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="name" />
              <YAxis tickFormatter={(v) => `$${(v / 1000).toFixed(0)}k`} />
              <Tooltip formatter={(value: number) => `$${value.toLocaleString()}`} />
              <Legend />
              <Bar dataKey="costBasis" name="Cost Basis" fill="#3b82f6" />
              <Bar dataKey="marketValue" name="Market Value" fill="#10b981" />
            </BarChart>
          </ResponsiveContainer>
        </div>
      )}

      {reports.map((report, idx) => (
        <div key={report.portfolioId} className="bg-white rounded-lg shadow-sm border p-5">
          <div className="flex justify-between items-center mb-3">
            <h3 className="font-semibold text-gray-800">{report.portfolioId} - {report.portfolioName}</h3>
            <div className="text-right">
              <span className={`text-lg font-bold ${report.totalGainLoss >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                {report.totalGainLoss >= 0 ? '+' : ''}${report.totalGainLoss.toLocaleString(undefined, { minimumFractionDigits: 2 })}
              </span>
            </div>
          </div>
          <div className="grid grid-cols-3 gap-4 mb-3 text-sm">
            <div><span className="text-gray-500">Cost Basis:</span> <span className="font-medium">${report.totalCostBasis.toLocaleString()}</span></div>
            <div><span className="text-gray-500">Market Value:</span> <span className="font-medium">${report.totalMarketValue.toLocaleString()}</span></div>
            <div>
              <span className="text-gray-500">G/L %:</span>{' '}
              <span className={`font-medium ${report.totalGainLoss >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                {report.totalCostBasis > 0 ? ((report.totalGainLoss / report.totalCostBasis) * 100).toFixed(2) : 0}%
              </span>
            </div>
          </div>
          {report.positions.length > 0 && (
            <div className="flex items-center gap-4">
              <div className="w-24 h-24">
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie data={report.positions.map((p) => ({ name: p.investmentId, value: Number(p.marketValue) }))} dataKey="value" cx="50%" cy="50%" innerRadius={20} outerRadius={40}>
                      {report.positions.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                    </Pie>
                  </PieChart>
                </ResponsiveContainer>
              </div>
              <div className="flex flex-wrap gap-2">
                {report.positions.map((p, i) => (
                  <span key={p.investmentId} className="text-xs px-2 py-1 rounded" style={{ backgroundColor: `${COLORS[i % COLORS.length]}20`, color: COLORS[i % COLORS.length] }}>
                    {p.investmentId}: ${Number(p.marketValue).toLocaleString()}
                  </span>
                ))}
              </div>
            </div>
          )}
        </div>
      ))}
    </div>
  );
}

function AuditReportView() {
  const [page, setPage] = useState(1);

  const { data, isLoading } = useQuery({
    queryKey: ['report-audit', page],
    queryFn: () => api.getAuditReport({ page: String(page), pageSize: '20' }) as Promise<{
      data: { entries: AuditEntry[]; total: number; page: number; totalPages: number };
    }>,
  });

  const actionColor = (a: string) => {
    const colors: Record<string, string> = { ADD: 'bg-green-100 text-green-800', CHANGE: 'bg-blue-100 text-blue-800', DELETE: 'bg-red-100 text-red-800' };
    return colors[a] || 'bg-gray-100 text-gray-800';
  };

  const columns = [
    { key: 'createdAt', header: 'Timestamp', render: (row: Record<string, unknown>) => new Date(String(row.createdAt)).toLocaleString() },
    { key: 'recordType', header: 'Record Type' },
    {
      key: 'action',
      header: 'Action',
      render: (row: Record<string, unknown>) => <span className={`px-2 py-1 rounded-full text-xs font-medium ${actionColor(String(row.action))}`}>{String(row.action)}</span>,
    },
    { key: 'message', header: 'Message' },
    { key: 'userId', header: 'User' },
    { key: 'programId', header: 'Program' },
  ];

  return (
    <div className="bg-white rounded-lg shadow-sm border">
      <div className="p-4 border-b">
        <h2 className="text-lg font-semibold">Audit Trail</h2>
      </div>
      <DataTable
        columns={columns}
        data={(data?.data?.entries || []) as unknown as Record<string, unknown>[]}
        page={data?.data?.page}
        totalPages={data?.data?.totalPages}
        onPageChange={setPage}
        loading={isLoading}
      />
    </div>
  );
}

function StatisticsView() {
  const { data, isLoading } = useQuery({
    queryKey: ['report-statistics'],
    queryFn: () => api.getStatistics() as Promise<{
      data: {
        totalPortfolios: number;
        activePortfolios: number;
        totalPositions: number;
        totalTransactions: number;
        pendingTransactions: number;
        totalValue: number;
        recentActivity: { date: string; count: number }[];
      };
    }>,
  });

  if (isLoading) return <div className="flex justify-center py-12"><div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600" /></div>;

  const stats = data?.data;
  if (!stats) return null;

  const metrics = [
    { label: 'Total Portfolios', value: stats.totalPortfolios },
    { label: 'Active Portfolios', value: stats.activePortfolios },
    { label: 'Suspended/Closed', value: stats.totalPortfolios - stats.activePortfolios },
    { label: 'Total Positions', value: stats.totalPositions },
    { label: 'Total Transactions', value: stats.totalTransactions },
    { label: 'Pending Transactions', value: stats.pendingTransactions },
    { label: 'Total AUM', value: `$${stats.totalValue.toLocaleString()}` },
  ];

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {metrics.map((m) => (
          <div key={m.label} className="bg-white rounded-lg shadow-sm border p-4">
            <p className="text-sm text-gray-500">{m.label}</p>
            <p className="text-xl font-bold text-gray-800 mt-1">{m.value}</p>
          </div>
        ))}
      </div>
      <div className="bg-white rounded-lg shadow-sm border p-5">
        <h2 className="text-lg font-semibold mb-4">Transaction Activity (7 Days)</h2>
        <ResponsiveContainer width="100%" height={250}>
          <BarChart data={stats.recentActivity}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="date" />
            <YAxis allowDecimals={false} />
            <Tooltip />
            <Bar dataKey="count" fill="#3b82f6" radius={[4, 4, 0, 0]} />
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
