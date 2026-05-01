"use client";

import { use, useMemo } from "react";
import Link from "next/link";
import { MOCK_PORTFOLIOS, MOCK_POSITIONS } from "@/data/mock";
import { CLIENT_TYPE_LABELS } from "@/types/portfolio";
import PositionTable from "@/components/portfolio/PositionTable";
import StatusBadge from "@/components/common/StatusBadge";
import CurrencyDisplay from "@/components/common/CurrencyDisplay";
import ErrorDisplay from "@/components/common/ErrorDisplay";
import { ArrowLeft } from "lucide-react";

export default function PortfolioDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const portfolio = MOCK_PORTFOLIOS.find((p) => p.id === id);

  const positions = useMemo(
    () => MOCK_POSITIONS.filter((pos) => pos.portfolioId === id),
    [id]
  );

  if (!portfolio) {
    return (
      <div className="space-y-4">
        <Link
          href="/portfolios"
          className="inline-flex items-center gap-1 text-sm text-indigo-600 hover:text-indigo-800"
        >
          <ArrowLeft className="h-4 w-4" />
          Back to Portfolios
        </Link>
        <ErrorDisplay
          error={{
            program: "INQONLN",
            paragraph: "READ-PORTFOLIO",
            severity: "W",
            message: `Portfolio not found: ${id}`,
            action: "R",
            traceId: "TRC-0000000001",
            timestamp: "2024-03-20T10:30:00Z",
          }}
        />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Link
          href="/portfolios"
          className="inline-flex items-center gap-1 text-sm text-indigo-600 hover:text-indigo-800"
        >
          <ArrowLeft className="h-4 w-4" />
          Back
        </Link>
      </div>

      <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
        <div className="flex items-start justify-between">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">
              {portfolio.clientName}
            </h1>
            <p className="mt-1 text-sm text-gray-500">
              {CLIENT_TYPE_LABELS[portfolio.clientType]} &middot; Account:{" "}
              <span className="font-mono">{portfolio.accountNo}</span>
            </p>
          </div>
          <StatusBadge code={portfolio.status} />
        </div>

        <div className="mt-6 grid grid-cols-2 gap-6 sm:grid-cols-4">
          <div>
            <p className="text-xs font-medium text-gray-500">Portfolio ID</p>
            <p className="mt-1 font-mono text-sm text-gray-900">
              {portfolio.id}
            </p>
          </div>
          <div>
            <p className="text-xs font-medium text-gray-500">Total Value</p>
            <CurrencyDisplay
              amount={portfolio.totalValue}
              className="mt-1 text-lg font-bold text-gray-900"
            />
          </div>
          <div>
            <p className="text-xs font-medium text-gray-500">Cash Balance</p>
            <CurrencyDisplay
              amount={portfolio.cashBalance}
              className="mt-1 text-lg font-bold text-gray-900"
            />
          </div>
          <div>
            <p className="text-xs font-medium text-gray-500">Last Updated</p>
            <p className="mt-1 font-mono text-sm text-gray-900">
              {portfolio.lastMaint}
            </p>
          </div>
        </div>
      </div>

      <div>
        <h2 className="mb-4 text-lg font-semibold text-gray-900">
          Position Details
        </h2>
        {positions.length === 0 ? (
          <p className="text-sm text-gray-500">
            No positions found for this portfolio.
          </p>
        ) : (
          <PositionTable positions={positions} />
        )}
      </div>
    </div>
  );
}
