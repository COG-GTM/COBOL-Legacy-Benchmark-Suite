import { useState, useMemo } from 'react';
import type { ReactNode } from 'react';
import { ChevronUp, ChevronDown, ChevronsUpDown } from 'lucide-react';

export interface Column<T> {
  key: string;
  header: string;
  sortable?: boolean;
  render?: (row: T) => ReactNode;
  className?: string;
}

export type SortDirection = 'asc' | 'desc' | null;

interface DataTableProps<T> {
  columns: Column<T>[];
  data: T[];
  keyExtractor: (row: T) => string;
  emptyMessage?: string;
  sortKey?: string | null;
  sortDirection?: SortDirection;
  onSortChange?: (key: string | null, direction: SortDirection) => void;
}

export function DataTable<T extends Record<string, unknown>>({
  columns,
  data,
  keyExtractor,
  emptyMessage = 'No data found',
  sortKey: controlledSortKey,
  sortDirection: controlledSortDir,
  onSortChange,
}: DataTableProps<T>) {
  const isControlled = onSortChange !== undefined;

  const [internalSortKey, setInternalSortKey] = useState<string | null>(null);
  const [internalSortDir, setInternalSortDir] = useState<SortDirection>(null);

  const sortKey = isControlled ? (controlledSortKey ?? null) : internalSortKey;
  const sortDir = isControlled ? (controlledSortDir ?? null) : internalSortDir;

  const handleSort = (key: string) => {
    let newKey: string | null;
    let newDir: SortDirection;

    if (sortKey === key) {
      if (sortDir === 'asc') {
        newKey = key;
        newDir = 'desc';
      } else {
        newKey = null;
        newDir = null;
      }
    } else {
      newKey = key;
      newDir = 'asc';
    }

    if (isControlled) {
      onSortChange(newKey, newDir);
    } else {
      setInternalSortKey(newKey);
      setInternalSortDir(newDir);
    }
  };

  const sortedData = useMemo(() => {
    if (isControlled) return data;
    if (!sortKey || !sortDir) return data;
    return [...data].sort((a, b) => {
      const aVal = a[sortKey];
      const bVal = b[sortKey];
      if (aVal == null && bVal == null) return 0;
      if (aVal == null) return 1;
      if (bVal == null) return -1;
      if (typeof aVal === 'number' && typeof bVal === 'number') {
        return sortDir === 'asc' ? aVal - bVal : bVal - aVal;
      }
      const aStr = String(aVal);
      const bStr = String(bVal);
      return sortDir === 'asc' ? aStr.localeCompare(bStr) : bStr.localeCompare(aStr);
    });
  }, [data, sortKey, sortDir, isControlled]);

  if (data.length === 0) {
    return (
      <div className="text-center py-12 text-slate-500">
        <p>{emptyMessage}</p>
      </div>
    );
  }

  return (
    <div className="overflow-x-auto">
      <table className="min-w-full divide-y divide-slate-200">
        <thead className="bg-slate-50">
          <tr>
            {columns.map((col) => (
              <th
                key={col.key}
                className={`px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider ${
                  col.sortable ? 'cursor-pointer select-none hover:bg-slate-100' : ''
                } ${col.className ?? ''}`}
                onClick={col.sortable ? () => handleSort(col.key) : undefined}
              >
                <div className="flex items-center gap-1">
                  {col.header}
                  {col.sortable && (
                    <span className="text-slate-400">
                      {sortKey === col.key && sortDir === 'asc' ? (
                        <ChevronUp className="w-3.5 h-3.5" />
                      ) : sortKey === col.key && sortDir === 'desc' ? (
                        <ChevronDown className="w-3.5 h-3.5" />
                      ) : (
                        <ChevronsUpDown className="w-3.5 h-3.5" />
                      )}
                    </span>
                  )}
                </div>
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="bg-white divide-y divide-slate-200">
          {sortedData.map((row) => (
            <tr key={keyExtractor(row)} className="hover:bg-slate-50 transition-colors">
              {columns.map((col) => (
                <td key={col.key} className={`px-4 py-3 text-sm text-slate-700 whitespace-nowrap ${col.className ?? ''}`}>
                  {col.render ? col.render(row) : String(row[col.key] ?? '')}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
