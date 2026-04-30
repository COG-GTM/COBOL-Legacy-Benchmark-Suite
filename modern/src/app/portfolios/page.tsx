"use client";

import { useState } from "react";
import useSWR from "swr";
import Link from "next/link";
import { swrFetcher, deletePortfolio } from "@/lib/api";
import type { PortfolioListResponse, PortfolioStatus } from "@/types";
import { CLIENT_TYPE_LABELS } from "@/types";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { LoadingState } from "@/components/ui/LoadingState";
import { ErrorDisplay } from "@/components/ui/ErrorDisplay";
import { CreatePortfolioForm } from "@/components/portfolio/CreatePortfolioForm";
import toast from "react-hot-toast";

function formatCurrency(value: number): string {
  return new Intl.NumberFormat("en-US", { style: "currency", currency: "USD", maximumFractionDigits: 0 }).format(value);
}

export default function PortfoliosPage() {
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [showCreate, setShowCreate] = useState(false);

  const params = new URLSearchParams();
  if (search) params.set("search", search);
  if (statusFilter) params.set("status", statusFilter);
  const qs = params.toString();

  const { data, error, isLoading, mutate } = useSWR<PortfolioListResponse>(
    `/api/portfolios${qs ? `?${qs}` : ""}`,
    swrFetcher,
    { refreshInterval: 15000 }
  );

  async function handleDelete(id: string, accountNo: string) {
    if (!confirm(`Delete portfolio ${accountNo}? This cannot be undone.`)) return;
    try {
      await deletePortfolio(id);
      toast.success(`Portfolio ${accountNo} deleted`);
      mutate();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Delete failed");
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold text-gray-900">Portfolios</h2>
        <button
          onClick={() => setShowCreate(true)}
          className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
        >
          + New Portfolio
        </button>
      </div>

      {showCreate && (
        <div className="rounded-lg border border-gray-200 bg-white p-5">
          <CreatePortfolioForm
            onSuccess={() => { setShowCreate(false); mutate(); }}
            onCancel={() => setShowCreate(false)}
          />
        </div>
      )}

      <div className="flex gap-4">
        <input
          type="text"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Search by name or account..."
          className="w-64 rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
        />
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          className="rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
        >
          <option value="">All Statuses</option>
          <option value="A">Active</option>
          <option value="S">Suspended</option>
          <option value="C">Closed</option>
        </select>
      </div>

      {isLoading && <LoadingState message="Loading portfolios..." />}
      {error && <ErrorDisplay message={error.message} onRetry={() => mutate()} />}

      {data && (
        <>
          <p className="text-sm text-gray-500">{data.total} portfolio{data.total !== 1 ? "s" : ""} found</p>
          <div className="overflow-hidden rounded-lg border border-gray-200 bg-white">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">Account</th>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">Client Name</th>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">Type</th>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">Status</th>
                  <th className="px-4 py-3 text-right text-xs font-medium uppercase text-gray-500">Total Value</th>
                  <th className="px-4 py-3 text-right text-xs font-medium uppercase text-gray-500">Cash</th>
                  <th className="px-4 py-3 text-right text-xs font-medium uppercase text-gray-500">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {data.portfolios.map((p) => (
                  <tr key={p.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3 text-sm font-mono">
                      <Link href={`/portfolios/${p.id}`} className="text-blue-600 hover:underline">{p.accountNo}</Link>
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-900">{p.clientName}</td>
                    <td className="px-4 py-3 text-sm text-gray-500">{CLIENT_TYPE_LABELS[p.clientType as keyof typeof CLIENT_TYPE_LABELS] ?? p.clientType}</td>
                    <td className="px-4 py-3"><StatusBadge status={p.status as PortfolioStatus} /></td>
                    <td className="px-4 py-3 text-right text-sm font-medium text-gray-900">{formatCurrency(p.totalValue)}</td>
                    <td className="px-4 py-3 text-right text-sm text-gray-500">{formatCurrency(p.cashBalance)}</td>
                    <td className="px-4 py-3 text-right">
                      <button
                        onClick={() => handleDelete(p.id, p.accountNo)}
                        className="text-sm text-red-600 hover:text-red-800"
                      >
                        Delete
                      </button>
                    </td>
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
