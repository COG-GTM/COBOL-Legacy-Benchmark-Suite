"use client";

import { useMemo } from "react";
import { createColumnHelper } from "@tanstack/react-table";
import type { Transaction } from "@/types/portfolio";
import { TRANSACTION_TYPE_LABELS } from "@/types/portfolio";
import DataTable from "@/components/common/DataTable";
import CurrencyDisplay from "@/components/common/CurrencyDisplay";
import StatusBadge from "@/components/common/StatusBadge";

const columnHelper = createColumnHelper<Transaction>();

function formatDate(dateStr: string): string {
  if (dateStr.length !== 8) return dateStr;
  return `${dateStr.slice(0, 4)}-${dateStr.slice(4, 6)}-${dateStr.slice(6, 8)}`;
}

interface TransactionTableProps {
  transactions: Transaction[];
}

export default function TransactionTable({
  transactions,
}: TransactionTableProps) {
  const columns = useMemo(
    () => [
      columnHelper.accessor("date", {
        header: "Date",
        cell: (info) => (
          <span className="font-mono text-xs">{formatDate(info.getValue())}</span>
        ),
      }),
      columnHelper.accessor("type", {
        header: "Type",
        cell: (info) => {
          const type = info.getValue();
          const label = TRANSACTION_TYPE_LABELS[type];
          const colorMap: Record<string, string> = {
            BU: "text-emerald-700 bg-emerald-50",
            SL: "text-red-700 bg-red-50",
            TR: "text-blue-700 bg-blue-50",
            FE: "text-gray-700 bg-gray-50",
          };
          return (
            <span
              className={`inline-flex rounded-md px-2 py-0.5 text-xs font-medium ${colorMap[type] ?? ""}`}
            >
              {label}
            </span>
          );
        },
      }),
      columnHelper.accessor("quantity", {
        header: "Units",
        cell: (info) => {
          const val = info.getValue();
          return (
            <span className="font-mono">
              {val === 0
                ? "-"
                : val.toLocaleString("en-US", {
                    minimumFractionDigits: 2,
                    maximumFractionDigits: 4,
                  })}
            </span>
          );
        },
      }),
      columnHelper.accessor("price", {
        header: "Price",
        cell: (info) => {
          const val = info.getValue();
          return val === 0 ? (
            <span className="text-gray-400">-</span>
          ) : (
            <CurrencyDisplay amount={val} />
          );
        },
      }),
      columnHelper.accessor("amount", {
        header: "Amount",
        cell: (info) => <CurrencyDisplay amount={info.getValue()} />,
      }),
      columnHelper.accessor("status", {
        header: "Status",
        cell: (info) => <StatusBadge code={info.getValue()} />,
      }),
    ],
    []
  );

  return <DataTable columns={columns} data={transactions} pageSize={10} />;
}
