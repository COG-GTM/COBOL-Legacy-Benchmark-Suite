"use client";

import { useState, useMemo } from "react";
import Link from "next/link";
import { MOCK_PORTFOLIOS } from "@/data/mock";
import { CLIENT_TYPE_LABELS } from "@/types/portfolio";
import AccountSearch from "@/components/portfolio/AccountSearch";
import StatusBadge from "@/components/common/StatusBadge";
import CurrencyDisplay from "@/components/common/CurrencyDisplay";
import { ChevronRight } from "lucide-react";

export default function PortfoliosPage() {
  const [searchTerm, setSearchTerm] = useState("");

  const filtered = useMemo(() => {
    if (!searchTerm) return MOCK_PORTFOLIOS;
    const term = searchTerm.toLowerCase();
    return MOCK_PORTFOLIOS.filter(
      (p) =>
        p.accountNo.includes(term) ||
        p.id.toLowerCase().includes(term) ||
        p.clientName.toLowerCase().includes(term)
    );
  }, [searchTerm]);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">
            Portfolio Position Inquiry
          </h1>
          <p className="mt-1 text-sm text-gray-500">
            Search and view portfolio positions
          </p>
        </div>
        <AccountSearch onSearch={setSearchTerm} />
      </div>

      <div className="overflow-hidden rounded-lg border border-gray-200 bg-white shadow-sm">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                Account
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                Client
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                Type
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                Total Value
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                Status
              </th>
              <th className="px-4 py-3" />
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200">
            {filtered.length === 0 ? (
              <tr>
                <td
                  colSpan={6}
                  className="px-4 py-8 text-center text-sm text-gray-500"
                >
                  No portfolios found
                </td>
              </tr>
            ) : (
              filtered.map((p) => (
                <tr key={p.id} className="hover:bg-gray-50 transition-colors">
                  <td className="whitespace-nowrap px-4 py-3 text-sm font-mono text-gray-900">
                    {p.accountNo}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-gray-900">
                    {p.clientName}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-gray-500">
                    {CLIENT_TYPE_LABELS[p.clientType]}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm">
                    <CurrencyDisplay amount={p.totalValue} />
                  </td>
                  <td className="whitespace-nowrap px-4 py-3">
                    <StatusBadge code={p.status} />
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-right">
                    <Link
                      href={`/portfolios/${p.id}`}
                      className="inline-flex items-center gap-1 text-sm font-medium text-indigo-600 hover:text-indigo-800"
                    >
                      View
                      <ChevronRight className="h-4 w-4" />
                    </Link>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
