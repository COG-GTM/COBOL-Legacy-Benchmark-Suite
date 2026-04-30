"use client";

import useSWR from "swr";
import Link from "next/link";
import { swrFetcher } from "@/lib/api";
import type { PortfolioListResponse, TransactionListResponse } from "@/types";
import { StatCard } from "@/components/ui/StatCard";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { LoadingState } from "@/components/ui/LoadingState";
import { ErrorDisplay } from "@/components/ui/ErrorDisplay";
import { BatchRunPanel } from "@/components/batch/BatchRunPanel";

function formatCurrency(value: number): string {
  return new Intl.NumberFormat("en-US", { style: "currency", currency: "USD", maximumFractionDigits: 0 }).format(value);
}

export default function DashboardPage() {
  const { data: portfolioData, error: pError, isLoading: pLoading } = useSWR<PortfolioListResponse>(
    "/api/portfolios",
    swrFetcher,
    { refreshInterval: 30000 }
  );
  const { data: txData, error: tError, isLoading: tLoading } = useSWR<TransactionListResponse>(
    "/api/transactions",
    swrFetcher,
    { refreshInterval: 30000 }
  );

  if (pLoading || tLoading) return <LoadingState message="Loading dashboard..." />;
  if (pError) return <ErrorDisplay message={pError.message} />;
  if (tError) return <ErrorDisplay message={tError.message} />;

  const portfolios = portfolioData?.portfolios ?? [];
  const transactions = txData?.transactions ?? [];
  const totalAUM = portfolios.reduce((s, p) => s + p.totalValue, 0);
  const activeCount = portfolios.filter((p) => p.status === "A").length;
  const recentTx = transactions.slice(0, 8);

  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-bold text-gray-900">Dashboard</h2>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard label="Total Portfolios" value={String(portfolios.length)} />
        <StatCard label="Active Portfolios" value={String(activeCount)} />
        <StatCard label="Assets Under Management" value={formatCurrency(totalAUM)} />
        <StatCard label="Total Transactions" value={String(txData?.total ?? 0)} />
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <div className="rounded-lg border border-gray-200 bg-white p-5">
          <div className="mb-4 flex items-center justify-between">
            <h3 className="text-lg font-semibold text-gray-900">Recent Portfolios</h3>
            <Link href="/portfolios" className="text-sm text-blue-600 hover:underline">View all</Link>
          </div>
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead>
                <tr>
                  <th className="px-3 py-2 text-left text-xs font-medium uppercase text-gray-500">Account</th>
                  <th className="px-3 py-2 text-left text-xs font-medium uppercase text-gray-500">Client</th>
                  <th className="px-3 py-2 text-left text-xs font-medium uppercase text-gray-500">Status</th>
                  <th className="px-3 py-2 text-right text-xs font-medium uppercase text-gray-500">Value</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {portfolios.slice(0, 5).map((p) => (
                  <tr key={p.id} className="hover:bg-gray-50">
                    <td className="px-3 py-2 text-sm font-mono text-gray-900">
                      <Link href={`/portfolios/${p.id}`} className="text-blue-600 hover:underline">{p.accountNo}</Link>
                    </td>
                    <td className="px-3 py-2 text-sm text-gray-700">{p.clientName}</td>
                    <td className="px-3 py-2"><StatusBadge status={p.status} /></td>
                    <td className="px-3 py-2 text-right text-sm font-medium text-gray-900">{formatCurrency(p.totalValue)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        <div className="rounded-lg border border-gray-200 bg-white p-5">
          <div className="mb-4 flex items-center justify-between">
            <h3 className="text-lg font-semibold text-gray-900">Recent Transactions</h3>
            <Link href="/transactions" className="text-sm text-blue-600 hover:underline">View all</Link>
          </div>
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead>
                <tr>
                  <th className="px-3 py-2 text-left text-xs font-medium uppercase text-gray-500">Date</th>
                  <th className="px-3 py-2 text-left text-xs font-medium uppercase text-gray-500">Type</th>
                  <th className="px-3 py-2 text-left text-xs font-medium uppercase text-gray-500">Account</th>
                  <th className="px-3 py-2 text-right text-xs font-medium uppercase text-gray-500">Amount</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {recentTx.map((tx) => (
                  <tr key={tx.id} className="hover:bg-gray-50">
                    <td className="px-3 py-2 text-sm text-gray-500">{new Date(tx.createdAt).toLocaleDateString()}</td>
                    <td className="px-3 py-2"><StatusBadge status={tx.transactionType} /></td>
                    <td className="px-3 py-2 text-sm font-mono text-gray-700">{tx.portfolio?.accountNo ?? "—"}</td>
                    <td className="px-3 py-2 text-right text-sm font-medium text-gray-900">{formatCurrency(tx.amount)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <div className="rounded-lg border border-gray-200 bg-white p-5">
        <BatchRunPanel />
      </div>
    </div>
  );
}
