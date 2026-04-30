"use client";

import { useState } from "react";
import useSWR from "swr";
import { swrFetcher } from "@/lib/api";
import type { TransactionListResponse, TransactionType } from "@/types";
import { INVESTMENT_TYPE_LABELS } from "@/types";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { LoadingState } from "@/components/ui/LoadingState";
import { ErrorDisplay } from "@/components/ui/ErrorDisplay";
import { SubmitTransactionForm } from "@/components/transactions/SubmitTransactionForm";

function formatCurrency(value: number): string {
  return new Intl.NumberFormat("en-US", { style: "currency", currency: "USD", minimumFractionDigits: 2 }).format(value);
}

export default function TransactionsPage() {
  const [typeFilter, setTypeFilter] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [showSubmit, setShowSubmit] = useState(false);

  const params = new URLSearchParams();
  if (typeFilter) params.set("type", typeFilter);
  if (startDate) params.set("startDate", startDate);
  if (endDate) params.set("endDate", endDate);
  const qs = params.toString();

  const { data, error, isLoading, mutate } = useSWR<TransactionListResponse>(
    `/api/transactions${qs ? `?${qs}` : ""}`,
    swrFetcher,
    { refreshInterval: 15000 }
  );

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold text-gray-900">Transactions</h2>
        <button
          onClick={() => setShowSubmit(true)}
          className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
        >
          + New Transaction
        </button>
      </div>

      {showSubmit && (
        <div className="rounded-lg border border-gray-200 bg-white p-5">
          <SubmitTransactionForm
            onSuccess={() => { setShowSubmit(false); mutate(); }}
            onCancel={() => setShowSubmit(false)}
          />
        </div>
      )}

      <div className="flex flex-wrap gap-4">
        <select
          value={typeFilter}
          onChange={(e) => setTypeFilter(e.target.value)}
          className="rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
        >
          <option value="">All Types</option>
          <option value="BUY">Buy</option>
          <option value="SELL">Sell</option>
          <option value="TRANSFER">Transfer</option>
          <option value="FEE">Fee</option>
        </select>
        <div className="flex items-center gap-2">
          <label className="text-sm text-gray-500">From:</label>
          <input
            type="date"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            className="rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
        </div>
        <div className="flex items-center gap-2">
          <label className="text-sm text-gray-500">To:</label>
          <input
            type="date"
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
            className="rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
        </div>
        {(typeFilter || startDate || endDate) && (
          <button
            onClick={() => { setTypeFilter(""); setStartDate(""); setEndDate(""); }}
            className="text-sm text-blue-600 hover:underline"
          >
            Clear filters
          </button>
        )}
      </div>

      {isLoading && <LoadingState message="Loading transactions..." />}
      {error && <ErrorDisplay message={error.message} onRetry={() => mutate()} />}

      {data && (
        <>
          <p className="text-sm text-gray-500">{data.total} transaction{data.total !== 1 ? "s" : ""} found</p>
          <div className="overflow-hidden rounded-lg border border-gray-200 bg-white">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">Date</th>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">Account</th>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">Client</th>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">Type</th>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">Investment</th>
                  <th className="px-4 py-3 text-right text-xs font-medium uppercase text-gray-500">Units</th>
                  <th className="px-4 py-3 text-right text-xs font-medium uppercase text-gray-500">Price</th>
                  <th className="px-4 py-3 text-right text-xs font-medium uppercase text-gray-500">Amount</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {data.transactions.map((tx) => (
                  <tr key={tx.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3 text-sm text-gray-500">{new Date(tx.createdAt).toLocaleDateString()}</td>
                    <td className="px-4 py-3 text-sm font-mono text-gray-900">{tx.portfolio?.accountNo ?? "—"}</td>
                    <td className="px-4 py-3 text-sm text-gray-700">{tx.portfolio?.clientName ?? "—"}</td>
                    <td className="px-4 py-3"><StatusBadge status={tx.transactionType as TransactionType} /></td>
                    <td className="px-4 py-3 text-sm text-gray-500">{INVESTMENT_TYPE_LABELS[tx.investmentType as keyof typeof INVESTMENT_TYPE_LABELS] ?? tx.investmentType}</td>
                    <td className="px-4 py-3 text-right text-sm text-gray-900">{tx.units.toFixed(2)}</td>
                    <td className="px-4 py-3 text-right text-sm text-gray-500">{formatCurrency(tx.price)}</td>
                    <td className="px-4 py-3 text-right text-sm font-medium text-gray-900">{formatCurrency(tx.amount)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </div>
  );
}
