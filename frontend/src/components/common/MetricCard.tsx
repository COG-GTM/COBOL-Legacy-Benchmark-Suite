import { TrendingUp, TrendingDown, Minus } from "lucide-react";
import type { MetricCardProps } from "../../types";

const trendIcons = {
  up: TrendingUp,
  down: TrendingDown,
  flat: Minus,
};

const trendColors = {
  up: "text-green-600",
  down: "text-red-600",
  flat: "text-gray-500",
};

export default function MetricCard({
  title,
  value,
  unit,
  trend,
  trendValue,
  color,
}: MetricCardProps) {
  const TrendIcon = trend ? trendIcons[trend] : null;
  const trendColor = trend ? trendColors[trend] : "";

  return (
    <div className="rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
      <p className="text-sm font-medium text-gray-500">{title}</p>
      <div className="mt-1 flex items-baseline gap-1">
        <span
          className="text-2xl font-semibold"
          style={color ? { color } : undefined}
        >
          {value}
        </span>
        {unit && <span className="text-sm text-gray-500">{unit}</span>}
      </div>
      {trend && trendValue && TrendIcon && (
        <div className={`mt-1 flex items-center gap-1 text-xs ${trendColor}`}>
          <TrendIcon size={14} />
          <span>{trendValue}</span>
        </div>
      )}
    </div>
  );
}
