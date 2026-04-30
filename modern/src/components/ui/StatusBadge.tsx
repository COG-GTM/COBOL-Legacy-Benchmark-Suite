"use client";

import type { PortfolioStatus, TransactionType, BatchStatus } from "@/types";

const STATUS_COLORS: Record<string, string> = {
  A: "bg-green-100 text-green-800",
  C: "bg-gray-100 text-gray-800",
  S: "bg-yellow-100 text-yellow-800",
  BUY: "bg-green-100 text-green-800",
  SELL: "bg-red-100 text-red-800",
  TRANSFER: "bg-blue-100 text-blue-800",
  FEE: "bg-orange-100 text-orange-800",
  PENDING: "bg-yellow-100 text-yellow-800",
  RUNNING: "bg-blue-100 text-blue-800",
  COMPLETED: "bg-green-100 text-green-800",
  FAILED: "bg-red-100 text-red-800",
  SUCC: "bg-green-100 text-green-800",
  FAIL: "bg-red-100 text-red-800",
  WARN: "bg-yellow-100 text-yellow-800",
};

const STATUS_LABELS: Record<string, string> = {
  A: "Active", C: "Closed", S: "Suspended",
  BUY: "Buy", SELL: "Sell", TRANSFER: "Transfer", FEE: "Fee",
  PENDING: "Pending", RUNNING: "Running", COMPLETED: "Completed", FAILED: "Failed",
  SUCC: "Success", FAIL: "Failed", WARN: "Warning",
};

export function StatusBadge({ status }: { status: PortfolioStatus | TransactionType | BatchStatus | string }) {
  const colors = STATUS_COLORS[status] ?? "bg-gray-100 text-gray-800";
  const label = STATUS_LABELS[status] ?? status;
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${colors}`}>
      {label}
    </span>
  );
}
