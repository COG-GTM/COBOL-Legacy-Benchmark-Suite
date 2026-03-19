import type { ErrorRateData } from '@/types';
import { cn } from '@/lib/utils';
import { ArrowDown, ArrowUp } from 'lucide-react';

interface ErrorRatePanelProps {
  data: ErrorRateData;
}

const severityColors = {
  critical: { bar: 'bg-red-500', badge: 'bg-red-100 text-red-700' },
  warning: { bar: 'bg-yellow-500', badge: 'bg-yellow-100 text-yellow-700' },
  info: { bar: 'bg-blue-500', badge: 'bg-blue-100 text-blue-700' },
};

function HourlyChart({ data }: { data: number[] }) {
  const max = Math.max(...data, 1);
  const height = 48;
  const width = 240;
  const step = width / (data.length - 1);

  const points = data
    .map((v, i) => `${i * step},${height - (v / max) * (height - 4)}`)
    .join(' ');

  const areaPoints = `0,${height} ${points} ${width},${height}`;

  return (
    <svg
      viewBox={`0 0 ${width} ${height}`}
      className="w-full h-12"
      preserveAspectRatio="none"
      aria-hidden="true"
    >
      <polygon points={areaPoints} fill="#ef4444" fillOpacity="0.1" />
      <polyline points={points} fill="none" stroke="#ef4444" strokeWidth="1.5" />
    </svg>
  );
}

export default function ErrorRatePanel({ data }: ErrorRatePanelProps) {
  const changePercent = data.totalYesterday > 0
    ? ((data.totalToday - data.totalYesterday) / data.totalYesterday * 100).toFixed(1)
    : '0';
  const isDown = data.totalToday < data.totalYesterday;
  const maxCount = Math.max(...data.byCategory.map((c) => c.count));

  return (
    <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
      <h3 className="text-sm font-medium text-gray-600 mb-4">Error Rate</h3>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
        <div className="space-y-3">
          <div>
            <p className="text-xs text-gray-400">Current Rate</p>
            <p className="text-2xl font-semibold text-gray-900">{data.currentRate}<span className="text-sm font-normal text-gray-500">/min</span></p>
          </div>
          <div>
            <p className="text-xs text-gray-400">Total Today</p>
            <p className="text-2xl font-semibold text-gray-900">{data.totalToday}</p>
          </div>
          <div className="flex items-center gap-2">
            <p className="text-xs text-gray-400">vs Yesterday ({data.totalYesterday})</p>
            <span className={cn('inline-flex items-center text-sm font-medium', isDown ? 'text-green-600' : 'text-red-600')}>
              {isDown ? <ArrowDown className="h-3.5 w-3.5" /> : <ArrowUp className="h-3.5 w-3.5" />}
              {Math.abs(Number(changePercent))}%
            </span>
          </div>
        </div>

        <div className="space-y-2">
          {data.byCategory.map((cat) => (
            <div key={cat.category} className="flex items-center gap-2">
              <div className="flex-1 min-w-0">
                <div className="flex items-center justify-between mb-0.5">
                  <span className="text-xs text-gray-600 truncate">{cat.category}</span>
                  <span className="text-xs font-medium text-gray-900 ml-2">{cat.count}</span>
                </div>
                <div className="h-2 bg-gray-100 rounded-full overflow-hidden">
                  <div
                    className={cn('h-full rounded-full transition-all duration-500', severityColors[cat.severity].bar)}
                    style={{ width: `${(cat.count / maxCount) * 100}%` }}
                  />
                </div>
              </div>
              <span className={cn('text-xs rounded-full px-1.5 py-0.5 font-medium shrink-0', severityColors[cat.severity].badge)}>
                {cat.severity}
              </span>
            </div>
          ))}
        </div>
      </div>

      <div className="mt-4">
        <p className="text-xs text-gray-400 mb-1">Hourly Errors (24h)</p>
        <HourlyChart data={data.hourlyErrors} />
      </div>
    </div>
  );
}
