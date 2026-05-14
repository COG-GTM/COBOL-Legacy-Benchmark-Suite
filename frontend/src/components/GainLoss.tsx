import { TrendingUp, TrendingDown, Minus } from "lucide-react";

interface Props {
  value: number;
  percent?: number;
  size?: "sm" | "md";
}

export default function GainLoss({ value, percent, size = "md" }: Props) {
  const isPositive = value > 0;
  const isZero = value === 0;
  const color = isZero ? "text-[#94A3B8]" : isPositive ? "text-[#4ADE80]" : "text-[#F87171]";
  const Icon = isZero ? Minus : isPositive ? TrendingUp : TrendingDown;
  const textSize = size === "sm" ? "text-xs" : "text-sm";

  const fmt = new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    minimumFractionDigits: 2,
  });

  return (
    <span className={`inline-flex items-center gap-1 ${color} ${textSize}`}>
      <Icon size={size === "sm" ? 12 : 14} />
      {fmt.format(Math.abs(value))}
      {percent !== undefined && (
        <span className="opacity-75">({percent > 0 ? "+" : ""}{percent.toFixed(2)}%)</span>
      )}
    </span>
  );
}
