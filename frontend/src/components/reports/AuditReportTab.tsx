import { useState, useMemo } from 'react';
import { ReportSummaryCard } from '@/components/common/ReportSummaryCard';
import { DataTable } from '@/components/common/DataTable';
import type { ColumnDef } from '@/components/common/DataTable';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { auditReportSummary, auditReportEntries } from '@/mock/reportsData';
import type { AuditReportEntry } from '@/types/reports';
import { cn } from '@/lib/utils';

const statusVariant = (status: string) => {
  if (status === 'SUCC') return 'success' as const;
  if (status === 'FAIL') return 'destructive' as const;
  return 'warning' as const;
};

const typeVariant = (type: string) => {
  if (type === 'TRAN') return 'secondary' as const;
  if (type === 'USER') return 'default' as const;
  return 'outline' as const;
};

function HorizontalBar({ label, value, max }: { label: string; value: number; max: number }) {
  const widthPct = max > 0 ? (value / max) * 100 : 0;
  return (
    <div className="flex items-center gap-3">
      <span className="w-24 shrink-0 text-right text-xs text-[#94A3B8]">{label}</span>
      <div className="flex-1">
        <div
          className="h-5 rounded bg-[#22D3EE]/30"
          style={{ width: `${Math.max(widthPct, 2)}%` }}
        />
      </div>
      <span className="w-12 text-right text-xs font-medium text-[#CBD5E1]">{value}</span>
    </div>
  );
}

export function AuditReportTab() {
  const [typeFilter, setTypeFilter] = useState('All');
  const [actionFilter, setActionFilter] = useState('All');
  const [statusFilter, setStatusFilter] = useState('All');
  const [programFilter, setProgramFilter] = useState('All');
  const [userIdFilter, setUserIdFilter] = useState('');
  const [searchText, setSearchText] = useState('');

  const programs = useMemo(
    () => [...new Set(auditReportEntries.map((e) => e.program))].sort(),
    []
  );

  const filteredData = useMemo(() => {
    return auditReportEntries.filter((entry) => {
      if (typeFilter !== 'All' && entry.type !== typeFilter) return false;
      if (actionFilter !== 'All' && entry.action !== actionFilter) return false;
      if (statusFilter !== 'All' && entry.status !== statusFilter) return false;
      if (programFilter !== 'All' && entry.program !== programFilter) return false;
      if (userIdFilter && !entry.userId.toLowerCase().includes(userIdFilter.toLowerCase()))
        return false;
      if (searchText) {
        const search = searchText.toLowerCase();
        const searchable = [
          entry.timestamp,
          entry.systemId,
          entry.userId,
          entry.program,
          entry.terminal,
          entry.type,
          entry.action,
          entry.status,
          entry.portfolioId,
          entry.accountNo,
          entry.message,
        ]
          .join(' ')
          .toLowerCase();
        if (!searchable.includes(search)) return false;
      }
      return true;
    });
  }, [typeFilter, actionFilter, statusFilter, programFilter, userIdFilter, searchText]);

  const columns: ColumnDef<AuditReportEntry>[] = [
    { key: 'timestamp', header: 'Timestamp', sortable: true },
    { key: 'userId', header: 'User ID', sortable: true },
    { key: 'program', header: 'Program', sortable: true },
    { key: 'terminal', header: 'Terminal' },
    {
      key: 'type',
      header: 'Type',
      sortable: true,
      render: (row) => <Badge variant={typeVariant(row.type)}>{row.type}</Badge>,
    },
    {
      key: 'action',
      header: 'Action',
      sortable: true,
    },
    {
      key: 'status',
      header: 'Status',
      sortable: true,
      render: (row) => <Badge variant={statusVariant(row.status)}>{row.status}</Badge>,
    },
    { key: 'portfolioId', header: 'Portfolio ID' },
    { key: 'accountNo', header: 'Account' },
    {
      key: 'message',
      header: 'Message',
      render: (row) => (
        <span className="max-w-[200px] truncate" title={row.message}>
          {row.message}
        </span>
      ),
    },
  ];

  const clearFilters = () => {
    setTypeFilter('All');
    setActionFilter('All');
    setStatusFilter('All');
    setProgramFilter('All');
    setUserIdFilter('');
    setSearchText('');
  };

  const hasFilters =
    typeFilter !== 'All' ||
    actionFilter !== 'All' ||
    statusFilter !== 'All' ||
    programFilter !== 'All' ||
    userIdFilter ||
    searchText;

  const summary = auditReportSummary;
  const maxByType = Math.max(...summary.byType.map((t) => t.count));
  const maxByAction = Math.max(...summary.byAction.map((a) => a.count));
  const maxByProgram = Math.max(...summary.byProgram.map((p) => p.count));

  return (
    <div className="space-y-6">
      {/* Summary Section */}
      <div className="grid gap-4 lg:grid-cols-2">
        {/* Left: Event Summary Cards */}
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          <ReportSummaryCard title="Total Events" value={summary.totalEvents.toLocaleString()} color="blue" />
          <ReportSummaryCard title="Successes" value={summary.successCount.toLocaleString()} color="green" />
          <ReportSummaryCard title="Failures" value={summary.failureCount.toLocaleString()} color="red" />
          <ReportSummaryCard title="Warnings" value={summary.warningCount.toLocaleString()} color="yellow" />
        </div>

        {/* Right: Breakdown Charts */}
        <div className="space-y-4 rounded-xl border border-[#334155] bg-[#1E293B] p-4">
          <div>
            <h4 className="mb-2 text-xs font-semibold uppercase tracking-wider text-[#94A3B8]">
              By Type
            </h4>
            <div className="space-y-1">
              {summary.byType.map((t) => (
                <HorizontalBar key={t.type} label={t.type} value={t.count} max={maxByType} />
              ))}
            </div>
          </div>
          <div>
            <h4 className="mb-2 text-xs font-semibold uppercase tracking-wider text-[#94A3B8]">
              By Action
            </h4>
            <div className="space-y-1">
              {summary.byAction.map((a) => (
                <HorizontalBar key={a.action} label={a.action} value={a.count} max={maxByAction} />
              ))}
            </div>
          </div>
          <div>
            <h4 className="mb-2 text-xs font-semibold uppercase tracking-wider text-[#94A3B8]">
              By Program
            </h4>
            <div className="space-y-1">
              {summary.byProgram.map((p) => (
                <HorizontalBar key={p.program} label={p.program} value={p.count} max={maxByProgram} />
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* Filters Bar */}
      <div className="flex flex-wrap items-end gap-3 rounded-lg bg-[#0F172A] p-4">
        <div className="flex flex-col gap-1">
          <label htmlFor="audit-type-filter" className="sr-only">Type</label>
          <select
            id="audit-type-filter"
            value={typeFilter}
            onChange={(e) => setTypeFilter(e.target.value)}
            className="h-10 rounded-md border border-[#334155] bg-[#0F172A] px-3 py-2 text-sm text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#22D3EE]"
          >
            <option>All</option>
            <option>TRAN</option>
            <option>USER</option>
            <option>SYST</option>
          </select>
        </div>
        <div className="flex flex-col gap-1">
          <label htmlFor="audit-action-filter" className="sr-only">Action</label>
          <select
            id="audit-action-filter"
            value={actionFilter}
            onChange={(e) => setActionFilter(e.target.value)}
            className="h-10 rounded-md border border-[#334155] bg-[#0F172A] px-3 py-2 text-sm text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#22D3EE]"
          >
            <option>All</option>
            <option>CREATE</option>
            <option>UPDATE</option>
            <option>DELETE</option>
            <option>INQUIRE</option>
            <option>LOGIN</option>
            <option>LOGOUT</option>
            <option>STARTUP</option>
            <option>SHUTDOWN</option>
          </select>
        </div>
        <div className="flex flex-col gap-1">
          <label htmlFor="audit-status-filter" className="sr-only">Status</label>
          <select
            id="audit-status-filter"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="h-10 rounded-md border border-[#334155] bg-[#0F172A] px-3 py-2 text-sm text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#22D3EE]"
          >
            <option>All</option>
            <option>SUCC</option>
            <option>FAIL</option>
            <option>WARN</option>
          </select>
        </div>
        <div className="flex flex-col gap-1">
          <label htmlFor="audit-program-filter" className="sr-only">Program</label>
          <select
            id="audit-program-filter"
            value={programFilter}
            onChange={(e) => setProgramFilter(e.target.value)}
            className="h-10 rounded-md border border-[#334155] bg-[#0F172A] px-3 py-2 text-sm text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#22D3EE]"
          >
            <option>All</option>
            {programs.map((p) => (
              <option key={p}>{p}</option>
            ))}
          </select>
        </div>
        <div className="flex flex-col gap-1">
          <label htmlFor="audit-user-filter" className="sr-only">User ID</label>
          <Input
            id="audit-user-filter"
            placeholder="User ID..."
            value={userIdFilter}
            onChange={(e) => setUserIdFilter(e.target.value)}
            className="w-32"
          />
        </div>
        <div className="flex flex-col gap-1">
          <label htmlFor="audit-search" className="sr-only">Search</label>
          <Input
            id="audit-search"
            placeholder="Search all fields..."
            value={searchText}
            onChange={(e) => setSearchText(e.target.value)}
            className="w-48"
          />
        </div>
        {hasFilters && (
          <Button variant="ghost" size="sm" onClick={clearFilters}>
            Clear Filters
          </Button>
        )}
      </div>

      {/* Audit Log Table */}
      <DataTable<AuditReportEntry>
        columns={columns}
        data={filteredData}
        pageSize={10}
        expandableRow={(row) => (
          <div className="text-sm text-[#CBD5E1]">
            <span className="font-medium text-[#94A3B8]">Full Message: </span>
            {row.message}
          </div>
        )}
        rowClassName={(row) =>
          cn(
            row.status === 'FAIL' && 'bg-[#F87171]/5',
            row.status === 'WARN' && 'bg-amber-500/5'
          )
        }
        getRowKey={(row, i) => `${row.timestamp}-${i}`}
      />
    </div>
  );
}
