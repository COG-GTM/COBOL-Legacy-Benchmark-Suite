import type { ResourceMetric } from '@/types';
import { cn } from '@/lib/utils';

interface ResourceGaugesProps {
  metrics: ResourceMetric[];
}

function getColor(value: number, warning: number, critical: number): 'green' | 'yellow' | 'red' {
  if (value >= critical) return 'red';
  if (value >= warning) return 'yellow';
  return 'green';
}

const colorMap = {
  green: { stroke: '#22c55e', text: 'text-green-500', bg: 'text-green-600' },
  yellow: { stroke: '#eab308', text: 'text-yellow-500', bg: 'text-yellow-600' },
  red: { stroke: '#ef4444', text: 'text-red-500', bg: 'text-red-600' },
};

function CircularGauge({ value, warning, critical }: { value: number; warning: number; critical: number }) {
  const color = getColor(value, warning, critical);
  const { stroke } = colorMap[color];
  const radius = 54;
  const circumference = 2 * Math.PI * radius;
  const offset = circumference - (value / 100) * circumference;

  return (
    <div className="relative inline-flex items-center justify-center">
      <svg width="136" height="136" viewBox="0 0 136 136" className="-rotate-90">
        <circle
          cx="68"
          cy="68"
          r={radius}
          fill="none"
          stroke="#e5e7eb"
          strokeWidth="10"
        />
        <circle
          cx="68"
          cy="68"
          r={radius}
          fill="none"
          stroke={stroke}
          strokeWidth="10"
          strokeDasharray={circumference}
          strokeDashoffset={offset}
          strokeLinecap="round"
          className="transition-all duration-500"
        />
      </svg>
      <span className={cn('absolute text-2xl font-bold', colorMap[color].bg)}>
        {value}%
      </span>
    </div>
  );
}

function Sparkline({ data, color }: { data: number[]; color: 'green' | 'yellow' | 'red' }) {
  const max = Math.max(...data);
  const min = Math.min(...data);
  const range = max - min || 1;
  const height = 40;
  const width = 200;
  const step = width / (data.length - 1);

  const points = data
    .map((v, i) => `${i * step},${height - ((v - min) / range) * height}`)
    .join(' ');

  const areaPoints = `0,${height} ${points} ${width},${height}`;

  return (
    <svg
      viewBox={`0 0 ${width} ${height}`}
      className="w-full h-10"
      preserveAspectRatio="none"
      aria-hidden="true"
    >
      <polygon
        points={areaPoints}
        fill={colorMap[color].stroke}
        fillOpacity="0.1"
      />
      <polyline
        points={points}
        fill="none"
        stroke={colorMap[color].stroke}
        strokeWidth="1.5"
      />
    </svg>
  );
}

export default function ResourceGauges({ metrics }: ResourceGaugesProps) {
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
      {metrics.map((metric) => {
        const color = getColor(metric.current, metric.threshold_warning, metric.threshold_critical);
        return (
          <div
            key={metric.name}
            className="bg-white rounded-lg shadow-sm border border-gray-200 p-6 flex flex-col items-center"
            aria-label={`${metric.name}: ${metric.current}${metric.unit}`}
          >
            <h3 className="text-sm font-medium text-gray-600 mb-3">{metric.name}</h3>
            <CircularGauge
              value={metric.current}
              warning={metric.threshold_warning}
              critical={metric.threshold_critical}
            />
            <div className="w-full mt-4">
              <Sparkline data={metric.trend} color={color} />
            </div>
            <p className="text-xs text-gray-400 mt-2">
              Warning: {metric.threshold_warning}% | Critical: {metric.threshold_critical}%
            </p>
          </div>
        );
      })}
    </div>
  );
}
