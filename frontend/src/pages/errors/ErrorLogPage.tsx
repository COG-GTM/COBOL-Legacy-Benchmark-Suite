import { useEffect, useMemo, useState } from 'react';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { PageHeader } from '@/components/ui/PageHeader';
import { LoadingSpinner } from '@/components/ui/LoadingSpinner';
import { EmptyState } from '@/components/ui/EmptyState';
import { StatusBadge, getSeverityVariant } from '@/components/ui/StatusBadge';
import { errorEntries } from '@/data/mockData';
import type { ErrorEntry } from '@/data/types';

type ErrorRow = ErrorEntry & Record<string, unknown>;

export function ErrorLogPage() {
  const [loading, setLoading] = useState(true);
  const [programFilter, setProgramFilter] = useState('ALL');
  const [severityFilter, setSeverityFilter] = useState('ALL');

  useEffect(() => {
    const timer = setTimeout(() => setLoading(false), 400);
    return () => clearTimeout(timer);
  }, []);

  const programs = useMemo(
    () => [...new Set(errorEntries.map((e) => e.program))].sort(),
    [],
  );

  const filteredEntries = useMemo(
    () =>
      errorEntries.filter(
        (e) =>
          (programFilter === 'ALL' || e.program === programFilter) &&
          (severityFilter === 'ALL' || e.severity === severityFilter),
      ),
    [programFilter, severityFilter],
  );

  const columns: Column<ErrorRow>[] = [
    {
      key: 'timestamp',
      header: 'Timestamp',
      sortable: true,
      render: (e) => <span className="font-mono text-xs">{e.timestamp}</span>,
    },
    {
      key: 'code',
      header: 'Code',
      sortable: true,
      render: (e) => <span className="font-mono font-medium text-slate-900">{e.code}</span>,
    },
    {
      key: 'severity',
      header: 'Severity',
      sortable: true,
      render: (e) => <StatusBadge label={e.severity} variant={getSeverityVariant(e.severity)} />,
    },
    {
      key: 'program',
      header: 'Program',
      sortable: true,
      render: (e) => <span className="font-mono">{e.program}</span>,
    },
    {
      key: 'paragraph',
      header: 'Paragraph',
      render: (e) => <span className="font-mono text-xs">{e.paragraph}</span>,
    },
    {
      key: 'respCode',
      header: 'RESP',
      render: (e) => <span className="font-mono text-xs">{e.respCode}</span>,
    },
    {
      key: 'respCode2',
      header: 'RESP2',
      render: (e) => <span className="font-mono text-xs">{e.respCode2}</span>,
    },
    {
      key: 'description',
      header: 'Message',
      render: (e) => <span className="text-slate-700">{e.description}</span>,
    },
    {
      key: 'action',
      header: 'Recommended Action',
      render: (e) => <span className="text-slate-500">{e.action}</span>,
    },
  ];

  if (loading) {
    return (
      <div>
        <PageHeader title="Error Log" description="Review system errors and warnings captured by the error handler" />
        <LoadingSpinner size="lg" message="Loading error log..." />
      </div>
    );
  }

  const selectClasses =
    'px-3 py-2 text-sm border border-slate-300 rounded-lg bg-white text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500';

  return (
    <div>
      <PageHeader title="Error Log" description="Review system errors and warnings captured by the error handler" />

      <Card>
        <div className="flex flex-col sm:flex-row gap-3 mb-4">
          <div>
            <label htmlFor="program-filter" className="block text-xs font-medium text-slate-500 mb-1">
              Program
            </label>
            <select
              id="program-filter"
              value={programFilter}
              onChange={(e) => setProgramFilter(e.target.value)}
              className={selectClasses}
            >
              <option value="ALL">All Programs</option>
              {programs.map((p) => (
                <option key={p} value={p}>
                  {p}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label htmlFor="severity-filter" className="block text-xs font-medium text-slate-500 mb-1">
              Severity
            </label>
            <select
              id="severity-filter"
              value={severityFilter}
              onChange={(e) => setSeverityFilter(e.target.value)}
              className={selectClasses}
            >
              <option value="ALL">All Severities</option>
              <option value="Error">Error</option>
              <option value="Warning">Warning</option>
            </select>
          </div>
        </div>

        {filteredEntries.length === 0 ? (
          <EmptyState
            title="No errors found"
            message="No error entries match the selected filters."
          />
        ) : (
          <div className="-mx-6 -mb-6">
            <DataTable
              columns={columns}
              data={filteredEntries as ErrorRow[]}
              keyExtractor={(e) => `${e.timestamp}-${e.program}-${e.code}`}
              emptyMessage="No errors found"
            />
          </div>
        )}
      </Card>
    </div>
  );
}
