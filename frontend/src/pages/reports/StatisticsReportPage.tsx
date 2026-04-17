import { useMemo } from 'react';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card } from '@/components/ui/Card';
import { batchJobs } from '@/data/mockData';
import {
  LineChart,
  Line,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import { Cpu, HardDrive, Database, MemoryStick } from 'lucide-react';

const transactionVolumeData = [
  { date: 'Aug 09', volume: 890 },
  { date: 'Aug 10', volume: 1120 },
  { date: 'Aug 11', volume: 1340 },
  { date: 'Aug 12', volume: 980 },
  { date: 'Aug 13', volume: 1450 },
  { date: 'Aug 14', volume: 1280 },
  { date: 'Aug 15', volume: 1250 },
];

const transactionsByType = [
  { type: 'Buy', count: 3200 },
  { type: 'Sell', count: 1850 },
  { type: 'Fee', count: 420 },
];

const portfolioStatusData = [
  { name: 'Active', value: 10 },
  { name: 'Inactive', value: 1 },
  { name: 'Closed', value: 1 },
];

const PIE_COLORS = ['#10b981', '#f59e0b', '#ef4444'];
const BAR_COLORS = ['#3b82f6', '#ef4444', '#f59e0b'];

interface MetricCardProps {
  icon: React.ReactNode;
  label: string;
  value: string;
  subLabel?: string;
  colorClass?: string;
}

function MetricCard({ icon, label, value, subLabel, colorClass = 'text-blue-600' }: MetricCardProps) {
  return (
    <div className="bg-white rounded-lg border border-slate-200 shadow-sm p-5">
      <div className="flex items-center gap-3">
        <div className={`p-2 rounded-lg bg-slate-100 ${colorClass}`}>{icon}</div>
        <div>
          <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">{label}</p>
          <p className="text-xl font-bold text-slate-900">{value}</p>
          {subLabel && <p className="text-xs text-slate-400">{subLabel}</p>}
        </div>
      </div>
    </div>
  );
}

function parseDuration(start: string, end: string): number {
  if (!start || !end) return 0;
  const [sh, sm, ss] = start.split(':').map(Number);
  const [eh, em, es] = end.split(':').map(Number);
  return (eh * 3600 + em * 60 + es) - (sh * 3600 + sm * 60 + ss);
}

function formatDuration(seconds: number): string {
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${m}m ${s}s`;
}

export function StatisticsReportPage() {
  const completedJobs = useMemo(() => batchJobs.filter((j) => j.status === 'C'), []);

  const avgDuration = useMemo(() => {
    const durations = completedJobs.map((j) => parseDuration(j.startTime, j.endTime));
    if (durations.length === 0) return 0;
    return Math.round(durations.reduce((a, b) => a + b, 0) / durations.length);
  }, [completedJobs]);

  const totalRecords = completedJobs.reduce((sum, j) => sum + j.recordCount, 0);
  const totalErrors = completedJobs.reduce((sum, j) => sum + j.errorCount, 0);
  const errorRate = totalRecords > 0 ? ((totalErrors / totalRecords) * 100).toFixed(2) : '0.00';

  return (
    <div>
      <PageHeader
        title="Statistics Report"
        description="Modernized from COBOL program RPTSTA00 \u2014 Statistics Report Generator"
      />

      {/* System Performance Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <MetricCard
          icon={<Cpu className="w-5 h-5" />}
          label="CPU Utilization"
          value="67%"
          subLabel="z/OS LPAR"
          colorClass="text-blue-600"
        />
        <MetricCard
          icon={<MemoryStick className="w-5 h-5" />}
          label="Memory Usage"
          value="4.2 GB"
          subLabel="of 8 GB allocated"
          colorClass="text-emerald-600"
        />
        <MetricCard
          icon={<HardDrive className="w-5 h-5" />}
          label="DASD Usage"
          value="72%"
          subLabel="3600 cylinders"
          colorClass="text-amber-600"
        />
        <MetricCard
          icon={<Database className="w-5 h-5" />}
          label="DB2 Connections"
          value="12"
          subLabel="Active threads"
          colorClass="text-purple-600"
        />
      </div>

      {/* Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
        <Card title="Transaction Volume \u2014 Last 7 Days">
          <ResponsiveContainer width="100%" height={280}>
            <LineChart data={transactionVolumeData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
              <XAxis dataKey="date" tick={{ fontSize: 12 }} stroke="#94a3b8" />
              <YAxis tick={{ fontSize: 12 }} stroke="#94a3b8" />
              <Tooltip />
              <Line
                type="monotone"
                dataKey="volume"
                stroke="#3b82f6"
                strokeWidth={2}
                dot={{ fill: '#3b82f6', r: 4 }}
                activeDot={{ r: 6 }}
              />
            </LineChart>
          </ResponsiveContainer>
        </Card>

        <Card title="Transactions by Type">
          <ResponsiveContainer width="100%" height={280}>
            <BarChart data={transactionsByType}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
              <XAxis dataKey="type" tick={{ fontSize: 12 }} stroke="#94a3b8" />
              <YAxis tick={{ fontSize: 12 }} stroke="#94a3b8" />
              <Tooltip />
              <Bar dataKey="count" radius={[4, 4, 0, 0]}>
                {transactionsByType.map((_, index) => (
                  <Cell key={`bar-cell-${index}`} fill={BAR_COLORS[index]} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </Card>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
        <Card title="Portfolio Status Distribution">
          <ResponsiveContainer width="100%" height={280}>
            <PieChart>
              <Pie
                data={portfolioStatusData}
                cx="50%"
                cy="50%"
                innerRadius={60}
                outerRadius={100}
                paddingAngle={3}
                dataKey="value"
                label={(props: { name?: string; percent?: number }) => `${props.name ?? ''} ${((props.percent ?? 0) * 100).toFixed(0)}%`}
              >
                {portfolioStatusData.map((_, index) => (
                  <Cell key={`pie-cell-${index}`} fill={PIE_COLORS[index]} />
                ))}
              </Pie>
              <Tooltip />
              <Legend />
            </PieChart>
          </ResponsiveContainer>
        </Card>

        <Card title="Batch Processing Metrics">
          <div className="grid grid-cols-2 gap-4 mb-5">
            <div className="bg-slate-50 rounded-lg p-3 text-center">
              <p className="text-xs font-semibold text-slate-500 uppercase">Avg Duration</p>
              <p className="mt-1 text-lg font-bold text-slate-900">{formatDuration(avgDuration)}</p>
            </div>
            <div className="bg-slate-50 rounded-lg p-3 text-center">
              <p className="text-xs font-semibold text-slate-500 uppercase">Records Today</p>
              <p className="mt-1 text-lg font-bold text-slate-900">{totalRecords.toLocaleString()}</p>
            </div>
            <div className="bg-slate-50 rounded-lg p-3 text-center col-span-2">
              <p className="text-xs font-semibold text-slate-500 uppercase">Error Rate</p>
              <p className="mt-1 text-lg font-bold text-red-600">{errorRate}%</p>
            </div>
          </div>

          <h4 className="text-sm font-semibold text-slate-700 mb-2">Job Completion Times</h4>
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-slate-200 text-xs">
              <thead className="bg-slate-50">
                <tr>
                  <th className="px-3 py-2 text-left font-semibold text-slate-600 uppercase tracking-wider">Job</th>
                  <th className="px-3 py-2 text-left font-semibold text-slate-600 uppercase tracking-wider">Start</th>
                  <th className="px-3 py-2 text-left font-semibold text-slate-600 uppercase tracking-wider">End</th>
                  <th className="px-3 py-2 text-left font-semibold text-slate-600 uppercase tracking-wider">Duration</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200">
                {completedJobs.map((job) => (
                  <tr key={job.processId} className="hover:bg-slate-50">
                    <td className="px-3 py-2 font-mono text-slate-700">{job.processId}</td>
                    <td className="px-3 py-2 font-mono text-slate-600">{job.startTime}</td>
                    <td className="px-3 py-2 font-mono text-slate-600">{job.endTime}</td>
                    <td className="px-3 py-2 font-mono text-slate-600">{formatDuration(parseDuration(job.startTime, job.endTime))}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      </div>
    </div>
  );
}
