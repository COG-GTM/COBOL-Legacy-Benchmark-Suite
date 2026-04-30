import { useState } from 'react';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  LineChart, Line, PieChart, Pie, Cell, Legend,
} from 'recharts';
import Card from '../components/Card';
import { portfolios, positions, auditRecords, batchJobs } from '../data/mockData';

const COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899'];

type ReportTab = 'position' | 'audit' | 'statistics';

export default function Reports() {
  const [tab, setTab] = useState<ReportTab>('position');

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-xl font-bold text-slate-900">Reports</h2>
        <p className="text-sm text-slate-500 mt-0.5">
          Modernized from RPTPOS00 / RPTAUD00 / RPTSTA00
        </p>
      </div>

      <div className="flex gap-1 bg-slate-100 rounded-lg p-1 w-fit">
        {([
          { key: 'position', label: 'Position Report' },
          { key: 'audit', label: 'Audit Report' },
          { key: 'statistics', label: 'System Statistics' },
        ] as const).map(({ key, label }) => (
          <button
            key={key}
            onClick={() => setTab(key)}
            className={`px-4 py-2 text-sm font-medium rounded-md transition-colors ${
              tab === key ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500 hover:text-slate-700'
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      {tab === 'position' && <PositionReport />}
      {tab === 'audit' && <AuditReport />}
      {tab === 'statistics' && <StatisticsReport />}
    </div>
  );
}

function PositionReport() {
  const activePortfolios = portfolios.filter(p => p.status === 'A');
  const portfolioSummary = activePortfolios.map(port => {
    const portPositions = positions.filter(p => p.portfolioId === port.id);
    const totalCost = portPositions.reduce((s, p) => s + p.costBasis, 0);
    const totalMarket = portPositions.reduce((s, p) => s + p.marketValue, 0);
    return {
      name: port.clientName.split(' ').slice(0, 2).join(' '),
      holdings: portPositions.length,
      costBasis: totalCost,
      marketValue: totalMarket,
      pnl: totalMarket - totalCost,
      pnlPct: totalCost > 0 ? ((totalMarket - totalCost) / totalCost) * 100 : 0,
    };
  });

  const txnSummary = [
    { date: 'Apr 24', buys: 1, sells: 0, volume: 311700 },
    { date: 'Apr 25', buys: 1, sells: 1, volume: 114720 },
    { date: 'Apr 26', buys: 1, sells: 0, volume: 156300 },
    { date: 'Apr 27', buys: 2, sells: 0, volume: 161492 },
    { date: 'Apr 28', buys: 1, sells: 0, volume: 172405 },
    { date: 'Apr 29', buys: 2, sells: 1, volume: 423135 },
  ];

  return (
    <div className="space-y-6">
      <div className="bg-white rounded-xl border border-slate-200 p-6">
        <div className="text-center border-b border-slate-200 pb-4 mb-4">
          <div className="text-lg font-bold text-slate-900">DAILY POSITION REPORT</div>
          <div className="text-sm text-slate-500 mt-1">Report Date: April 29, 2026</div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b-2 border-slate-200">
                <th className="text-left py-2 px-3 font-semibold text-slate-700">Portfolio</th>
                <th className="text-right py-2 px-3 font-semibold text-slate-700">Holdings</th>
                <th className="text-right py-2 px-3 font-semibold text-slate-700">Cost Basis</th>
                <th className="text-right py-2 px-3 font-semibold text-slate-700">Market Value</th>
                <th className="text-right py-2 px-3 font-semibold text-slate-700">P&L</th>
                <th className="text-right py-2 px-3 font-semibold text-slate-700">P&L %</th>
              </tr>
            </thead>
            <tbody>
              {portfolioSummary.map((row, i) => (
                <tr key={i} className="border-b border-slate-100">
                  <td className="py-2.5 px-3 font-medium text-slate-800">{row.name}</td>
                  <td className="py-2.5 px-3 text-right">{row.holdings}</td>
                  <td className="py-2.5 px-3 text-right">${row.costBasis.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</td>
                  <td className="py-2.5 px-3 text-right font-medium">${row.marketValue.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</td>
                  <td className={`py-2.5 px-3 text-right font-medium ${row.pnl >= 0 ? 'text-emerald-600' : 'text-red-600'}`}>
                    {row.pnl >= 0 ? '+' : ''}${row.pnl.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                  </td>
                  <td className={`py-2.5 px-3 text-right ${row.pnl >= 0 ? 'text-emerald-600' : 'text-red-600'}`}>
                    {row.pnlPct.toFixed(2)}%
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <Card title="Transaction Activity (Last 6 Days)">
        <div className="h-52">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={txnSummary}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
              <XAxis dataKey="date" tick={{ fontSize: 12 }} stroke="#94a3b8" />
              <YAxis tick={{ fontSize: 12 }} stroke="#94a3b8" />
              <Tooltip />
              <Legend />
              <Bar dataKey="buys" name="Buys" fill="#10b981" radius={[2, 2, 0, 0]} />
              <Bar dataKey="sells" name="Sells" fill="#ef4444" radius={[2, 2, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </Card>
    </div>
  );
}

function AuditReport() {
  const typeBreakdown = [
    { name: 'Transaction', value: auditRecords.filter(a => a.type === 'TRAN').length },
    { name: 'System', value: auditRecords.filter(a => a.type === 'SYST').length },
    { name: 'User', value: auditRecords.filter(a => a.type === 'USER').length },
  ];

  const statusBreakdown = [
    { name: 'Success', value: auditRecords.filter(a => a.status === 'SUCC').length },
    { name: 'Failed', value: auditRecords.filter(a => a.status === 'FAIL').length },
    { name: 'Warning', value: auditRecords.filter(a => a.status === 'WARN').length },
  ];

  return (
    <div className="space-y-6">
      <div className="bg-white rounded-xl border border-slate-200 p-6">
        <div className="text-center border-b border-slate-200 pb-4 mb-4">
          <div className="text-lg font-bold text-slate-900">SYSTEM AUDIT REPORT</div>
          <div className="text-sm text-slate-500 mt-1">Report Date: April 29, 2026</div>
        </div>

        <div className="grid grid-cols-3 gap-4 mb-6">
          <div className="text-center p-3 bg-slate-50 rounded-lg">
            <div className="text-2xl font-bold text-slate-900">{auditRecords.length}</div>
            <div className="text-xs text-slate-500 mt-1">Total Events</div>
          </div>
          <div className="text-center p-3 bg-emerald-50 rounded-lg">
            <div className="text-2xl font-bold text-emerald-700">{auditRecords.filter(a => a.status === 'SUCC').length}</div>
            <div className="text-xs text-slate-500 mt-1">Successful</div>
          </div>
          <div className="text-center p-3 bg-red-50 rounded-lg">
            <div className="text-2xl font-bold text-red-700">{auditRecords.filter(a => a.status === 'FAIL').length}</div>
            <div className="text-xs text-slate-500 mt-1">Failed</div>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card title="Events by Type">
          <div className="h-52">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie data={typeBreakdown} cx="50%" cy="50%" outerRadius={80} dataKey="value" label={(props) => `${props.name ?? ''}: ${props.value ?? 0}`}>
                  {typeBreakdown.map((_, i) => (
                    <Cell key={i} fill={COLORS[i]} />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </Card>
        <Card title="Events by Status">
          <div className="h-52">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie data={statusBreakdown} cx="50%" cy="50%" outerRadius={80} dataKey="value" label={(props) => `${props.name ?? ''}: ${props.value ?? 0}`}>
                  <Cell fill="#10b981" />
                  <Cell fill="#ef4444" />
                  <Cell fill="#f59e0b" />
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </Card>
      </div>
    </div>
  );
}

function StatisticsReport() {
  const completedJobs = batchJobs.filter(j => j.status === 'D');
  const failedJobs = batchJobs.filter(j => j.status === 'E');
  const successRate = completedJobs.length > 0
    ? (completedJobs.length / (completedJobs.length + failedJobs.length)) * 100 : 0;

  const performanceData = [
    { time: '06:00', cpu: 25, memory: 42, io: 15 },
    { time: '08:00', cpu: 72, memory: 65, io: 48 },
    { time: '10:00', cpu: 58, memory: 60, io: 35 },
    { time: '12:00', cpu: 45, memory: 55, io: 28 },
    { time: '14:00', cpu: 62, memory: 58, io: 40 },
    { time: '16:00', cpu: 78, memory: 70, io: 55 },
    { time: '18:00', cpu: 35, memory: 48, io: 20 },
    { time: '20:00', cpu: 22, memory: 40, io: 12 },
  ];

  return (
    <div className="space-y-6">
      <div className="bg-white rounded-xl border border-slate-200 p-6">
        <div className="text-center border-b border-slate-200 pb-4 mb-4">
          <div className="text-lg font-bold text-slate-900">SYSTEM STATISTICS AND PERFORMANCE REPORT</div>
          <div className="text-sm text-slate-500 mt-1">Report Date: April 29, 2026</div>
        </div>

        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          <div className="text-center p-3 bg-slate-50 rounded-lg">
            <div className="text-2xl font-bold text-slate-900">{batchJobs.length}</div>
            <div className="text-xs text-slate-500 mt-1">Total Batch Jobs</div>
          </div>
          <div className="text-center p-3 bg-emerald-50 rounded-lg">
            <div className="text-2xl font-bold text-emerald-700">{completedJobs.length}</div>
            <div className="text-xs text-slate-500 mt-1">Completed</div>
          </div>
          <div className="text-center p-3 bg-red-50 rounded-lg">
            <div className="text-2xl font-bold text-red-700">{failedJobs.length}</div>
            <div className="text-xs text-slate-500 mt-1">Failed</div>
          </div>
          <div className="text-center p-3 bg-blue-50 rounded-lg">
            <div className="text-2xl font-bold text-blue-700">{successRate.toFixed(1)}%</div>
            <div className="text-xs text-slate-500 mt-1">Success Rate</div>
          </div>
        </div>
      </div>

      <Card title="System Resource Utilization (Today)">
        <div className="h-64">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={performanceData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
              <XAxis dataKey="time" tick={{ fontSize: 12 }} stroke="#94a3b8" />
              <YAxis tick={{ fontSize: 12 }} stroke="#94a3b8" tickFormatter={(v: number) => `${v}%`} />
              <Tooltip formatter={(v) => `${v}%`} />
              <Legend />
              <Line type="monotone" dataKey="cpu" name="CPU" stroke="#3b82f6" strokeWidth={2} dot={false} />
              <Line type="monotone" dataKey="memory" name="Memory" stroke="#10b981" strokeWidth={2} dot={false} />
              <Line type="monotone" dataKey="io" name="I/O" stroke="#f59e0b" strokeWidth={2} dot={false} />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </Card>

      <Card title="DB2 Performance Metrics">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-200">
                <th className="text-left py-2.5 px-3 font-semibold text-slate-700">Metric</th>
                <th className="text-right py-2.5 px-3 font-semibold text-slate-700">Value</th>
                <th className="text-right py-2.5 px-3 font-semibold text-slate-700">Threshold</th>
                <th className="text-left py-2.5 px-3 font-semibold text-slate-700">Status</th>
              </tr>
            </thead>
            <tbody>
              {[
                { metric: 'DB2 Calls', value: '12,847', threshold: '50,000', ok: true },
                { metric: 'Avg Response Time', value: '2.3ms', threshold: '10ms', ok: true },
                { metric: 'Connection Pool Usage', value: '65%', threshold: '90%', ok: true },
                { metric: 'Buffer Pool Hit Ratio', value: '98.5%', threshold: '95%', ok: true },
                { metric: 'Lock Escalations', value: '3', threshold: '10', ok: true },
                { metric: 'Deadlocks', value: '0', threshold: '0', ok: true },
              ].map((row, i) => (
                <tr key={i} className="border-b border-slate-100">
                  <td className="py-2.5 px-3 font-medium text-slate-800">{row.metric}</td>
                  <td className="py-2.5 px-3 text-right">{row.value}</td>
                  <td className="py-2.5 px-3 text-right text-slate-500">{row.threshold}</td>
                  <td className="py-2.5 px-3">
                    <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${
                      row.ok ? 'bg-emerald-100 text-emerald-700' : 'bg-red-100 text-red-700'
                    }`}>
                      {row.ok ? 'Normal' : 'Alert'}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  );
}
