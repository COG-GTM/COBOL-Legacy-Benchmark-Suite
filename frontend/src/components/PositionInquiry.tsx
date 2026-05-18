"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { fetchPositions, type PaginatedResponse } from "@/lib/api";
import type { Position } from "@/types/domain";

function formatCurrency(value: number): string {
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    minimumFractionDigits: 2,
  }).format(value);
}

function formatQuantity(value: number): string {
  return new Intl.NumberFormat("en-US", {
    minimumFractionDigits: 0,
    maximumFractionDigits: 4,
  }).format(value);
}

const STATUS_LABELS: Record<string, string> = {
  A: "Active",
  C: "Closed",
  P: "Pending",
};

interface PositionInquiryProps {
  portfolioId: string;
}

export default function PositionInquiry({ portfolioId }: PositionInquiryProps) {
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const pageSize = 20;

  const { data, isLoading, error } = useQuery<PaginatedResponse<Position>>({
    queryKey: ["positions", portfolioId, page],
    queryFn: () => fetchPositions(portfolioId, { limit: pageSize, offset: page * pageSize }),
    enabled: !!portfolioId,
  });

  const positions = data?.data ?? [];
  const total = data?.total ?? 0;
  const totalPages = Math.ceil(total / pageSize);

  const filtered = search
    ? positions.filter(
        (p) =>
          p.investmentId.toLowerCase().includes(search.toLowerCase()) ||
          p.currency.toLowerCase().includes(search.toLowerCase()),
      )
    : positions;

  return (
    <section className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold text-zinc-900 dark:text-zinc-100">
          Position Inquiry
        </h2>
        <span className="text-sm text-zinc-500 dark:text-zinc-400">
          {total} position{total !== 1 ? "s" : ""}
        </span>
      </div>

      {/* Search — mirrors ACCTIN field from POSMAP */}
      <div className="flex items-center gap-3">
        <label
          htmlFor="position-search"
          className="text-sm font-medium text-zinc-700 dark:text-zinc-300"
        >
          Fund ID:
        </label>
        <input
          id="position-search"
          type="text"
          value={search}
          onChange={(e) => { setSearch(e.target.value); setPage(0); }}
          placeholder="Search by investment ID..."
          className="w-64 rounded-md border border-zinc-300 bg-white px-3 py-2 text-sm text-zinc-900 placeholder-zinc-400 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500 dark:border-zinc-600 dark:bg-zinc-800 dark:text-zinc-100 dark:placeholder-zinc-500"
        />
      </div>

      {/* Data Table — matches POSMAP fields: Fund ID, Fund Name, Units, Cost Basis, Market Value */}
      <div className="overflow-hidden rounded-lg border border-zinc-200 dark:border-zinc-700">
        <table className="min-w-full text-left text-sm">
          <thead className="bg-zinc-100 dark:bg-zinc-800">
            <tr>
              <th className="px-4 py-3 font-medium text-zinc-700 dark:text-zinc-300">
                Fund ID
              </th>
              <th className="px-4 py-3 font-medium text-zinc-700 dark:text-zinc-300">
                Units
              </th>
              <th className="px-4 py-3 font-medium text-zinc-700 dark:text-zinc-300">
                Cost Basis
              </th>
              <th className="px-4 py-3 font-medium text-zinc-700 dark:text-zinc-300">
                Market Value
              </th>
              <th className="px-4 py-3 font-medium text-zinc-700 dark:text-zinc-300">
                Gain/Loss
              </th>
              <th className="px-4 py-3 font-medium text-zinc-700 dark:text-zinc-300">
                Status
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-zinc-100 dark:divide-zinc-700">
            {isLoading ? (
              <tr>
                <td
                  colSpan={6}
                  className="px-4 py-8 text-center text-zinc-400 dark:text-zinc-500"
                >
                  Loading positions...
                </td>
              </tr>
            ) : error ? (
              <tr>
                <td
                  colSpan={6}
                  className="px-4 py-8 text-center text-red-500"
                >
                  Failed to load positions.
                </td>
              </tr>
            ) : filtered.length === 0 ? (
              <tr>
                <td
                  colSpan={6}
                  className="px-4 py-8 text-center text-zinc-400 dark:text-zinc-500"
                >
                  {search ? "No positions match your search." : "No positions found."}
                </td>
              </tr>
            ) : (
              filtered.map((pos) => {
                const gainLoss = pos.marketValue - pos.costBasis;
                const gainClass =
                  gainLoss >= 0
                    ? "text-green-600 dark:text-green-400"
                    : "text-red-600 dark:text-red-400";

                return (
                  <tr
                    key={`${pos.portfolioId}-${pos.investmentId}`}
                    className="hover:bg-zinc-50 dark:hover:bg-zinc-750"
                  >
                    <td className="px-4 py-3 font-mono text-zinc-900 dark:text-zinc-100">
                      {pos.investmentId}
                    </td>
                    <td className="px-4 py-3 font-mono text-zinc-900 dark:text-zinc-100">
                      {formatQuantity(pos.quantity)}
                    </td>
                    <td className="px-4 py-3 font-mono text-zinc-700 dark:text-zinc-300">
                      {formatCurrency(pos.costBasis)}
                    </td>
                    <td className="px-4 py-3 font-mono text-zinc-900 dark:text-zinc-100">
                      {formatCurrency(pos.marketValue)}
                    </td>
                    <td className={`px-4 py-3 font-mono ${gainClass}`}>
                      {gainLoss >= 0 ? "+" : ""}
                      {formatCurrency(gainLoss)}
                    </td>
                    <td className="px-4 py-3">
                      <span className="inline-flex rounded-full bg-zinc-100 px-2 py-0.5 text-xs font-medium text-zinc-700 dark:bg-zinc-700 dark:text-zinc-300">
                        {STATUS_LABELS[pos.status] ?? pos.status}
                      </span>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination — mirrors PF7=Previous PF8=Next from POSMAP */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between text-sm">
          <button
            type="button"
            disabled={page === 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            className="rounded-md border border-zinc-300 px-3 py-1.5 text-zinc-700 hover:bg-zinc-100 disabled:cursor-not-allowed disabled:opacity-50 dark:border-zinc-600 dark:text-zinc-300 dark:hover:bg-zinc-700"
          >
            Previous
          </button>
          <span className="text-zinc-500 dark:text-zinc-400">
            Page {page + 1} of {totalPages}
          </span>
          <button
            type="button"
            disabled={page >= totalPages - 1}
            onClick={() => setPage((p) => p + 1)}
            className="rounded-md border border-zinc-300 px-3 py-1.5 text-zinc-700 hover:bg-zinc-100 disabled:cursor-not-allowed disabled:opacity-50 dark:border-zinc-600 dark:text-zinc-300 dark:hover:bg-zinc-700"
          >
            Next
          </button>
        </div>
      )}
    </section>
  );
}
