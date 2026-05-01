import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getPositionReport, getAuditReport, getStatistics } from '../lib/api';
import StatusBadge, { GainLossDisplay } from '../components/StatusBadge';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts';

const COLORS = ['#4f46e5', '#06b6d4', '#10b981', '#f59e0b', '#ef4444'];

export default function Reports() {
  const [activeReport, setActiveReport] = useState<'positions' | 'audit' | 'statistics'>('statistics');

  const { data: statsData } = useQuery({
    queryKey: ['reports', 'statistics'],
    queryFn: getStatistics,
    enabled: activeReport === 'statistics',
  });

  const { data: positionsData } = useQuery({
    queryKey: ['reports', 'positions'],
    queryFn: () => getPositionReport(),
    enabled: activeReport === 'positions',
  });

  const { data: auditData } = useQuery({
    queryKey: ['reports', 'audit'],
    queryFn: () => getAuditReport(),
    enabled: activeReport === 'audit',
  });

  const stats = statsData?.data;

  const portfolioChartData = stats
    ? [
        { name: 'Active', value: stats.portfolios.active },
        { name: 'Closed', value: stats.portfolios.closed },
        { name: 'Suspended', value: stats.portfolios.suspended },
      ]
    : [];

  const txnChartData = stats
    ? [
        { name: 'Done', value: stats.transactions.done },
        { name: 'Pending', value: stats.transactions.pending },
        { name: 'Failed', value: stats.transactions.failed },
      ]
    : [];

  return (
    <div>
      <h1 className="text-2xl font-bold mb-6">Reports</h1>

      {/* Report Tabs */}
      <div className="border-b mb-6">
        <nav className="flex gap-4">
          {(['statistics', 'positions', 'audit'] as const).map((tab) => (
            <button
              key={tab}
              onClick={() => setActiveReport(tab)}
              className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors ${
                activeReport === tab
                  ? 'border-indigo-600 text-indigo-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
            >
              {tab.charAt(0).toUpperCase() + tab.slice(1)} Report
            </button>
          ))}
        </nav>
      </div>

      {/* Statistics Report — RPTSTA00 */}
      {activeReport === 'statistics' && stats && (
        <div>
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
            <div className="bg-white rounded-xl shadow-sm border p-6">
              <h3 className="text-lg font-semibold mb-4">Portfolio Distribution</h3>
              <ResponsiveContainer width="100%" height={250}>
                <PieChart>
                  <Pie data={portfolioChartData} cx="50%" cy="50%" innerRadius={60} outerRadius={100} dataKey="value" label={({ name, value }) => `${name}: ${value}`}>
                    {portfolioChartData.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                  </Pie>
                  <Tooltip />
                </PieChart>
              </ResponsiveContainer>
            </div>

            <div className="bg-white rounded-xl shadow-sm border p-6">
              <h3 className="text-lg font-semibold mb-4">Transaction Status</h3>
              <ResponsiveContainer width="100%" height={250}>
                <BarChart data={txnChartData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="name" />
                  <YAxis />
                  <Tooltip />
                  <Bar dataKey="value" fill="#4f46e5" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>

          <div className="bg-white rounded-xl shadow-sm border p-6">
            <h3 className="text-lg font-semibold mb-4">System Summary</h3>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              <MetricBox label="Total Portfolios" value={stats.portfolios.total} />
              <MetricBox label="Total Transactions" value={stats.transactions.total} />
              <MetricBox label="Total Positions" value={stats.positions.total} />
              <MetricBox label="Batch Success Rate" value={`${stats.batch.successRate.toFixed(0)}%`} />
            </div>
          </div>
        </div>
      )}

      {/* Positions Report — RPTPOS00 */}
      {activeReport === 'positions' && (
        <div className="bg-white rounded-xl shadow-sm border overflow-hidden">
          {positionsData?.data?.summary && (
            <div className="px-6 py-4 border-b bg-gray-50">
              <div className="flex gap-6 text-sm">
                <span>Total Positions: <strong>{(positionsData.data.summary as Record<string, number>).totalPositions}</strong></span>
                <span>Market Value: <strong>${((positionsData.data.summary as Record<string, number>).totalMarketValue ?? 0).toLocaleString()}</strong></span>
                <span>Cost Basis: <strong>${((positionsData.data.summary as Record<string, number>).totalCostBasis ?? 0).toLocaleString()}</strong></span>
                <span>
                  Gain/Loss: <GainLossDisplay value={(positionsData.data.summary as Record<string, number>).totalGainLoss ?? 0} />
                </span>
              </div>
            </div>
          )}
          <table className="w-full">
            <thead className="bg-gray-50 border-b">
              <tr>
                <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">Portfolio</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">Investment</th>
                <th className="text-right px-4 py-3 text-xs font-medium text-gray-500 uppercase">Quantity</th>
                <th className="text-right px-4 py-3 text-xs font-medium text-gray-500 uppercase">Cost Basis</th>
                <th className="text-right px-4 py-3 text-xs font-medium text-gray-500 uppercase">Market Value</th>
                <th className="text-right px-4 py-3 text-xs font-medium text-gray-500 uppercase">Gain/Loss</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {(positionsData?.data?.positions ?? []).map((pos, i) => {
                const gl = Number(pos.marketValue) - Number(pos.costBasis);
                return (
                  <tr key={i} className="hover:bg-gray-50">
                    <td className="px-4 py-3">{pos.portfolioId}</td>
                    <td className="px-4 py-3">{pos.investmentId.trim()}</td>
                    <td className="px-4 py-3 text-right">{Number(pos.quantity).toLocaleString()}</td>
                    <td className="px-4 py-3 text-right">${Number(pos.costBasis).toLocaleString()}</td>
                    <td className="px-4 py-3 text-right font-medium">${Number(pos.marketValue).toLocaleString()}</td>
                    <td className="px-4 py-3 text-right"><GainLossDisplay value={gl} /></td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {/* Audit Report — RPTAUD00 */}
      {activeReport === 'audit' && (
        <div className="bg-white rounded-xl shadow-sm border overflow-hidden">
          {auditData?.data?.summary && (
            <div className="px-6 py-4 border-b bg-gray-50">
              <div className="flex gap-6 text-sm">
                <span>Total Entries: <strong>{(auditData.data.summary as Record<string, number>).totalEntries}</strong></span>
              </div>
            </div>
          )}
          <table className="w-full">
            <thead className="bg-gray-50 border-b">
              <tr>
                <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">Date</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">Portfolio</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">Type</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">Action</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">User</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {(auditData?.data?.auditLogs ?? []).slice(0, 50).map((log) => (
                <tr key={log.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 text-sm">{new Date(log.processDate).toLocaleString()}</td>
                  <td className="px-4 py-3">{log.portfolioId}</td>
                  <td className="px-4 py-3">
                    <StatusBadge status={log.recordType === 'PT' ? 'A' : log.recordType === 'TR' ? 'P' : 'W'} />
                  </td>
                  <td className="px-4 py-3">{actionLabel(log.actionCode)}</td>
                  <td className="px-4 py-3">{log.processUser}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

function MetricBox({ label, value }: { label: string; value: number | string }) {
  return (
    <div className="p-4 bg-gray-50 rounded-lg">
      <p className="text-sm text-gray-500">{label}</p>
      <p className="text-2xl font-bold mt-1">{typeof value === 'number' ? value.toLocaleString() : value}</p>
    </div>
  );
}

function actionLabel(ac: string) {
  const m: Record<string, string> = { A: 'Added', C: 'Changed', D: 'Deleted' };
  return m[ac] || ac;
}
