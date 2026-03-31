import { TrendingDown, TrendingUp } from "lucide-react";
import type { ErrorRateData } from "../../types";

interface ErrorRatePanelProps {
  errorData: ErrorRateData;
}

const severityColors = {
  critical: { bar: "bg-red-500", badge: "bg-red-100 text-red-700" },
  warning: { bar: "bg-yellow-500", badge: "bg-yellow-100 text-yellow-700" },
  info: { bar: "bg-blue-500", badge: "bg-blue-100 text-blue-700" },
};

function HourlyChart({ data }: { data: number[] }) {
  const max = Math.max(...data, 1);
  const width = 200;
  const height = 40;
  const padding = 2;

  const points = data
    .map((v, i) => {
      const x = padding + (i / (data.length - 1)) * (width - 2 * padding);
      const y = height - padding - (v / max) * (height - 2 * padding);
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
      <polygon points={areaPoints} fill="#ef4444" opacity="0.12" />
      <polyline
        points={points}
        fill="none"
        stroke="#ef4444"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

export default function ErrorRatePanel({ errorData }: ErrorRatePanelProps) {
  const changePercent =
    ((errorData.totalToday - errorData.totalYesterday) /
      errorData.totalYesterday) *
    100;
  const isDown = changePercent < 0;
  const maxCount = Math.max(...errorData.byCategory.map((c) => c.count));

  return (
    <div className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm">
      <h3 className="mb-4 text-sm font-semibold text-gray-700">Error Rates</h3>

      <div className="mb-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
        {/* Left: Summary metrics */}
        <div className="space-y-3">
          <div>
            <p className="text-xs text-gray-500">Current Error Rate</p>
            <p className="text-2xl font-bold text-gray-900">
              {errorData.currentRate}
              <span className="text-sm font-normal text-gray-500">/min</span>
            </p>
          </div>
          <div className="flex gap-4">
            <div>
              <p className="text-xs text-gray-500">Today</p>
              <p className="text-lg font-semibold text-gray-900">
                {errorData.totalToday}
              </p>
            </div>
            <div>
              <p className="text-xs text-gray-500">Yesterday</p>
              <p className="text-lg font-semibold text-gray-900">
                {errorData.totalYesterday}
              </p>
            </div>
            <div>
              <p className="text-xs text-gray-500">Change</p>
              <div
                className={`flex items-center gap-1 text-sm font-semibold ${
                  isDown ? "text-green-600" : "text-red-600"
                }`}
              >
                {isDown ? (
                  <TrendingDown size={14} />
                ) : (
                  <TrendingUp size={14} />
                )}
                <span>{Math.abs(changePercent).toFixed(1)}%</span>
              </div>
            </div>
          </div>
        </div>

        {/* Right: Category breakdown */}
        <div className="space-y-2">
          {errorData.byCategory.map((cat) => {
            const colors = severityColors[cat.severity];
            return (
              <div key={cat.category}>
                <div className="mb-0.5 flex items-center justify-between text-xs">
                  <span className="truncate text-gray-600">{cat.category}</span>
                  <div className="flex items-center gap-1.5">
                    <span
                      className={`rounded-full px-1.5 py-0.5 text-[10px] font-medium ${colors.badge}`}
                    >
                      {cat.severity}
                    </span>
                    <span className="font-medium text-gray-900">
                      {cat.count}
                    </span>
                  </div>
                </div>
                <div className="h-1.5 overflow-hidden rounded-full bg-gray-100">
                  <div
                    className={`h-full rounded-full ${colors.bar} transition-all`}
                    style={{
                      width: `${(cat.count / maxCount) * 100}%`,
                    }}
                  />
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Hourly chart */}
      <div>
        <p className="mb-1 text-xs text-gray-400">Hourly errors (24h)</p>
        <HourlyChart data={errorData.hourlyErrors} />
      </div>
    </div>
  );
}
