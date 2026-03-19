import { useState, useMemo } from 'react';
import type { ErrorLogEntry } from '@/types';
import { cn } from '@/lib/utils';
import { Search, ArrowUpDown } from 'lucide-react';

interface ErrorLogTableProps {
  entries: ErrorLogEntry[];
}

type SortKey = 'timestamp' | 'errorCode' | 'program' | 'severity' | 'message';
type SortDir = 'asc' | 'desc';

const severityBadge = {
  critical: 'bg-red-100 text-red-700',
  warning: 'bg-yellow-100 text-yellow-700',
  info: 'bg-blue-100 text-blue-700',
};

const severityOrder = { critical: 0, warning: 1, info: 2 };

export default function ErrorLogTable({ entries }: ErrorLogTableProps) {
  const [filter, setFilter] = useState('');
  const [sortKey, setSortKey] = useState<SortKey>('timestamp');
  const [sortDir, setSortDir] = useState<SortDir>('desc');

  const handleSort = (key: SortKey) => {
    if (sortKey === key) {
      setSortDir(sortDir === 'asc' ? 'desc' : 'asc');
    } else {
      setSortKey(key);
      setSortDir('desc');
    }
  };

  const filtered = useMemo(() => {
    const lowerFilter = filter.toLowerCase();
    return entries.filter(
      (e) =>
        e.errorCode.toLowerCase().includes(lowerFilter) ||
        e.program.toLowerCase().includes(lowerFilter) ||
        e.message.toLowerCase().includes(lowerFilter)
    );
  }, [entries, filter]);

  const sorted = useMemo(() => {
    return [...filtered].sort((a, b) => {
      const dir = sortDir === 'asc' ? 1 : -1;
      if (sortKey === 'severity') {
        return (severityOrder[a.severity] - severityOrder[b.severity]) * dir;
      }
      const aVal = a[sortKey];
      const bVal = b[sortKey];
      return aVal < bVal ? -dir : aVal > bVal ? dir : 0;
    });
  }, [filtered, sortKey, sortDir]);

  const ariaSort = (key: SortKey): 'ascending' | 'descending' | 'none' => {
    if (sortKey !== key) return 'none';
    return sortDir === 'asc' ? 'ascending' : 'descending';
  };

  const columns: { key: SortKey; label: string; className?: string }[] = [
    { key: 'timestamp', label: 'Timestamp', className: 'w-44' },
    { key: 'errorCode', label: 'Error Code', className: 'w-28' },
    { key: 'program', label: 'Program', className: 'w-28' },
    { key: 'severity', label: 'Severity', className: 'w-24' },
    { key: 'message', label: 'Message' },
  ];

  return (
    <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between mb-4">
        <h3 className="text-sm font-medium text-gray-600">Recent Error Log</h3>
        <div className="relative mt-2 sm:mt-0">
          <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
          <input
            type="text"
            placeholder="Filter by code, program, or message..."
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
            className="pl-8 pr-3 py-1.5 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 w-full sm:w-72"
            aria-label="Filter error log entries"
          />
        </div>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-gray-200">
              {columns.map((col) => (
                <th
                  key={col.key}
                  scope="col"
                  aria-sort={ariaSort(col.key)}
                  className={cn(
                    'text-left text-xs font-medium text-gray-500 uppercase tracking-wider py-2 pr-4 cursor-pointer select-none hover:text-gray-700',
                    col.className
                  )}
                  onClick={() => handleSort(col.key)}
                >
                  <span className="inline-flex items-center gap-1">
                    {col.label}
                    <ArrowUpDown className="h-3 w-3" />
                  </span>
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {sorted.map((entry, i) => (
              <tr key={`${entry.timestamp}-${entry.errorCode}-${i}`} className="border-b border-gray-100 hover:bg-gray-50">
                <td className="py-2.5 pr-4 text-gray-600 whitespace-nowrap font-mono text-xs">{entry.timestamp}</td>
                <td className="py-2.5 pr-4 font-mono text-xs font-medium text-gray-900">{entry.errorCode}</td>
                <td className="py-2.5 pr-4 font-mono text-xs text-gray-700">{entry.program}</td>
                <td className="py-2.5 pr-4">
                  <span className={cn('inline-block text-xs font-medium rounded-full px-2 py-0.5', severityBadge[entry.severity])}>
                    {entry.severity}
                  </span>
                </td>
                <td className="py-2.5 text-gray-600 text-xs">{entry.message}</td>
              </tr>
            ))}
            {sorted.length === 0 && (
              <tr>
                <td colSpan={5} className="py-8 text-center text-gray-400">
                  No matching entries found
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <p className="mt-3 text-xs text-gray-400">Showing {sorted.length} of {entries.length} most recent entries</p>
    </div>
  );
}
