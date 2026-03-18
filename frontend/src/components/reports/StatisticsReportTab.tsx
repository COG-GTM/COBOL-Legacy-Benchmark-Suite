import { useMemo } from 'react';
import { TrendingUp, TrendingDown, Minus } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { DataTable } from '@/components/common/DataTable';
import type { ColumnDef } from '@/components/common/DataTable';
import { statisticsReportSummary } from '@/mock/reportsData';
import type { ErrorSummaryEntry, ProcessingThroughput, StatisticsReportEntry } from '@/types/reports';
import { cn } from '@/lib/utils';

const categoryLabels: Record<string, string> = {
  Performance: 'Performance',
  Error: 'Error Rates',
  Volume: 'Volume',
  System: 'System',
};

// For response time / error metrics, "down" is good. For volume/throughput, "up" is good.
const isImprovementForMetric = (metric: StatisticsReportEntry): boolean => {
  const name = metric.metricName.toLowerCase();
  const isLowerBetter =
    name.includes('response time') ||
    name.includes('error') ||
    name.includes('failure') ||
    name.includes('utilization') ||
    name.includes('usage');
  if (metric.trend === 'flat') return true;
  if (isLowerBetter) return metric.trend === 'down';
  return metric.trend === 'up';
};

const statusDotColor = (status: string) => {
  if (status === 'normal') return 'bg-[#4ADE80]';
  if (status === 'warning') return 'bg-amber-400';
  return 'bg-[#F87171]';
};

const severityVariant = (severity: string) => {
  if (severity === 'Info') return 'secondary' as const;
  if (severity === 'Warning') return 'warning' as const;
  if (severity === 'Severe') return 'destructive' as const;
  return 'destructive' as const; // Error
};

function MetricCard({ metric }: { metric: StatisticsReportEntry }) {
  const isGood = isImprovementForMetric(metric);
  const TrendIcon = metric.trend === 'up' ? TrendingUp : metric.trend === 'down' ? TrendingDown : Minus;

  return (
    <div className="rounded-xl border border-[#334155] bg-[#1E293B] p-4">
      <div className="flex items-start justify-between">
        <p className="text-sm font-medium text-[#94A3B8]">{metric.metricName}</p>
        <div className={cn('h-2.5 w-2.5 shrink-0 rounded-full', statusDotColor(metric.status))} />
      </div>
      <p className="mt-2 text-2xl font-bold text-white">
        {metric.currentValue} <span className="text-sm font-normal text-[#94A3B8]">{metric.unit}</span>
      </p>
      <div className="mt-2 flex items-center gap-2">
        <TrendIcon
          className={cn(
            'h-3.5 w-3.5',
            metric.trend === 'flat' ? 'text-[#94A3B8]' : isGood ? 'text-[#4ADE80]' : 'text-[#F87171]'
          )}
        />
        <span
          className={cn(
            'text-xs font-medium',
            metric.trend === 'flat' ? 'text-[#94A3B8]' : isGood ? 'text-[#4ADE80]' : 'text-[#F87171]'
          )}
        >
          {metric.changePercent === 0 ? 'No change' : `${metric.changePercent > 0 ? '+' : ''}${metric.changePercent}%`}
        </span>
      </div>
    </div>
  );
}

function DailyVolumeChart({
  data,
}: {
  data: { date: string; transactions: number; positions: number; errors: number }[];
}) {
  const chartWidth = 700;
  const chartHeight = 300;
  const paddingLeft = 60;
  const paddingRight = 60;
  const paddingTop = 20;
  const paddingBottom = 50;
  const plotWidth = chartWidth - paddingLeft - paddingRight;
  const plotHeight = chartHeight - paddingTop - paddingBottom;

  const maxMainY = 10000;
  const maxErrorY = 100;

  const xStep = plotWidth / (data.length - 1);

  const toMainY = (val: number) => paddingTop + plotHeight - (val / maxMainY) * plotHeight;
  const toErrorY = (val: number) => paddingTop + plotHeight - (val / maxErrorY) * plotHeight;
  const toX = (i: number) => paddingLeft + i * xStep;

  const transactionsPath = data.map((d, i) => `${i === 0 ? 'M' : 'L'}${toX(i)},${toMainY(d.transactions)}`).join(' ');
  const positionsPath = data.map((d, i) => `${i === 0 ? 'M' : 'L'}${toX(i)},${toMainY(d.positions)}`).join(' ');
  const errorsPath = data.map((d, i) => `${i === 0 ? 'M' : 'L'}${toX(i)},${toErrorY(d.errors)}`).join(' ');

  return (
    <div className="rounded-xl border border-[#334155] bg-[#1E293B] p-6">
      <h3 className="mb-4 text-lg font-semibold text-white">14-Day Volume Trend</h3>
      <div className="overflow-x-auto">
        <svg viewBox={`0 0 ${chartWidth} ${chartHeight}`} className="w-full min-w-[500px]">
          {/* Grid lines */}
          {[0, 2500, 5000, 7500, 10000].map((val) => (
            <g key={val}>
              <line
                x1={paddingLeft}
                y1={toMainY(val)}
                x2={chartWidth - paddingRight}
                y2={toMainY(val)}
                stroke="#334155"
                strokeDasharray="4,4"
              />
              <text x={paddingLeft - 8} y={toMainY(val) + 4} textAnchor="end" className="fill-[#94A3B8] text-[10px]">
                {val.toLocaleString()}
              </text>
            </g>
          ))}
          {/* Right Y-axis labels (errors) */}
          {[0, 25, 50, 75, 100].map((val) => (
            <text
              key={`err-${val}`}
              x={chartWidth - paddingRight + 8}
              y={toErrorY(val) + 4}
              textAnchor="start"
              className="fill-[#F87171] text-[10px]"
            >
              {val}
            </text>
          ))}
          {/* X-axis labels */}
          {data.map((d, i) => {
            if (i % 2 !== 0 && i !== data.length - 1) return null;
            return (
              <text
                key={d.date}
                x={toX(i)}
                y={chartHeight - paddingBottom + 20}
                textAnchor="middle"
                className="fill-[#94A3B8] text-[9px]"
              >
                {d.date.slice(5)}
              </text>
            );
          })}
          {/* Lines */}
          <polyline points={transactionsPath.replace(/[ML]/g, (m) => (m === 'M' ? '' : ' ')).trim()} fill="none" stroke="#60A5FA" strokeWidth="2" />
          <polyline points={positionsPath.replace(/[ML]/g, (m) => (m === 'M' ? '' : ' ')).trim()} fill="none" stroke="#4ADE80" strokeWidth="2" />
          <polyline points={errorsPath.replace(/[ML]/g, (m) => (m === 'M' ? '' : ' ')).trim()} fill="none" stroke="#F87171" strokeWidth="2" strokeDasharray="6,3" />
          {/* Data points */}
          {data.map((d, i) => (
            <g key={`dots-${d.date}`}>
              <circle cx={toX(i)} cy={toMainY(d.transactions)} r="3" fill="#60A5FA" />
              <circle cx={toX(i)} cy={toMainY(d.positions)} r="3" fill="#4ADE80" />
              <circle cx={toX(i)} cy={toErrorY(d.errors)} r="3" fill="#F87171" />
            </g>
          ))}
        </svg>
      </div>
      {/* Legend */}
      <div className="mt-3 flex flex-wrap justify-center gap-6 text-xs">
        <div className="flex items-center gap-2">
          <div className="h-0.5 w-5 bg-[#60A5FA]" />
          <span className="text-[#94A3B8]">Transactions</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="h-0.5 w-5 bg-[#4ADE80]" />
          <span className="text-[#94A3B8]">Positions</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="h-0.5 w-5 border-t border-dashed border-[#F87171]" />
          <span className="text-[#94A3B8]">Errors (right axis)</span>
        </div>
      </div>
    </div>
  );
}

export function StatisticsReportTab() {
  const summary = statisticsReportSummary;

  const metricsByCategory = useMemo(() => {
    const groups = new Map<string, StatisticsReportEntry[]>();
    for (const metric of summary.metrics) {
      if (!groups.has(metric.category)) groups.set(metric.category, []);
      groups.get(metric.category)!.push(metric);
    }
    return groups;
  }, [summary.metrics]);

  const maxErrorCount = Math.max(...summary.errorSummary.map((e) => e.count));

  const errorColumns: ColumnDef<ErrorSummaryEntry>[] = [
    { key: 'errorCode', header: 'Error Code', sortable: true },
    { key: 'errorDescription', header: 'Description', sortable: true },
    {
      key: 'count',
      header: 'Count',
      sortable: true,
      render: (row) => (
        <div className="flex items-center gap-2">
          <span className="w-8 text-right">{row.count}</span>
          <div className="h-3 flex-1 max-w-[100px] rounded bg-[#334155]">
            <div
              className="h-full rounded bg-[#22D3EE]/50"
              style={{ width: `${maxErrorCount > 0 ? (row.count / maxErrorCount) * 100 : 0}%` }}
            />
          </div>
        </div>
      ),
    },
    {
      key: 'severity',
      header: 'Severity',
      sortable: true,
      render: (row) => <Badge variant={severityVariant(row.severity)}>{row.severity}</Badge>,
    },
    { key: 'lastOccurrence', header: 'Last Occurrence', sortable: true },
    { key: 'program', header: 'Program', sortable: true },
  ];

  const throughputColumns: ColumnDef<ProcessingThroughput>[] = [
    { key: 'stepName', header: 'Step Name', sortable: true },
    {
      key: 'recordsProcessed',
      header: 'Records Processed',
      sortable: true,
      render: (row) => row.recordsProcessed.toLocaleString(),
    },
    { key: 'elapsedTime', header: 'Elapsed Time', sortable: true },
    {
      key: 'recordsPerSecond',
      header: 'Records/Second',
      sortable: true,
      render: (row) => (row.recordsPerSecond > 0 ? row.recordsPerSecond.toFixed(1) : 'N/A'),
    },
    {
      key: 'returnCode',
      header: 'Return Code',
      sortable: true,
      render: (row) => {
        const variant =
          row.returnCode === 0 ? 'success' as const : row.returnCode === 4 ? 'warning' as const : 'destructive' as const;
        return <Badge variant={variant}>RC={row.returnCode}</Badge>;
      },
    },
    { key: 'date', header: 'Date', sortable: true },
  ];

  const totalRecords = summary.throughput.reduce((s, t) => s + t.recordsProcessed, 0);

  // Parse elapsed times and sum
  const totalSeconds = summary.throughput.reduce((s, t) => {
    const parts = t.elapsedTime.split(':').map(Number);
    return s + parts[0] * 3600 + parts[1] * 60 + parts[2];
  }, 0);
  const totalHrs = Math.floor(totalSeconds / 3600);
  const totalMins = Math.floor((totalSeconds % 3600) / 60);
  const totalSecs = totalSeconds % 60;
  const totalElapsed = `${String(totalHrs).padStart(2, '0')}:${String(totalMins).padStart(2, '0')}:${String(totalSecs).padStart(2, '0')}`;

  const throughputTotalRow: Record<string, React.ReactNode> = {
    stepName: 'Total',
    recordsProcessed: totalRecords.toLocaleString(),
    elapsedTime: totalElapsed,
    recordsPerSecond: '',
    returnCode: '',
    date: '',
  };

  return (
    <div className="space-y-8">
      {/* Metrics Grid by Category */}
      {Array.from(metricsByCategory.entries()).map(([category, metrics]) => (
        <div key={category}>
          <h3 className="mb-3 text-sm font-semibold uppercase tracking-wider text-[#94A3B8]">
            {categoryLabels[category] ?? category}
          </h3>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {metrics.map((metric) => (
              <MetricCard key={metric.metricName} metric={metric} />
            ))}
          </div>
        </div>
      ))}

      {/* Error Summary Table */}
      <div>
        <h3 className="mb-3 text-lg font-semibold text-white">Error Code Summary</h3>
        <DataTable<ErrorSummaryEntry>
          columns={errorColumns}
          data={summary.errorSummary}
          pageSize={10}
          getRowKey={(row) => row.errorCode}
        />
      </div>

      {/* Processing Throughput Table */}
      <div>
        <h3 className="mb-3 text-lg font-semibold text-white">Batch Processing Throughput</h3>
        <DataTable<ProcessingThroughput>
          columns={throughputColumns}
          data={summary.throughput}
          pageSize={10}
          totalRow={throughputTotalRow}
          getRowKey={(row) => row.stepName}
        />
      </div>

      {/* Daily Volume Trend Chart */}
      <DailyVolumeChart data={summary.dailyVolumes} />
    </div>
  );
}
