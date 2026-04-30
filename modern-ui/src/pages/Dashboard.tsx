import {
  AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  BarChart, Bar, PieChart, Pie, Cell,
} from 'recharts';
import { TrendingUp, Briefcase, ArrowLeftRight, Clock, AlertTriangle } from 'lucide-react';
import Card from '../components/Card';
import StatusBadge from '../components/StatusBadge';
import {
  portfolios, transactions, batchJobs,
  getPortfolioTotalAUM, getActivePortfolioCount,
  getTodayTransactionCount, getPendingTransactionCount,
  getPortfolioValueHistory, getTransactionVolumeByType,
} from '../data/mockData';
import { TXN_TYPE_LABELS, TXN_STATUS_LABELS, BATCH_STATUS_LABELS } from '../types';

const PIE_COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444'];

function formatUSD(value: number): string {
  if (value >= 1_000_000) return `$${(value / 1_000_000).toFixed(1)}M`;
  if (value >= 1_000) return `$${(value / 1_000).toFixed(0)}K`;
  return `$${value.toFixed(2)}`;
}

const KPI_CARDS = [
  {
    label: 'Total AUM',
    value: formatUSD(getPortfolioTotalAUM()),
    change: '+3.2%',
    icon: TrendingUp,
    color: 'text-blue-600 bg-blue-50',
  },
  {
    label: 'Active Portfolios',
    value: getActivePortfolioCount().toString(),
    change: '+1 this month',
    icon: Briefcase,
    color: 'text-emerald-600 bg-emerald-50',
  },
  {
    label: "Today's Transactions",
    value: getTodayTransactionCount().toString(),
    change: '$422.1K volume',
    icon: ArrowLeftRight,
    color: 'text-indigo-600 bg-indigo-50',
  },
  {
    label: 'Pending',
    value: getPendingTransactionCount().toString(),
    change: 'Awaiting processing',
    icon: Clock,
    color: 'text-amber-600 bg-amber-50',
  },
];

export default function Dashboard() {
  const aumHistory = getPortfolioValueHistory();
  const txnVolume = getTransactionVolumeByType();
  const recentTxns = transactions.slice(0, 5);
  const todayBatch = batchJobs.filter(j => j.processDate === '2026-04-29');
  const failedJobs = batchJobs.filter(j => j.status === 'E');

  const allocationData = [
    { name: 'Equities', value: 45 },
    { name: 'Fixed Income', value: 25 },
    { name: 'Intl Equity', value: 18 },
    { name: 'Real Estate', value: 12 },
  ];

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {KPI_CARDS.map(({ label, value, change, icon: Icon, color }) => (
          <div
            key={label}
            className="bg-white rounded-xl border border-slate-200 shadow-sm p-5 flex items-start gap-4"
          >
            <div className={`p-2.5 rounded-lg ${color}`}>
              <Icon size={20} />
            </div>
            <div>
              <div className="text-xs text-slate-500 font-medium">{label}</div>
              <div className="text-2xl font-bold text-slate-900 mt-0.5">{value}</div>
              <div className="text-xs text-slate-400 mt-1">{change}</div>
            </div>
          </div>
        ))}
      </div>

      {failedJobs.length > 0 && (
        <div className="bg-red-50 border border-red-200 rounded-xl p-4 flex items-start gap-3">
          <AlertTriangle size={20} className="text-red-500 mt-0.5 shrink-0" />
          <div>
            <div className="font-semibold text-red-800 text-sm">Batch Job Alert</div>
            <div className="text-red-700 text-sm mt-0.5">
              {failedJobs.length} job(s) failed: {failedJobs.map(j => `${j.jobName} (RC=${j.returnCode})`).join(', ')}.
              {failedJobs[0].errorDesc && ` Error: ${failedJobs[0].errorDesc}`}
            </div>
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <Card title="AUM Trend (12 Months)" className="lg:col-span-2">
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={aumHistory}>
                <defs>
                  <linearGradient id="aumGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.15} />
                    <stop offset="95%" stopColor="#3b82f6" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                <XAxis dataKey="date" tick={{ fontSize: 12 }} stroke="#94a3b8" />
                <YAxis tickFormatter={(v: number) => formatUSD(v)} tick={{ fontSize: 12 }} stroke="#94a3b8" />
                <Tooltip formatter={(v) => formatUSD(Number(v))} />
                <Area type="monotone" dataKey="value" stroke="#3b82f6" fillOpacity={1} fill="url(#aumGrad)" strokeWidth={2} />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </Card>

        <Card title="Asset Allocation">
          <div className="h-64 flex items-center justify-center">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie data={allocationData} cx="50%" cy="50%" innerRadius={55} outerRadius={85} paddingAngle={3} dataKey="value" label={(props) => `${props.name ?? ''} ${((props.percent ?? 0) * 100).toFixed(0)}%`} labelLine={false}>
                  {allocationData.map((_, i) => (
                    <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </Card>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card title="Transaction Volume by Type">
          <div className="h-52">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={txnVolume}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                <XAxis dataKey="type" tick={{ fontSize: 12 }} stroke="#94a3b8" />
                <YAxis tick={{ fontSize: 12 }} stroke="#94a3b8" />
                <Tooltip />
                <Bar dataKey="count" fill="#6366f1" radius={[4, 4, 0, 0]} name="Count" />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </Card>

        <Card title="Batch Pipeline Status (Today)">
          <div className="space-y-3">
            {todayBatch.map((job, i) => (
              <div key={job.id} className="flex items-center gap-3">
                <div className="w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold bg-slate-100 text-slate-600">
                  {i + 1}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="text-sm font-medium text-slate-800">{job.jobName}</div>
                  <div className="text-xs text-slate-400">{job.programName}</div>
                </div>
                <StatusBadge status={job.status} label={BATCH_STATUS_LABELS[job.status]} />
                {job.startTime && (
                  <span className="text-xs text-slate-400">{job.startTime}</span>
                )}
              </div>
            ))}
          </div>
        </Card>
      </div>

      <Card title="Recent Transactions">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-100">
                <th className="text-left py-2 px-3 text-xs font-medium text-slate-500 uppercase">Date</th>
                <th className="text-left py-2 px-3 text-xs font-medium text-slate-500 uppercase">Portfolio</th>
                <th className="text-left py-2 px-3 text-xs font-medium text-slate-500 uppercase">Type</th>
                <th className="text-left py-2 px-3 text-xs font-medium text-slate-500 uppercase">Investment</th>
                <th className="text-right py-2 px-3 text-xs font-medium text-slate-500 uppercase">Qty</th>
                <th className="text-right py-2 px-3 text-xs font-medium text-slate-500 uppercase">Amount</th>
                <th className="text-left py-2 px-3 text-xs font-medium text-slate-500 uppercase">Status</th>
              </tr>
            </thead>
            <tbody>
              {recentTxns.map(txn => {
                const port = portfolios.find(p => p.id === txn.portfolioId);
                return (
                  <tr key={txn.id} className="border-b border-slate-50 hover:bg-slate-50">
                    <td className="py-2.5 px-3 text-slate-600">{txn.date}</td>
                    <td className="py-2.5 px-3 text-slate-800 font-medium">{port?.clientName ?? txn.portfolioId}</td>
                    <td className="py-2.5 px-3">
                      <span className={`text-xs font-medium ${txn.type === 'BU' ? 'text-emerald-600' : txn.type === 'SL' ? 'text-red-600' : 'text-slate-600'}`}>
                        {TXN_TYPE_LABELS[txn.type]}
                      </span>
                    </td>
                    <td className="py-2.5 px-3 text-slate-600">{txn.investmentId}</td>
                    <td className="py-2.5 px-3 text-right text-slate-600">{txn.quantity.toLocaleString()}</td>
                    <td className="py-2.5 px-3 text-right font-medium text-slate-800">
                      ${txn.amount.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                    </td>
                    <td className="py-2.5 px-3">
                      <StatusBadge status={txn.status} label={TXN_STATUS_LABELS[txn.status]} />
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  );
}
