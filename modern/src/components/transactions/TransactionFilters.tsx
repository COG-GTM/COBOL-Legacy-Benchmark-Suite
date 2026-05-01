"use client";

import { useState } from "react";
import type { TransactionType } from "@/types/portfolio";
import { TRANSACTION_TYPE_LABELS } from "@/types/portfolio";
import { Filter, X } from "lucide-react";

export interface TransactionFilterValues {
  dateFrom: string;
  dateTo: string;
  types: TransactionType[];
}

interface TransactionFiltersProps {
  filters: TransactionFilterValues;
  onChange: (filters: TransactionFilterValues) => void;
}

const ALL_TYPES: TransactionType[] = ["BU", "SL", "TR", "FE"];

export default function TransactionFilters({
  filters,
  onChange,
}: TransactionFiltersProps) {
  const [open, setOpen] = useState(false);

  function toggleType(type: TransactionType) {
    const next = filters.types.includes(type)
      ? filters.types.filter((t) => t !== type)
      : [...filters.types, type];
    onChange({ ...filters, types: next });
  }

  function clearFilters() {
    onChange({ dateFrom: "", dateTo: "", types: [] });
  }

  const hasFilters = filters.dateFrom || filters.dateTo || filters.types.length > 0;

  return (
    <div className="space-y-3">
      <div className="flex items-center gap-2">
        <button
          type="button"
          onClick={() => setOpen(!open)}
          className="inline-flex items-center gap-1.5 rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
        >
          <Filter className="h-4 w-4" />
          Filters
          {hasFilters && (
            <span className="ml-1 rounded-full bg-indigo-100 px-2 py-0.5 text-xs font-medium text-indigo-700">
              Active
            </span>
          )}
        </button>
        {hasFilters && (
          <button
            type="button"
            onClick={clearFilters}
            className="inline-flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700"
          >
            <X className="h-3.5 w-3.5" />
            Clear
          </button>
        )}
      </div>

      {open && (
        <div className="rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
            <div>
              <label
                htmlFor="date-from"
                className="block text-sm font-medium text-gray-700"
              >
                Date From
              </label>
              <input
                id="date-from"
                type="date"
                value={filters.dateFrom}
                onChange={(e) =>
                  onChange({ ...filters, dateFrom: e.target.value })
                }
                className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              />
            </div>
            <div>
              <label
                htmlFor="date-to"
                className="block text-sm font-medium text-gray-700"
              >
                Date To
              </label>
              <input
                id="date-to"
                type="date"
                value={filters.dateTo}
                onChange={(e) =>
                  onChange({ ...filters, dateTo: e.target.value })
                }
                className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              />
            </div>
            <div>
              <span className="block text-sm font-medium text-gray-700">
                Transaction Type
              </span>
              <div className="mt-2 flex flex-wrap gap-2">
                {ALL_TYPES.map((type) => {
                  const selected = filters.types.includes(type);
                  return (
                    <button
                      key={type}
                      type="button"
                      onClick={() => toggleType(type)}
                      className={`rounded-full px-3 py-1 text-xs font-medium transition-colors ${
                        selected
                          ? "bg-indigo-100 text-indigo-700"
                          : "bg-gray-100 text-gray-600 hover:bg-gray-200"
                      }`}
                    >
                      {TRANSACTION_TYPE_LABELS[type]}
                    </button>
                  );
                })}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
