import { useState, useMemo } from 'react';
import type { HistoryEntry } from '../types';
import { formatCurrency, formatNumber } from '../utils/validation';

interface TransactionTableProps {
  data: HistoryEntry[];
}

type SortField = 'date' | 'type' | 'units' | 'price' | 'amount';
type SortDir = 'asc' | 'desc';

const PAGE_SIZE = 10; // Matches OCCURS 10 TIMES in WS-HISTORY-TABLE

export function TransactionTable({ data }: TransactionTableProps) {
  const [sortField, setSortField] = useState<SortField>('date');
  const [sortDir, setSortDir] = useState<SortDir>('desc');
  const [page, setPage] = useState(0);
  const [typeFilter, setTypeFilter] = useState('');

  const filtered = useMemo(() => {
    let result = data;
    if (typeFilter) {
      result = result.filter(r => r.type === typeFilter);
    }
    return result;
  }, [data, typeFilter]);

  const sorted = useMemo(() => {
    return [...filtered].sort((a, b) => {
      const aVal = a[sortField];
      const bVal = b[sortField];
      const cmp = typeof aVal === 'string' ? aVal.localeCompare(bVal as string) : (aVal as number) - (bVal as number);
      return sortDir === 'asc' ? cmp : -cmp;
    });
  }, [filtered, sortField, sortDir]);

  const totalPages = Math.ceil(sorted.length / PAGE_SIZE);
  const pageData = sorted.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE);

  const toggleSort = (field: SortField) => {
    if (sortField === field) {
      setSortDir(d => (d === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortField(field);
      setSortDir('desc');
    }
    setPage(0);
  };

  const sortIndicator = (field: SortField) =>
    sortField === field ? (sortDir === 'asc' ? ' ^' : ' v') : '';

  return (
    <div>
      {/* Filter */}
      <div className="mb-4 flex items-center gap-3">
        <label className="text-sm text-gray-600">Filter by type:</label>
        <select
          value={typeFilter}
          onChange={e => { setTypeFilter(e.target.value); setPage(0); }}
          className="border border-gray-300 rounded px-2 py-1 text-sm"
        >
          <option value="">All</option>
          <option value="Buy">Buy</option>
          <option value="Sell">Sell</option>
          <option value="Transfer">Transfer</option>
          <option value="Fee">Fee</option>
        </select>
        <span className="text-xs text-gray-400">{sorted.length} records</span>
      </div>

      {/* Table */}
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b-2 border-gray-300">
              {([['date', 'Date'], ['type', 'Type'], ['units', 'Units'], ['price', 'Price'], ['amount', 'Amount']] as [SortField, string][]).map(([field, label]) => (
                <th
                  key={field}
                  onClick={() => toggleSort(field)}
                  className="text-left py-2 px-3 font-semibold text-gray-700 cursor-pointer hover:text-blue-600 select-none"
                >
                  {label}{sortIndicator(field)}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {pageData.map((row, i) => (
              <tr key={i} className="border-b border-gray-100 hover:bg-gray-50">
                <td className="py-2 px-3">{row.date}</td>
                <td className="py-2 px-3">
                  <span className={`inline-block px-2 py-0.5 rounded text-xs font-medium ${
                    row.type === 'Buy' ? 'bg-green-100 text-green-800' :
                    row.type === 'Sell' ? 'bg-red-100 text-red-800' :
                    row.type === 'Transfer' ? 'bg-blue-100 text-blue-800' :
                    'bg-gray-100 text-gray-800'
                  }`}>
                    {row.type}
                  </span>
                </td>
                <td className="py-2 px-3 text-right font-mono">{formatNumber(row.units, 2)}</td>
                <td className="py-2 px-3 text-right font-mono">{formatCurrency(row.price)}</td>
                <td className="py-2 px-3 text-right font-mono">{formatCurrency(row.amount)}</td>
              </tr>
            ))}
            {pageData.length === 0 && (
              <tr><td colSpan={5} className="text-center py-8 text-gray-400">No records</td></tr>
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination - PF7/PF8 style */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between mt-4">
          <button
            onClick={() => setPage(p => Math.max(0, p - 1))}
            disabled={page === 0}
            className="bg-gray-200 text-gray-700 px-3 py-1.5 rounded text-sm hover:bg-gray-300 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            PF7 Previous
          </button>
          <span className="text-sm text-gray-500">
            Page {page + 1} of {totalPages}
          </span>
          <button
            onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
            disabled={page === totalPages - 1}
            className="bg-gray-200 text-gray-700 px-3 py-1.5 rounded text-sm hover:bg-gray-300 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            PF8 Next
          </button>
        </div>
      )}
    </div>
  );
}
