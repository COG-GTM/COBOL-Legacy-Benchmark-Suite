import type { ConnectionPoolStats } from "../../types";

interface ConnectionPoolCardProps {
  pool: ConnectionPoolStats;
}

function MiniTrendChart({ data }: { data: number[] }) {
  const max = Math.max(...data);
  const min = Math.min(...data);
  const range = max - min || 1;
  const width = 200;
  const height = 40;
  const padding = 2;

  const points = data
    .map((v, i) => {
      const x = padding + (i / (data.length - 1)) * (width - 2 * padding);
      const y =
        height - padding - ((v - min) / range) * (height - 2 * padding);
      return `${x},${y}`;
    })
    .join(" ");

  const areaPoints = `${padding},${height - padding} ${points} ${width - padding},${height - padding}`;

  return (
    <svg
      viewBox={`0 0 ${width} ${height}`}
      className="w-full"
      aria-hidden="true"
    >
      <polygon points={areaPoints} fill="#3b82f6" opacity="0.15" />
      <polyline
        points={points}
        fill="none"
        stroke="#3b82f6"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function getResponseColor(ms: number) {
  if (ms > 100) return { bg: "bg-red-100", text: "text-red-700" };
  if (ms >= 50) return { bg: "bg-yellow-100", text: "text-yellow-700" };
  return { bg: "bg-green-100", text: "text-green-700" };
}

export default function ConnectionPoolCard({ pool }: ConnectionPoolCardProps) {
  const available = pool.maxTotal - pool.total;
  const activePercent = (pool.active / pool.maxTotal) * 100;
  const idlePercent = (pool.idle / pool.maxTotal) * 100;
  const availablePercent = (available / pool.maxTotal) * 100;
  const responseColor = getResponseColor(pool.avgResponseMs);

  return (
    <div className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm">
      <h3 className="mb-4 text-sm font-semibold text-gray-700">
        DB2 Connection Pool
      </h3>

      {/* Stacked bar */}
      <div
        className="mb-3 flex h-6 overflow-hidden rounded-full bg-gray-100"
        role="img"
        aria-label={`Connection pool: ${pool.active} active, ${pool.idle} idle, ${available} available out of ${pool.maxTotal} total`}
      >
        <div
          className="bg-blue-500 transition-all"
          style={{ width: `${activePercent}%` }}
          title={`Active: ${pool.active}`}
        />
        <div
          className="bg-blue-200 transition-all"
          style={{ width: `${idlePercent}%` }}
          title={`Idle: ${pool.idle}`}
        />
        <div
          className="bg-gray-200 transition-all"
          style={{ width: `${availablePercent}%` }}
          title={`Available: ${available}`}
        />
      </div>

      {/* Legend */}
      <div className="mb-4 flex flex-wrap gap-x-4 gap-y-1 text-xs text-gray-600">
        <span className="flex items-center gap-1">
          <span className="inline-block h-2.5 w-2.5 rounded-sm bg-blue-500" />
          Active: {pool.active}
        </span>
        <span className="flex items-center gap-1">
          <span className="inline-block h-2.5 w-2.5 rounded-sm bg-blue-200" />
          Idle: {pool.idle}
        </span>
        <span className="flex items-center gap-1">
          <span className="inline-block h-2.5 w-2.5 rounded-sm bg-gray-200" />
          Available: {available}
        </span>
      </div>

      {/* Stats grid */}
      <div className="mb-4 grid grid-cols-2 gap-3 text-sm">
        <div>
          <p className="text-gray-500">Total</p>
          <p className="font-semibold text-gray-900">
            {pool.total}/{pool.maxTotal}
          </p>
        </div>
        <div>
          <p className="text-gray-500">Wait Queue</p>
          <p className="font-semibold text-gray-900">{pool.waitCount}</p>
        </div>
        <div className="col-span-2">
          <p className="text-gray-500">Avg Response Time</p>
          <div className="flex items-center gap-2">
            <p className="font-semibold text-gray-900">{pool.avgResponseMs}ms</p>
            <span
              className={`rounded-full px-2 py-0.5 text-xs font-medium ${responseColor.bg} ${responseColor.text}`}
            >
              {pool.avgResponseMs < 50
                ? "Good"
                : pool.avgResponseMs <= 100
                  ? "Fair"
                  : "Slow"}
            </span>
          </div>
        </div>
      </div>

      {/* Trend */}
      <div>
        <p className="mb-1 text-xs text-gray-400">
          Active connections (24h)
        </p>
        <MiniTrendChart data={pool.trend} />
      </div>
    </div>
  );
}
