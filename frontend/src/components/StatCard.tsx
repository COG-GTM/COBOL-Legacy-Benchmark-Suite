import type { ReactNode } from "react";

interface Props {
  label: string;
  value: string;
  subValue?: string;
  icon: ReactNode;
  trend?: "up" | "down" | "neutral";
}

export default function StatCard({ label, value, subValue, icon, trend }: Props) {
  const trendColor =
    trend === "up"
      ? "text-[#4ADE80]"
      : trend === "down"
        ? "text-[#F87171]"
        : "text-[#94A3B8]";

  return (
    <div className="bg-[#1E293B] rounded-xl p-5 border border-white/5">
      <div className="flex items-center justify-between mb-3">
        <span className="text-sm text-[#94A3B8]">{label}</span>
        <div className="w-8 h-8 rounded-lg bg-[#22D3EE]/10 flex items-center justify-center text-[#22D3EE]">
          {icon}
        </div>
      </div>
      <p className="text-2xl font-semibold text-white">{value}</p>
      {subValue && (
        <p className={`text-sm mt-1 ${trendColor}`}>{subValue}</p>
      )}
    </div>
  );
}
