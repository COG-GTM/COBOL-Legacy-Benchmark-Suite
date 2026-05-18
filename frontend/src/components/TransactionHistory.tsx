"use client";

import { useState, useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { fetchTransactions, type PaginatedResponse } from "@/lib/api";
import type { Transaction, TransactionType, TransactionStatus } from "@/types/domain";
import { transactionTypeLabel, transactionStatusLabel } from "@/types/domain";

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

type SortField = "date" | "type" | "amount" | "quantity" | "price";
type SortDir = "asc" | "desc";

interface TransactionHistoryProps {
  portfolioId: string;
}

export default function TransactionHistory({ portfolioId }: TransactionHistoryProps) {
  const [page, setPage] = useState(0);
  const [typeFilter, setTypeFilter] = useState<TransactionType | "">("");
  const [statusFilter, setStatusFilter] = useState<TransactionStatus | "">("");
  const [sortField, setSortField] = useState<SortField>("date");
  const [sortDir, setSortDir] = useState<SortDir>("desc");
  const pageSize = 20;

  const { data, isLoading, error } = useQuery<PaginatedResponse<Transaction>>({
    queryKey: ["transactions", portfolioId, page],
    queryFn: () =>
      fetchTransactions(portfolioId, { limit: pageSize, offset: page * pageSize }),
    enabled: !!portfolioId,
  });

  const hasClientFilter = typeFilter !== "" || statusFilter !== "";

  const filteredAndSorted = useMemo(() => {
    let result = [...(data?.data ?? [])];

    if (typeFilter) {
      result = result.filter((t) => t.type === typeFilter);
    }
    if (statusFilter) {
      result = result.filter((t) => t.status === statusFilter);
    }

    result.sort((a, b) => {
      let cmp = 0;
      switch (sortField) {
        case "date":
          cmp = (a.date ?? "").localeCompare(b.date ?? "");
          break;
        case "type":
          cmp = a.type.localeCompare(b.type);
          break;
        case "amount":
          cmp = a.amount - b.amount;
          break;
        case "quantity":
          cmp = a.quantity - b.quantity;
          break;
        case "price":
          cmp = a.price - b.price;
          break;
      }
      return sortDir === "asc" ? cmp : -cmp;
    });

    return result;
  }, [data?.data, typeFilter, statusFilter, sortField, sortDir]);

  const serverTotal = data?.total ?? 0;
  const displayedCount = hasClientFilter ? filteredAndSorted.length : serverTotal;
  const totalPages = Math.ceil(serverTotal / pageSize);

  function handleSort(field: SortField) {
    if (sortField === field) {
      setSortDir((d) => (d === "asc" ? "desc" : "asc"));
    } else {
      setSortField(field);
      setSortDir("desc");
    }
  }

  function sortIndicator(field: SortField) {
    if (sortField !== field) return null;
    return sortDir === "asc" ? " \u2191" : " \u2193";
  }

  return (
    <section className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold text-zinc-900 dark:text-zinc-100">
          Transaction History
        </h2>
        <span className="text-sm text-zinc-500 dark:text-zinc-400">
          {displayedCount} transaction{displayedCount !== 1 ? "s" : ""}
          {hasClientFilter ? ` of ${serverTotal} (filtered)` : ""}
        </span>
      </div>

      {/* Filters — mirror HISMAP search */}
      <div className="flex flex-wrap items-center gap-3">
        <div className="flex items-center gap-2">
          <label
            htmlFor="txn-type-filter"
            className="text-sm font-medium text-zinc-700 dark:text-zinc-300"
          >
            Type:
          </label>
          <select
            id="txn-type-filter"
            value={typeFilter}
            onChange={(e) => { setTypeFilter(e.target.value as TransactionType | ""); setPage(0); }}
            className="rounded-md border border-zinc-300 bg-white px-3 py-1.5 text-sm text-zinc-900 focus:border-blue-500 focus:outline-none dark:border-zinc-600 dark:bg-zinc-800 dark:text-zinc-100"
          >
            <option value="">All Types</option>
            <option value="BU">Buy</option>
            <option value="SL">Sell</option>
            <option value="TR">Transfer</option>
            <option value="FE">Fee</option>
          </select>
        </div>

        <div className="flex items-center gap-2">
          <label
            htmlFor="txn-status-filter"
            className="text-sm font-medium text-zinc-700 dark:text-zinc-300"
          >
            Status:
          </label>
          <select
            id="txn-status-filter"
            value={statusFilter}
            onChange={(e) => { setStatusFilter(e.target.value as TransactionStatus | ""); setPage(0); }}
            className="rounded-md border border-zinc-300 bg-white px-3 py-1.5 text-sm text-zinc-900 focus:border-blue-500 focus:outline-none dark:border-zinc-600 dark:bg-zinc-800 dark:text-zinc-100"
          >
            <option value="">All Statuses</option>
            <option value="P">Pending</option>
            <option value="D">Done</option>
            <option value="F">Failed</option>
            <option value="R">Reversed</option>
          </select>
        </div>
      </div>

      {/* Data Table — column headers match HISMAP: Date, Type, Units, Price, Amount */}
      <div className="overflow-hidden rounded-lg border border-zinc-200 dark:border-zinc-700">
        <table className="min-w-full text-left text-sm">
          <thead className="bg-zinc-100 dark:bg-zinc-800">
            <tr>
              <th
                className="cursor-pointer px-4 py-3 font-medium text-zinc-700 hover:text-zinc-900 dark:text-zinc-300 dark:hover:text-zinc-100"
                onClick={() => handleSort("date")}
              >
                Date{sortIndicator("date")}
              </th>
              <th
                className="cursor-pointer px-4 py-3 font-medium text-zinc-700 hover:text-zinc-900 dark:text-zinc-300 dark:hover:text-zinc-100"
                onClick={() => handleSort("type")}
              >
                Type{sortIndicator("type")}
              </th>
              <th className="px-4 py-3 font-medium text-zinc-700 dark:text-zinc-300">
                Investment
              </th>
              <th
                className="cursor-pointer px-4 py-3 font-medium text-zinc-700 hover:text-zinc-900 dark:text-zinc-300 dark:hover:text-zinc-100"
                onClick={() => handleSort("quantity")}
              >
                Units{sortIndicator("quantity")}
              </th>
              <th
                className="cursor-pointer px-4 py-3 font-medium text-zinc-700 hover:text-zinc-900 dark:text-zinc-300 dark:hover:text-zinc-100"
                onClick={() => handleSort("price")}
              >
                Price{sortIndicator("price")}
              </th>
              <th
                className="cursor-pointer px-4 py-3 font-medium text-zinc-700 hover:text-zinc-900 dark:text-zinc-300 dark:hover:text-zinc-100"
                onClick={() => handleSort("amount")}
              >
                Amount{sortIndicator("amount")}
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
                  colSpan={7}
                  className="px-4 py-8 text-center text-zinc-400 dark:text-zinc-500"
                >
                  Loading transactions...
                </td>
              </tr>
            ) : error ? (
              <tr>
                <td
                  colSpan={7}
                  className="px-4 py-8 text-center text-red-500"
                >
                  Failed to load transactions.
                </td>
              </tr>
            ) : filteredAndSorted.length === 0 ? (
              <tr>
                <td
                  colSpan={7}
                  className="px-4 py-8 text-center text-zinc-400 dark:text-zinc-500"
                >
                  No transactions found.
                </td>
              </tr>
            ) : (
              filteredAndSorted.map((tx, idx) => (
                <tr
                  key={`${tx.portfolioId}-${tx.sequenceNo}-${idx}`}
                  className="hover:bg-zinc-50 dark:hover:bg-zinc-750"
                >
                  <td className="px-4 py-3 text-zinc-900 dark:text-zinc-100">
                    {tx.date}
                  </td>
                  <td className="px-4 py-3">
                    <TypeBadge type={tx.type} />
                  </td>
                  <td className="px-4 py-3 font-mono text-zinc-600 dark:text-zinc-400">
                    {tx.investmentId}
                  </td>
                  <td className="px-4 py-3 font-mono text-zinc-900 dark:text-zinc-100">
                    {formatQuantity(tx.quantity)}
                  </td>
                  <td className="px-4 py-3 font-mono text-zinc-700 dark:text-zinc-300">
                    {formatCurrency(tx.price)}
                  </td>
                  <td className="px-4 py-3 font-mono text-zinc-900 dark:text-zinc-100">
                    {formatCurrency(tx.amount)}
                  </td>
                  <td className="px-4 py-3">
                    <StatusBadge status={tx.status} />
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination — mirrors PF7=Previous PF8=Next from HISMAP */}
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

function TypeBadge({ type }: { type: TransactionType }) {
  const colors: Record<TransactionType, string> = {
    BU: "bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200",
    SL: "bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200",
    TR: "bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200",
    FE: "bg-amber-100 text-amber-800 dark:bg-amber-900 dark:text-amber-200",
  };

  return (
    <span
      className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${colors[type]}`}
    >
      {transactionTypeLabel(type)}
    </span>
  );
}

function StatusBadge({ status }: { status: TransactionStatus }) {
  const colors: Record<TransactionStatus, string> = {
    P: "bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-200",
    D: "bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200",
    F: "bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200",
    R: "bg-zinc-100 text-zinc-800 dark:bg-zinc-700 dark:text-zinc-200",
  };

  return (
    <span
      className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${colors[status]}`}
    >
      {transactionStatusLabel(status)}
    </span>
  );
}
