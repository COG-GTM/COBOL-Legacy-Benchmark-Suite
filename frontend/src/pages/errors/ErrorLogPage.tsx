import { useState, useMemo } from 'react';
import { AlertTriangle, XCircle } from 'lucide-react';
import { PageHeader } from '@/components/ui/PageHeader';
import { DataTable } from '@/components/ui/DataTable';
import { StatusBadge, getSeverityVariant } from '@/components/ui/StatusBadge';
import { errorEntries } from '@/data/mockData';
import type { ErrorEntry } from '@/data/types';
import type { Column } from '@/components/ui/DataTable';

type ErrorRecord = ErrorEntry & Record<string, unknown>;

const ITEMS_PER_PAGE = 10;

const programs = Array.from(new Set(errorEntries.map((e) => e.program))).sort();

const columns: Column<ErrorRecord>[] = [
  { key: 'timestamp', header: 'Timestamp', sortable: true },
  { key: 'code', header: 'Code', sortable: true, className: 'font-mono' },
  { key: 'description', header: 'Description', sortable: true },
  {
    key: 'severity',
    header: 'Severity',
    sortable: true,
    render: (row) => <StatusBadge label={row.severity} variant={getSeverityVariant(row.severity)} />,
  },
  { key: 'program', header: 'Program', sortable: true, className: 'font-mono' },
  { key: 'action', header: 'Action' },
];

export function ErrorLogPage() {
  const [severityFilter, setSeverityFilter] = useState<'All' | 'Error' | 'Warning'>('All');
  const [programFilter, setProgramFilter] = useState<string>('All');
  const [page, setPage] = useState(1);

  const filtered = useMemo(() => {
    return errorEntries.filter((e) => {
      if (severityFilter !== 'All' && e.severity !== severityFilter) return false;
      if (programFilter !== 'All' && e.program !== programFilter) return false;
      return true;
    });
  }, [severityFilter, programFilter]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / ITEMS_PER_PAGE));
  const safePage = Math.min(page, totalPages);
  const paginated = filtered.slice((safePage - 1) * ITEMS_PER_PAGE, safePage * ITEMS_PER_PAGE);

  const handleSeverityChange = (value: string) => {
    setSeverityFilter(value as 'All' | 'Error' | 'Warning');
    setPage(1);
  };

  const handleProgramChange = (value: string) => {
    setProgramFilter(value);
    setPage(1);
  };

  const errorCount = errorEntries.filter((e) => e.severity === 'Error').length;
  const warningCount = errorEntries.filter((e) => e.severity === 'Warning').length;

  return (
    <div>
      <PageHeader title="Error Log" description="System error and warning messages" />

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
        <div className="bg-white rounded-lg border border-slate-200 p-4 flex items-center gap-3">
          <div className="p-2 rounded-lg bg-slate-100">
            <AlertTriangle className="w-5 h-5 text-slate-600" />
          </div>
          <div>
            <p className="text-2xl font-bold text-slate-900">{errorEntries.length}</p>
            <p className="text-xs text-slate-500">Total Entries</p>
          </div>
        </div>
        <div className="bg-white rounded-lg border border-slate-200 p-4 flex items-center gap-3">
          <div className="p-2 rounded-lg bg-red-50">
            <XCircle className="w-5 h-5 text-red-600" />
          </div>
          <div>
            <p className="text-2xl font-bold text-red-600">{errorCount}</p>
            <p className="text-xs text-slate-500">Errors</p>
          </div>
        </div>
        <div className="bg-white rounded-lg border border-slate-200 p-4 flex items-center gap-3">
          <div className="p-2 rounded-lg bg-amber-50">
            <AlertTriangle className="w-5 h-5 text-amber-600" />
          </div>
          <div>
            <p className="text-2xl font-bold text-amber-600">{warningCount}</p>
            <p className="text-xs text-slate-500">Warnings</p>
          </div>
        </div>
      </div>

      <div className="bg-white rounded-lg border border-slate-200">
        <div className="p-4 border-b border-slate-200 flex flex-wrap items-center gap-4">
          <div className="flex items-center gap-2">
            <label className="text-sm font-medium text-slate-600">Severity:</label>
            <select
              value={severityFilter}
              onChange={(e) => handleSeverityChange(e.target.value)}
              className="text-sm border border-slate-300 rounded-lg px-3 py-1.5 bg-white text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="All">All</option>
              <option value="Error">Error</option>
              <option value="Warning">Warning</option>
            </select>
          </div>
          <div className="flex items-center gap-2">
            <label className="text-sm font-medium text-slate-600">Program:</label>
            <select
              value={programFilter}
              onChange={(e) => handleProgramChange(e.target.value)}
              className="text-sm border border-slate-300 rounded-lg px-3 py-1.5 bg-white text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="All">All</option>
              {programs.map((p) => (
                <option key={p} value={p}>
                  {p}
                </option>
              ))}
            </select>
          </div>
          <span className="text-sm text-slate-500 ml-auto">
            {filtered.length} {filtered.length === 1 ? 'entry' : 'entries'}
          </span>
        </div>

        <DataTable<ErrorRecord>
          columns={columns}
          data={paginated as ErrorRecord[]}
          keyExtractor={(row) => `${row.code}-${row.timestamp}-${row.program}`}
          emptyMessage="No error entries match the current filters"
        />

        {totalPages > 1 && (
          <div className="flex items-center justify-between px-4 py-3 border-t border-slate-200">
            <p className="text-sm text-slate-500">
              Page {safePage} of {totalPages}
            </p>
            <div className="flex items-center gap-2">
              <button
                onClick={() => setPage((p) => Math.max(1, p - 1))}
                disabled={safePage <= 1}
                className="px-3 py-1.5 text-sm font-medium rounded-lg border border-slate-300 bg-white text-slate-700 hover:bg-slate-50 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Previous
              </button>
              <button
                onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
                disabled={safePage >= totalPages}
                className="px-3 py-1.5 text-sm font-medium rounded-lg border border-slate-300 bg-white text-slate-700 hover:bg-slate-50 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
