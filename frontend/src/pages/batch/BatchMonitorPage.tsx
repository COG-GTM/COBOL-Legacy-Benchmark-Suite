import { useEffect, useMemo, useState } from 'react';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { PageHeader } from '@/components/ui/PageHeader';
import { LoadingSpinner } from '@/components/ui/LoadingSpinner';
import { StatusBadge, getBatchStatusVariant, getBatchStatusLabel } from '@/components/ui/StatusBadge';
import { batchJobs } from '@/data/mockData';
import type { BatchJob } from '@/data/types';

type BatchJobRow = BatchJob & Record<string, unknown>;

export function BatchMonitorPage() {
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const timer = setTimeout(() => setLoading(false), 400);
    return () => clearTimeout(timer);
  }, []);

  const summary = useMemo(() => {
    const completed = batchJobs.filter((j) => j.status === 'C').length;
    const processing = batchJobs.filter((j) => j.status === 'P').length;
    const waiting = batchJobs.filter((j) => j.status === 'W').length;
    const errors = batchJobs.filter((j) => j.status === 'E').length;
    return { completed, processing, waiting, errors };
  }, []);

  const columns: Column<BatchJobRow>[] = [
    {
      key: 'processId',
      header: 'Job Name',
      sortable: true,
      render: (job) => <span className="font-mono font-medium text-slate-900">{job.processId}</span>,
    },
    {
      key: 'status',
      header: 'Status',
      sortable: true,
      render: (job) => (
        <StatusBadge label={getBatchStatusLabel(job.status)} variant={getBatchStatusVariant(job.status)} />
      ),
    },
    {
      key: 'checkpointId',
      header: 'Checkpoint ID',
      render: (job) =>
        job.checkpointId ? <span className="font-mono text-xs">{job.checkpointId}</span> : <span className="text-slate-400">—</span>,
    },
    {
      key: 'checkpointCount',
      header: 'Checkpoints',
      sortable: true,
      render: (job) => (job.checkpointId ? job.checkpointCount.toLocaleString() : '—'),
      className: 'text-right',
    },
    {
      key: 'restartFlag',
      header: 'Restarted',
      render: (job) =>
        job.restartFlag === 'Y' ? (
          <StatusBadge label="Restarted" variant="warning" />
        ) : (
          <span className="text-slate-400">No</span>
        ),
    },
    {
      key: 'lastCheckpointTime',
      header: 'Last Checkpoint',
      render: (job) => job.lastCheckpointTime || '—',
    },
    {
      key: 'processDate',
      header: 'Run Date',
      sortable: true,
    },
    {
      key: 'startTime',
      header: 'Start',
      render: (job) => job.startTime || '—',
    },
    {
      key: 'endTime',
      header: 'End',
      render: (job) => job.endTime || '—',
    },
    {
      key: 'recordCount',
      header: 'Records',
      sortable: true,
      render: (job) => job.recordCount.toLocaleString(),
      className: 'text-right',
    },
    {
      key: 'errorCount',
      header: 'Errors',
      sortable: true,
      render: (job) => (
        <span className={job.errorCount > 0 ? 'text-red-600 font-medium' : ''}>{job.errorCount.toLocaleString()}</span>
      ),
      className: 'text-right',
    },
    {
      key: 'returnCode',
      header: 'RC',
      render: (job) =>
        job.returnCode ? <span className="font-mono text-xs">{job.returnCode}</span> : <span className="text-slate-400">—</span>,
    },
    {
      key: 'message',
      header: 'Message',
      render: (job) => <span className="text-slate-600">{job.message}</span>,
    },
  ];

  const summaryCards = [
    { label: 'Completed', value: summary.completed, color: 'text-emerald-600 bg-emerald-50' },
    { label: 'Processing', value: summary.processing, color: 'text-blue-600 bg-blue-50' },
    { label: 'Waiting', value: summary.waiting, color: 'text-slate-600 bg-slate-100' },
    { label: 'Errors', value: summary.errors, color: 'text-red-600 bg-red-50' },
  ];

  if (loading) {
    return (
      <div>
        <PageHeader title="Batch Monitor" description="Monitor batch job execution, checkpoint, and restart status" />
        <LoadingSpinner size="lg" message="Loading batch jobs..." />
      </div>
    );
  }

  return (
    <div>
      <PageHeader title="Batch Monitor" description="Monitor batch job execution, checkpoint, and restart status" />

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        {summaryCards.map((card) => (
          <div key={card.label} className="bg-white rounded-lg border border-slate-200 shadow-sm p-4">
            <p className="text-sm font-medium text-slate-500">{card.label}</p>
            <p className={`text-2xl font-bold mt-1 inline-block px-2 rounded ${card.color}`}>{card.value}</p>
          </div>
        ))}
      </div>

      <Card>
        <div className="-m-6">
          <DataTable
            columns={columns}
            data={batchJobs as BatchJobRow[]}
            keyExtractor={(job) => `${job.processDate}-${job.processId}`}
            emptyMessage="No batch jobs found"
          />
        </div>
      </Card>
    </div>
  );
}
