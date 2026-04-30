"use client";

import { use } from "react";
import useSWR from "swr";
import Link from "next/link";
import { swrFetcher } from "@/lib/api";
import type { PortfolioDetail, TransactionListResponse } from "@/types";
import { CLIENT_TYPE_LABELS, PORTFOLIO_STATUS_LABELS } from "@/types";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { LoadingState } from "@/components/ui/LoadingState";
import { ErrorDisplay } from "@/components/ui/ErrorDisplay";
import { StatCard } from "@/components/ui/StatCard";

function formatCurrency(value: number): string {
  return new Intl.NumberFormat("en-US", { style: "currency", currency: "USD", minimumFractionDigits: 2 }).format(value);
}

export default function PortfolioDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);

  const { data: portfolio, error, isLoading } = useSWR<PortfolioDetail>(
    `/api/portfolios/${id}`,
    swrFetcher,
    { refreshInterval: 15000 }
  );

  const { data: txData } = useSWR<TransactionListResponse>(
    portfolio ? `/api/transactions?portfolioId=${id}` : null,
    swrFetcher
  );

  if (isLoading) return <LoadingState message="Loading portfolio..." />;
  if (error) return <ErrorDisplay message={error.message} />;
  if (!portfolio) return <ErrorDisplay message="Portfolio not found" />;

  const positions = portfolio.positions ?? [];
  const totalCostBasis = positions.reduce((s, p) => s + p.costBasis, 0);
  const totalMarketValue = positions.reduce((s, p) => s + p.marketValue, 0);
  const totalGainLoss = totalMarketValue - totalCostBasis;
  const gainLossPct = totalCostBasis > 0 ? (totalGainLoss / totalCostBasis) * 100 : 0;

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <Link href="/portfolios" className="text-sm text-blue-600 hover:underline">&larr; Portfolios</Link>
      </div>

      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">{portfolio.clientName}</h2>
          <p className="mt-1 text-sm text-gray-500">
            Account: <span className="font-mono">{portfolio.accountNo}</span>
            {" | "}
            {CLIENT_TYPE_LABELS[portfolio.clientType as keyof typeof CLIENT_TYPE_LABELS]}
            {" | "}
            {PORTFOLIO_STATUS_LABELS[portfolio.status as keyof typeof PORTFOLIO_STATUS_LABELS]}
          </p>
        </div>
        <StatusBadge status={portfolio.status} />
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard label="Total Value" value={formatCurrency(portfolio.totalValue)} />
        <StatCard label="Cash Balance" value={formatCurrency(portfolio.cashBalance)} />
        <StatCard label="Market Value" value={formatCurrency(totalMarketValue)} />
        <StatCard
          label="Gain/Loss"
          value={formatCurrency(totalGainLoss)}
          trend={{ value: Math.round(gainLossPct * 100) / 100, label: "overall" }}
        />
      </div>

      <div className="rounded-lg border border-gray-200 bg-white p-5">
        <h3 className="mb-4 text-lg font-semibold text-gray-900">Positions ({positions.length})</h3>
        {positions.length === 0 ? (
          <p className="text-sm text-gray-500">No positions found for this portfolio.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">Fund ID</th>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">Fund Name</th>
                  <th className="px-4 py-3 text-right text-xs font-medium uppercase text-gray-500">Units</th>
                  <th className="px-4 py-3 text-right text-xs font-medium uppercase text-gray-500">Cost Basis</th>
                  <th className="px-4 py-3 text-right text-xs font-medium uppercase text-gray-500">Market Value</th>
                  <th className="px-4 py-3 text-right text-xs font-medium uppercase text-gray-500">Gain/Loss</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {positions.map((pos) => {
                  const gl = pos.marketValue - pos.costBasis;
                  return (
                    <tr key={pos.id} className="hover:bg-gray-50">
                      <td className="px-4 py-3 text-sm font-mono font-medium text-gray-900">{pos.fundId}</td>
                      <td className="px-4 py-3 text-sm text-gray-700">{pos.fundName}</td>
                      <td className="px-4 py-3 text-right text-sm text-gray-900">{pos.units.toLocaleString()}</td>
                      <td className="px-4 py-3 text-right text-sm text-gray-500">{formatCurrency(pos.costBasis)}</td>
                      <td className="px-4 py-3 text-right text-sm font-medium text-gray-900">{formatCurrency(pos.marketValue)}</td>
                      <td className={`px-4 py-3 text-right text-sm font-medium ${gl >= 0 ? "text-green-600" : "text-red-600"}`}>
                        {gl >= 0 ? "+" : ""}{formatCurrency(gl)}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
              <tfoot className="bg-gray-50">
                <tr className="font-medium">
                  <td className="px-4 py-3 text-sm text-gray-900" colSpan={3}>Total</td>
                  <td className="px-4 py-3 text-right text-sm text-gray-900">{formatCurrency(totalCostBasis)}</td>
                  <td className="px-4 py-3 text-right text-sm text-gray-900">{formatCurrency(totalMarketValue)}</td>
                  <td className={`px-4 py-3 text-right text-sm ${totalGainLoss >= 0 ? "text-green-600" : "text-red-600"}`}>
                    {totalGainLoss >= 0 ? "+" : ""}{formatCurrency(totalGainLoss)} ({gainLossPct.toFixed(2)}%)
                  </td>
                </tr>
              </tfoot>
            </table>
          </div>
        )}
      </div>

      {txData && txData.transactions.length > 0 && (
        <div className="rounded-lg border border-gray-200 bg-white p-5">
          <h3 className="mb-4 text-lg font-semibold text-gray-900">
            Transaction History ({txData.total})
          </h3>
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">Date</th>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">Type</th>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">Investment</th>
                  <th className="px-4 py-3 text-right text-xs font-medium uppercase text-gray-500">Units</th>
                  <th className="px-4 py-3 text-right text-xs font-medium uppercase text-gray-500">Price</th>
                  <th className="px-4 py-3 text-right text-xs font-medium uppercase text-gray-500">Amount</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {txData.transactions.slice(0, 20).map((tx) => (
                  <tr key={tx.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3 text-sm text-gray-500">{new Date(tx.createdAt).toLocaleDateString()}</td>
                    <td className="px-4 py-3"><StatusBadge status={tx.transactionType} /></td>
                    <td className="px-4 py-3 text-sm text-gray-700">{tx.investmentType}</td>
                    <td className="px-4 py-3 text-right text-sm text-gray-900">{tx.units.toFixed(2)}</td>
                    <td className="px-4 py-3 text-right text-sm text-gray-500">{formatCurrency(tx.price)}</td>
                    <td className="px-4 py-3 text-right text-sm font-medium text-gray-900">{formatCurrency(tx.amount)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
