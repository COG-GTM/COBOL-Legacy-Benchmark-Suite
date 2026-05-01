"use client";

import { useState, useMemo } from "react";
import { MOCK_TRANSACTIONS } from "@/data/mock";
import TransactionTable from "@/components/transactions/TransactionTable";
import TransactionFilters, {
  type TransactionFilterValues,
} from "@/components/transactions/TransactionFilters";
import AccountSearch from "@/components/portfolio/AccountSearch";

function cobolDateToISO(d: string): string {
  if (d.length !== 8) return d;
  return `${d.slice(0, 4)}-${d.slice(4, 6)}-${d.slice(6, 8)}`;
}

export default function TransactionsPage() {
  const [accountFilter, setAccountFilter] = useState("");
  const [filters, setFilters] = useState<TransactionFilterValues>({
    dateFrom: "",
    dateTo: "",
    types: [],
  });

  const filtered = useMemo(() => {
    let result = MOCK_TRANSACTIONS;

    if (accountFilter) {
      const term = accountFilter.toLowerCase();
      result = result.filter((t) =>
        t.portfolioId.toLowerCase().includes(term)
      );
    }

    if (filters.dateFrom) {
      result = result.filter(
        (t) => cobolDateToISO(t.date) >= filters.dateFrom
      );
    }
    if (filters.dateTo) {
      result = result.filter(
        (t) => cobolDateToISO(t.date) <= filters.dateTo
      );
    }
    if (filters.types.length > 0) {
      result = result.filter((t) => filters.types.includes(t.type));
    }

    return result;
  }, [accountFilter, filters]);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">
            Transaction History
          </h1>
          <p className="mt-1 text-sm text-gray-500">
            View and filter transaction records
          </p>
        </div>
        <AccountSearch
          onSearch={setAccountFilter}
          initialValue={accountFilter}
        />
      </div>

      <TransactionFilters filters={filters} onChange={setFilters} />

      <TransactionTable transactions={filtered} />
    </div>
  );
}
