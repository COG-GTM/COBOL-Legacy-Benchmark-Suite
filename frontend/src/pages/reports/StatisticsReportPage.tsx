import { useMemo } from 'react';
import { Download, FileText, TrendingUp, TrendingDown, Minus } from 'lucide-react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { StatusBadge } from '@/components/ui/StatusBadge';

type SystemMetric = Record<string, unknown> & {
  metricName: string;
  value: number;
  unit: string;
  trend: 'up' | 'down' | 'stable';
  threshold: number;
  lowerIsBetter?: boolean;
};

type DailyVolume = {
  date: string;
  transactions: number;
};

const systemMetrics: SystemMetric[] = [
  { metricName: 'Transactions/Day', value: 1247, unit: 'txn', trend: 'up', threshold: 2000 },
  { metricName: 'Avg Response Time', value: 145, unit: 'ms', trend: 'down', threshold: 200, lowerIsBetter: true },
  { metricName: 'Error Rate', value: 0.24, unit: '%', trend: 'down', threshold: 1.0, lowerIsBetter: true },
  { metricName: 'CPU Utilization', value: 68, unit: '%', trend: 'up', threshold: 85, lowerIsBetter: true },
  { metricName: 'VSAM I/O Ops', value: 8450, unit: 'ops/hr', trend: 'stable', threshold: 12000, lowerIsBetter: true },
  { metricName: 'DB2 Queries/Hour', value: 3280, unit: 'q/hr', trend: 'up', threshold: 5000, lowerIsBetter: true },
  { metricName: 'Batch Duration', value: 58, unit: 'min', trend: 'down', threshold: 90, lowerIsBetter: true },
  { metricName: 'Active Sessions', value: 24, unit: 'sessions', trend: 'stable', threshold: 50, lowerIsBetter: true },
];

function generateDailyVolume(): DailyVolume[] {
  const data: DailyVolume[] = [];
  const baseDate = new Date('2024-08-15');
  for (let i = 29; i >= 0; i--) {
    const d = new Date(baseDate);
    d.setDate(d.getDate() - i);
    const isWeekend = d.getDay() === 0 || d.getDay() === 6;
    const base = isWeekend ? 400 : 1100;
    const variance = Math.floor(Math.random() * 300) - 150;
    data.push({
      date: d.toISOString().slice(5, 10),
      transactions: Math.max(200, base + variance),
    });
  }
  return data;
}

const dailyVolume = generateDailyVolume();

const kpiCards = [
  { label: 'Transactions Today', value: '1,247', color: 'text-blue-600' },
  { label: 'Avg Response Time', value: '145ms', color: 'text-emerald-600' },
  { label: 'Error Rate', value: '0.24%', color: 'text-emerald-600' },
  { label: 'System Uptime', value: '99.97%', color: 'text-emerald-600' },
];

function downloadCsv(data: SystemMetric[], filename: string) {
  const headers = ['Metric Name', 'Current Value', 'Unit', 'Trend', 'Threshold', 'Status'];
  const rows = data.map((r) => [
    r.metricName,
    r.value.toString(),
    r.unit,
    r.trend,
    r.threshold.toString(),
    r.value <= r.threshold ? 'OK' : 'Warning',
  ]);
  const csv = [headers.join(','), ...rows.map((r) => r.join(','))].join('\n');
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}

function TrendIcon({ trend, lowerIsBetter }: { trend: 'up' | 'down' | 'stable'; lowerIsBetter?: boolean }) {
  const isPositive = trend === 'stable'
    ? null
    : lowerIsBetter ? trend === 'down' : trend === 'up';
  const color = isPositive === null ? 'text-slate-400' : isPositive ? 'text-emerald-600' : 'text-red-500';
  switch (trend) {
    case 'up':
      return <TrendingUp className={`w-4 h-4 ${color}`} />;
    case 'down':
      return <TrendingDown className={`w-4 h-4 ${color}`} />;
    case 'stable':
      return <Minus className={`w-4 h-4 ${color}`} />;
  }
}

export function StatisticsReportPage() {
  const columns: Column<SystemMetric>[] = useMemo(() => [
    { key: 'metricName', header: 'Metric', sortable: true, render: (r) => (
      <span className="font-medium text-slate-900">{r.metricName}</span>
    )},
    { key: 'value', header: 'Current Value', sortable: true, className: 'text-right', render: (r) => (
      <span className="text-right block font-mono">
        {typeof r.value === 'number' && r.value < 10 ? r.value.toFixed(2) : r.value.toLocaleString()}
      </span>
    )},
    { key: 'unit', header: 'Unit', sortable: false },
    { key: 'trend', header: 'Trend', sortable: true, render: (r) => (
      <div className="flex items-center gap-1.5">
        <TrendIcon trend={r.trend} lowerIsBetter={r.lowerIsBetter} />
        <span className="text-sm capitalize text-slate-600">{r.trend}</span>
      </div>
    )},
    { key: 'threshold', header: 'Threshold', sortable: true, className: 'text-right', render: (r) => (
      <span className="text-right block font-mono text-slate-500">{r.threshold.toLocaleString()}</span>
    )},
    { key: 'status', header: 'Status', sortable: false, render: (r) => (
      <StatusBadge
        label={r.value <= r.threshold ? 'OK' : 'Warning'}
        variant={r.value <= r.threshold ? 'success' : 'warning'}
      />
    )},
  ], []);

  const today = new Date().toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' });
  const now = new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });

  return (
    <div>
      <PageHeader
        title="Statistics Report"
        description="System performance metrics generated by RPTSTA00"
        actions={
          <button
            onClick={() => downloadCsv(systemMetrics, `statistics-report-${new Date().toISOString().slice(0, 10)}.csv`)}
            className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors"
          >
            <Download className="w-4 h-4" />
            Download CSV
          </button>
        }
      />

      <div className="flex items-center gap-3 mb-6 text-sm text-slate-500">
        <FileText className="w-4 h-4" />
        <span>Generated: {today} at {now}</span>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        {kpiCards.map((card) => (
          <div key={card.label} className="bg-white rounded-lg border border-slate-200 shadow-sm p-4">
            <p className="text-sm font-medium text-slate-500">{card.label}</p>
            <p className={`text-2xl font-bold mt-1 ${card.color}`}>{card.value}</p>
          </div>
        ))}
      </div>

      <Card title="Daily Transaction Volume (Last 30 Days)" className="mb-6">
        <div className="h-72">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={dailyVolume} margin={{ top: 5, right: 20, left: 10, bottom: 5 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
              <XAxis dataKey="date" tick={{ fontSize: 11, fill: '#64748b' }} />
              <YAxis tick={{ fontSize: 11, fill: '#64748b' }} />
              <Tooltip
                contentStyle={{ borderRadius: '0.5rem', border: '1px solid #e2e8f0', fontSize: '0.875rem' }}
                labelStyle={{ fontWeight: 600 }}
              />
              <Bar dataKey="transactions" fill="#3b82f6" radius={[4, 4, 0, 0]} name="Transactions" />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </Card>

      <Card title="System Metrics">
        <div className="-m-6 mt-0">
          <DataTable<SystemMetric>
            columns={columns}
            data={systemMetrics}
            keyExtractor={(r) => r.metricName}
            emptyMessage="No metrics available"
          />
        </div>
      </Card>
    </div>
  );
}
