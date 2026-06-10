import { useEffect, useMemo, useState } from 'react';
import { FilterX } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { PageHeader } from '@/components/ui/PageHeader';
import { LoadingSpinner } from '@/components/ui/LoadingSpinner';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { StatusBadge, getAuditStatusVariant } from '@/components/ui/StatusBadge';
import { auditEntries } from '@/data/mockData';
import type { AuditEntry } from '@/data/types';

type AuditRow = AuditEntry & Record<string, unknown>;

const selectClass =
  'rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-700 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500';

const columns: Column<AuditRow>[] = [
  { key: 'timestamp', header: 'Timestamp', sortable: true },
  { key: 'program', header: 'Program', sortable: true },
  { key: 'type', header: 'Type', sortable: true },
  { key: 'action', header: 'Action', sortable: true },
  {
    key: 'status',
    header: 'Status',
    sortable: true,
    render: (row) => <StatusBadge label={row.status} variant={getAuditStatusVariant(row.status)} />,
  },
  {
    key: 'portfolioId',
    header: 'Portfolio',
    sortable: true,
    render: (row) => row.portfolioId || '—',
  },
  { key: 'message', header: 'Message', className: 'whitespace-normal' },
];

export function AuditReportPage() {
  const [loading, setLoading] = useState(true);
  const [programFilter, setProgramFilter] = useState('');
  const [typeFilter, setTypeFilter] = useState('');
  const [actionFilter, setActionFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');

  useEffect(() => {
    const timer = setTimeout(() => setLoading(false), 400);
    return () => clearTimeout(timer);
  }, []);

  const programs = useMemo(() => [...new Set(auditEntries.map((e) => e.program))].sort(), []);
  const types = useMemo(() => [...new Set(auditEntries.map((e) => e.type))].sort(), []);
  const actions = useMemo(() => [...new Set(auditEntries.map((e) => e.action))].sort(), []);

  const filtered = useMemo<AuditRow[]>(
    () =>
      auditEntries.filter((entry) => {
        if (programFilter && entry.program !== programFilter) return false;
        if (typeFilter && entry.type !== typeFilter) return false;
        if (actionFilter && entry.action !== actionFilter) return false;
        if (statusFilter && entry.status !== statusFilter) return false;
        const entryDate = entry.timestamp.slice(0, 10);
        if (dateFrom && entryDate < dateFrom) return false;
        if (dateTo && entryDate > dateTo) return false;
        return true;
      }) as AuditRow[],
    [programFilter, typeFilter, actionFilter, statusFilter, dateFrom, dateTo],
  );

  const hasFilters = Boolean(programFilter || typeFilter || actionFilter || statusFilter || dateFrom || dateTo);

  const clearFilters = () => {
    setProgramFilter('');
    setTypeFilter('');
    setActionFilter('');
    setStatusFilter('');
    setDateFrom('');
    setDateTo('');
  };

  if (loading) {
    return (
      <div>
        <PageHeader title="Audit Report" description="System audit trail of batch and online program activity" />
        <LoadingSpinner message="Generating audit report..." />
      </div>
    );
  }

  return (
    <div>
      <PageHeader title="Audit Report" description="System audit trail of batch and online program activity" />
      <Card className="mb-6">
        <div className="flex flex-wrap items-end gap-4">
          <div className="flex flex-col gap-1">
            <label htmlFor="audit-program" className="text-xs font-semibold text-slate-600 uppercase tracking-wider">
              Program
            </label>
            <select id="audit-program" className={selectClass} value={programFilter} onChange={(e) => setProgramFilter(e.target.value)}>
              <option value="">All programs</option>
              {programs.map((p) => (
                <option key={p} value={p}>
                  {p}
                </option>
              ))}
            </select>
          </div>
          <div className="flex flex-col gap-1">
            <label htmlFor="audit-type" className="text-xs font-semibold text-slate-600 uppercase tracking-wider">
              Type
            </label>
            <select id="audit-type" className={selectClass} value={typeFilter} onChange={(e) => setTypeFilter(e.target.value)}>
              <option value="">All types</option>
              {types.map((t) => (
                <option key={t} value={t}>
                  {t}
                </option>
              ))}
            </select>
          </div>
          <div className="flex flex-col gap-1">
            <label htmlFor="audit-action" className="text-xs font-semibold text-slate-600 uppercase tracking-wider">
              Action
            </label>
            <select id="audit-action" className={selectClass} value={actionFilter} onChange={(e) => setActionFilter(e.target.value)}>
              <option value="">All actions</option>
              {actions.map((a) => (
                <option key={a} value={a}>
                  {a}
                </option>
              ))}
            </select>
          </div>
          <div className="flex flex-col gap-1">
            <label htmlFor="audit-status" className="text-xs font-semibold text-slate-600 uppercase tracking-wider">
              Status
            </label>
            <select id="audit-status" className={selectClass} value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
              <option value="">All statuses</option>
              <option value="SUCC">Success</option>
              <option value="FAIL">Failure</option>
            </select>
          </div>
          <div className="flex flex-col gap-1">
            <label htmlFor="audit-date-from" className="text-xs font-semibold text-slate-600 uppercase tracking-wider">
              From
            </label>
            <input
              id="audit-date-from"
              type="date"
              className={selectClass}
              value={dateFrom}
              onChange={(e) => setDateFrom(e.target.value)}
            />
          </div>
          <div className="flex flex-col gap-1">
            <label htmlFor="audit-date-to" className="text-xs font-semibold text-slate-600 uppercase tracking-wider">
              To
            </label>
            <input id="audit-date-to" type="date" className={selectClass} value={dateTo} onChange={(e) => setDateTo(e.target.value)} />
          </div>
          {hasFilters && (
            <button
              onClick={clearFilters}
              className="inline-flex items-center gap-2 rounded-md border border-slate-300 px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50 transition-colors"
            >
              <FilterX className="w-4 h-4" />
              Clear filters
            </button>
          )}
        </div>
      </Card>
      <Card>
        <p className="mb-4 text-sm text-slate-500">
          Showing {filtered.length} of {auditEntries.length} audit entries
        </p>
        <DataTable
          columns={columns}
          data={filtered}
          keyExtractor={(row) => `${row.timestamp}-${row.program}-${row.action}-${row.accountNo}`}
          emptyMessage="No audit entries match the selected filters"
        />
      </Card>
    </div>
  );
}
