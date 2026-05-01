"use client";

import { useMemo } from "react";
import { createColumnHelper } from "@tanstack/react-table";
import type { Position } from "@/types/portfolio";
import DataTable from "@/components/common/DataTable";
import CurrencyDisplay from "@/components/common/CurrencyDisplay";
import StatusBadge from "@/components/common/StatusBadge";

const columnHelper = createColumnHelper<Position>();

interface PositionTableProps {
  positions: Position[];
}

export default function PositionTable({ positions }: PositionTableProps) {
  const columns = useMemo(
    () => [
      columnHelper.accessor("investmentId", {
        header: "Fund ID",
        cell: (info) => (
          <span className="font-mono text-xs">{info.getValue()}</span>
        ),
      }),
      columnHelper.accessor("fundName", {
        header: "Fund Name",
      }),
      columnHelper.accessor("quantity", {
        header: "Units",
        cell: (info) => (
          <span className="font-mono">
            {info.getValue().toLocaleString("en-US", {
              minimumFractionDigits: 2,
              maximumFractionDigits: 4,
            })}
          </span>
        ),
      }),
      columnHelper.accessor("costBasis", {
        header: "Cost Basis",
        cell: (info) => <CurrencyDisplay amount={info.getValue()} />,
      }),
      columnHelper.accessor("marketValue", {
        header: "Market Value",
        cell: (info) => <CurrencyDisplay amount={info.getValue()} />,
      }),
      columnHelper.display({
        id: "gainLoss",
        header: "G/L",
        cell: (info) => {
          const row = info.row.original;
          const gl = row.marketValue - row.costBasis;
          return <CurrencyDisplay amount={gl} />;
        },
      }),
      columnHelper.accessor("status", {
        header: "Status",
        cell: (info) => <StatusBadge code={info.getValue()} />,
      }),
    ],
    []
  );

  return <DataTable columns={columns} data={positions} pageSize={10} />;
}
