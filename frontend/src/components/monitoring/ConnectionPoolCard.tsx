import type { ConnectionPoolStats } from '@/types';
import { cn } from '@/lib/utils';

interface ConnectionPoolCardProps {
  stats: ConnectionPoolStats;
}

function MiniTrendChart({ data }: { data: number[] }) {
  const max = Math.max(...data, 1);
  const height = 40;
  const width = 200;
  const step = width / (data.length - 1);

  const points = data
    .map((v, i) => `${i * step},${height - (v / max) * height}`)
    .join(' ');

  const areaPoints = `0,${height} ${points} ${width},${height}`;

  return (
    <svg
      viewBox={`0 0 ${width} ${height}`}
      className="w-full h-10"
      preserveAspectRatio="none"
      aria-hidden="true"
    >
      <polygon points={areaPoints} fill="#3b82f6" fillOpacity="0.1" />
      <polyline points={points} fill="none" stroke="#3b82f6" strokeWidth="1.5" />
    </svg>
  );
}

export default function ConnectionPoolCard({ stats }: ConnectionPoolCardProps) {
  const activePercent = (stats.active / stats.maxTotal) * 100;
  const idlePercent = (stats.idle / stats.maxTotal) * 100;
  const availablePercent = 100 - activePercent - idlePercent;

  const responseColor = stats.avgResponseMs < 50
    ? 'text-green-600'
    : stats.avgResponseMs <= 100
      ? 'text-yellow-600'
      : 'text-red-600';

  const responseBg = stats.avgResponseMs < 50
    ? 'bg-green-100'
    : stats.avgResponseMs <= 100
      ? 'bg-yellow-100'
      : 'bg-red-100';

  return (
    <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
      <h3 className="text-sm font-medium text-gray-600 mb-4">DB2 Connection Pool</h3>

      <div
        className="flex h-4 rounded-full overflow-hidden mb-4"
        role="img"
        aria-label={`Connection pool: ${stats.active} active, ${stats.idle} idle, ${stats.maxTotal - stats.total} available out of ${stats.maxTotal} max`}
      >
        <div
          className="bg-blue-500 transition-all duration-500"
          style={{ width: `${activePercent}%` }}
          title={`Active: ${stats.active}`}
        />
        <div
          className="bg-gray-400 transition-all duration-500"
          style={{ width: `${idlePercent}%` }}
          title={`Idle: ${stats.idle}`}
        />
        <div
          className="bg-gray-200 transition-all duration-500"
          style={{ width: `${availablePercent}%` }}
          title={`Available: ${stats.maxTotal - stats.total}`}
        />
      </div>

      <div className="flex items-center gap-4 mb-4 text-xs text-gray-500">
        <span className="flex items-center gap-1">
          <span className="h-2.5 w-2.5 rounded-full bg-blue-500" aria-hidden="true" /> Active
        </span>
        <span className="flex items-center gap-1">
          <span className="h-2.5 w-2.5 rounded-full bg-gray-400" aria-hidden="true" /> Idle
        </span>
        <span className="flex items-center gap-1">
          <span className="h-2.5 w-2.5 rounded-full bg-gray-200" aria-hidden="true" /> Available
        </span>
      </div>

      <div className="grid grid-cols-2 gap-3 mb-4">
        <div>
          <p className="text-xs text-gray-400">Active</p>
          <p className="text-lg font-semibold text-gray-900">{stats.active}</p>
        </div>
        <div>
          <p className="text-xs text-gray-400">Idle</p>
          <p className="text-lg font-semibold text-gray-900">{stats.idle}</p>
        </div>
        <div>
          <p className="text-xs text-gray-400">Total / Max</p>
          <p className="text-lg font-semibold text-gray-900">{stats.total} / {stats.maxTotal}</p>
        </div>
        <div>
          <p className="text-xs text-gray-400">Wait Queue</p>
          <p className="text-lg font-semibold text-gray-900">{stats.waitCount}</p>
        </div>
      </div>

      <div className="flex items-center gap-2 mb-4">
        <span className="text-xs text-gray-400">Avg Response:</span>
        <span className={cn('text-sm font-semibold px-2 py-0.5 rounded', responseBg, responseColor)}>
          {stats.avgResponseMs}ms
        </span>
      </div>

      <div>
        <p className="text-xs text-gray-400 mb-1">Active Connections (24h)</p>
        <MiniTrendChart data={stats.trend} />
      </div>
    </div>
  );
}
