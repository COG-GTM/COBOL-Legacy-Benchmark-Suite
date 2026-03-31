import type { ResourceMetric } from "../../types";

interface ResourceGaugesProps {
  metrics: ResourceMetric[];
}

function getColor(value: number, warning: number, critical: number) {
  if (value >= critical) return { stroke: "#ef4444", text: "text-red-500" };
  if (value >= warning) return { stroke: "#eab308", text: "text-yellow-500" };
  return { stroke: "#22c55e", text: "text-green-500" };
}

function Sparkline({ data, color }: { data: number[]; color: string }) {
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
      <polygon points={areaPoints} fill={color} opacity="0.15" />
      <polyline
        points={points}
        fill="none"
        stroke={color}
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function GaugeCircle({
  value,
  warning,
  critical,
}: {
  value: number;
  warning: number;
  critical: number;
}) {
  const { stroke, text } = getColor(value, warning, critical);
  const radius = 45;
  const circumference = 2 * Math.PI * radius;
  const offset = circumference - (value / 100) * circumference;

  return (
    <div className="relative mx-auto h-32 w-32">
      <svg
        viewBox="0 0 120 120"
        className="h-full w-full -rotate-90"
        aria-label={`${value}%`}
        role="img"
      >
        <circle
          cx="60"
          cy="60"
          r={radius}
          fill="none"
          stroke="#e5e7eb"
          strokeWidth="10"
        />
        <circle
          cx="60"
          cy="60"
          r={radius}
          fill="none"
          stroke={stroke}
          strokeWidth="10"
          strokeDasharray={circumference}
          strokeDashoffset={offset}
          strokeLinecap="round"
        />
      </svg>
      <div className="absolute inset-0 flex items-center justify-center">
        <span className={`text-2xl font-bold ${text}`}>{value}%</span>
      </div>
    </div>
  );
}

export default function ResourceGauges({ metrics }: ResourceGaugesProps) {
  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
      {metrics.map((metric) => {
        const { stroke } = getColor(
          metric.current,
          metric.threshold_warning,
          metric.threshold_critical
        );

        return (
          <div
            key={metric.name}
            className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm"
          >
            <h3 className="mb-3 text-center text-sm font-semibold text-gray-700">
              {metric.name}
            </h3>
            <GaugeCircle
              value={metric.current}
              warning={metric.threshold_warning}
              critical={metric.threshold_critical}
            />
            <div className="mt-3">
              <Sparkline data={metric.trend} color={stroke} />
            </div>
            <p className="mt-2 text-center text-xs text-gray-400">
              Warning: {metric.threshold_warning}% | Critical:{" "}
              {metric.threshold_critical}%
            </p>
          </div>
        );
      })}
    </div>
  );
}
