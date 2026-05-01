import type { PortfolioStatus, PositionStatus, TransactionStatus } from "@/types/portfolio";
import {
  PORTFOLIO_STATUS_LABELS,
  POSITION_STATUS_LABELS,
  TRANSACTION_STATUS_LABELS,
} from "@/types/portfolio";

type StatusCode = PortfolioStatus | PositionStatus | TransactionStatus;

const STATUS_COLORS: Record<string, string> = {
  Active: "bg-emerald-100 text-emerald-800",
  Closed: "bg-gray-100 text-gray-800",
  Suspended: "bg-amber-100 text-amber-800",
  Pending: "bg-blue-100 text-blue-800",
  Done: "bg-emerald-100 text-emerald-800",
  Failed: "bg-red-100 text-red-800",
  Reversed: "bg-purple-100 text-purple-800",
};

const ALL_LABELS: Record<string, string> = {
  ...PORTFOLIO_STATUS_LABELS,
  ...POSITION_STATUS_LABELS,
  ...TRANSACTION_STATUS_LABELS,
  D: "Done",
  F: "Failed",
  R: "Reversed",
};

interface StatusBadgeProps {
  code: StatusCode;
}

export default function StatusBadge({ code }: StatusBadgeProps) {
  const label = ALL_LABELS[code] ?? code;
  const colors = STATUS_COLORS[label] ?? "bg-gray-100 text-gray-800";

  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${colors}`}
    >
      {label}
    </span>
  );
}
