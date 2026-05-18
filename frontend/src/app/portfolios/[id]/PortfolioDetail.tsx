"use client";

import { useQuery } from "@tanstack/react-query";
import { getPortfolio } from "@/lib/api";
import { portfolioStatusLabel, clientTypeLabel } from "@/types/domain";
import ErrorBoundary from "@/components/ErrorBoundary";
import PositionInquiry from "@/components/PositionInquiry";
import TransactionHistory from "@/components/TransactionHistory";

function formatCurrency(value: number): string {
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    minimumFractionDigits: 2,
  }).format(value);
}

interface PortfolioDetailProps {
  portfolioId: string;
}

export default function PortfolioDetail({ portfolioId }: PortfolioDetailProps) {
  const { data: portfolio, isLoading, error } = useQuery({
    queryKey: ["portfolio", portfolioId],
    queryFn: () => getPortfolio(portfolioId),
    enabled: !!portfolioId,
  });

  if (isLoading) {
    return (
      <div className="space-y-4">
        <div className="h-24 animate-pulse rounded-lg bg-zinc-100 dark:bg-zinc-800" />
        <div className="h-64 animate-pulse rounded-lg bg-zinc-100 dark:bg-zinc-800" />
      </div>
    );
  }

  if (error || !portfolio) {
    return (
      <div className="rounded-lg border border-red-200 bg-red-50 p-4 dark:border-red-800 dark:bg-red-950">
        <p className="text-sm text-red-700 dark:text-red-300">
          {error ? "Failed to load portfolio." : "Portfolio not found."}
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      {/* Portfolio Summary */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <InfoCard label="Account No" value={portfolio.accountNo} />
        <InfoCard label="Client" value={portfolio.clientName} />
        <InfoCard label="Type" value={clientTypeLabel(portfolio.clientType)} />
        <InfoCard label="Status" value={portfolioStatusLabel(portfolio.status)} />
        <InfoCard label="Total Value" value={formatCurrency(portfolio.totalValue)} />
        <InfoCard label="Cash Balance" value={formatCurrency(portfolio.cashBalance)} />
        <InfoCard label="Created" value={portfolio.createDate} />
        <InfoCard label="Last Maintenance" value={portfolio.lastMaint} />
      </div>

      {/* Positions Section */}
      <ErrorBoundary>
        <PositionInquiry portfolioId={portfolioId} />
      </ErrorBoundary>

      {/* Transactions Section */}
      <ErrorBoundary>
        <TransactionHistory portfolioId={portfolioId} />
      </ErrorBoundary>
    </div>
  );
}

function InfoCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg border border-zinc-200 bg-white p-4 dark:border-zinc-700 dark:bg-zinc-800">
      <p className="text-xs font-medium uppercase tracking-wide text-zinc-500 dark:text-zinc-400">
        {label}
      </p>
      <p className="mt-1 text-sm font-semibold text-zinc-900 dark:text-zinc-100">
        {value}
      </p>
    </div>
  );
}
