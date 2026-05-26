import { useState, useMemo } from 'react';
import {
  Activity,
  CheckCircle,
  XCircle,
  Clock,
  Loader2,
  ChevronDown,
  ChevronRight,
  ArrowRight,
  Calendar,
  Info,
} from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge, getBatchStatusVariant, getBatchStatusLabel } from '@/components/ui/StatusBadge';
import { batchJobs, checkpointData } from '@/data/mockData';
import type { BatchJob } from '@/data/types';

const PIPELINE_STEPS = [
  { id: 'TRNVAL00', name: 'Transaction Validation', description: 'Validate incoming transactions against business rules' },
  { id: 'POSUPD00', name: 'Position Update', description: 'Update portfolio positions and cost basis' },
  { id: 'HISTLD00', name: 'History Load', description: 'Load transaction history from VSAM to DB2' },
  { id: 'RPTGEN00', name: 'Report Generation', description: 'Generate position, audit, and statistics reports' },
] as const;

const BATCH_SCHEDULE = [
  { id: 'TRNVAL00', name: 'Transaction Validation', window: '18:00 – 18:15' },
  { id: 'POSUPD00', name: 'Position Update', window: '18:15 – 19:00' },
  { id: 'HISTLD00', name: 'History Load', window: '19:00 – 19:30' },
  { id: 'RPTGEN00', name: 'Report Generation', window: '19:30 – 20:00' },
] as const;

function statusIcon(status: BatchJob['status']) {
  switch (status) {
    case 'C': return <CheckCircle className="w-5 h-5 text-emerald-500" />;
    case 'P': return <Loader2 className="w-5 h-5 text-blue-500 animate-spin" />;
    case 'E': return <XCircle className="w-5 h-5 text-red-500" />;
    case 'W': return <Clock className="w-5 h-5 text-slate-400" />;
  }
}

function statusRingColor(status: BatchJob['status']) {
  switch (status) {
    case 'C': return 'ring-emerald-400 bg-emerald-50';
    case 'P': return 'ring-blue-400 bg-blue-50';
    case 'E': return 'ring-red-400 bg-red-50';
    case 'W': return 'ring-slate-300 bg-slate-50';
  }
}

function connectorColor(status: BatchJob['status']) {
  switch (status) {
    case 'C': return 'bg-emerald-400';
    case 'P': return 'bg-blue-400';
    case 'E': return 'bg-red-400';
    case 'W': return 'bg-slate-300';
  }
}

function returnCodeColor(code: string): string {
  if (!code) return 'text-slate-400';
  const num = parseInt(code, 10);
  if (num === 0) return 'text-emerald-600 font-semibold';
  if (num <= 4) return 'text-amber-600 font-semibold';
  if (num <= 8) return 'text-orange-600 font-semibold';
  return 'text-red-600 font-semibold';
}

function calcDuration(start: string, end: string): string {
  if (!start || !end) return '—';
  const [sh, sm, ss] = start.split(':').map(Number);
  const [eh, em, es] = end.split(':').map(Number);
  const diff = (eh * 3600 + em * 60 + es) - (sh * 3600 + sm * 60 + ss);
  if (diff <= 0) return '—';
  const m = Math.floor(diff / 60);
  const s = diff % 60;
  return `${m}m ${s.toString().padStart(2, '0')}s`;
}

export function BatchMonitorPage() {
  const dates = useMemo(() => {
    const set = new Set(batchJobs.map((j) => j.processDate));
    return [...set].sort((a, b) => b.localeCompare(a));
  }, []);

  const [selectedDate, setSelectedDate] = useState(dates[0] ?? '');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [expandedRow, setExpandedRow] = useState<string | null>(null);

  const jobsForDate = useMemo(
    () => batchJobs.filter((j) => j.processDate === selectedDate),
    [selectedDate],
  );

  const pipelineJobs = useMemo(() => {
    const map = new Map(jobsForDate.map((j) => [j.processId, j]));
    return PIPELINE_STEPS.map((step) => ({ step, job: map.get(step.id) }));
  }, [jobsForDate]);

  const allJobs = useMemo(() => {
    let filtered = [...batchJobs].sort((a, b) => {
      const d = b.processDate.localeCompare(a.processDate);
      if (d !== 0) return d;
      return a.startTime.localeCompare(b.startTime);
    });
    if (statusFilter !== 'ALL') {
      filtered = filtered.filter((j) => j.status === statusFilter);
    }
    return filtered;
  }, [statusFilter]);

  const toggleRow = (key: string) => {
    setExpandedRow((prev) => (prev === key ? null : key));
  };

  return (
    <div>
      <PageHeader
        title="Batch Monitor"
        description="COBOL batch processing pipeline — BCHCTL00 operations dashboard"
        actions={
          <div className="flex items-center gap-2">
            <Activity className="w-5 h-5 text-blue-600" />
            <span className="text-sm font-medium text-slate-600">Process Date:</span>
            <select
              value={selectedDate}
              onChange={(e) => setSelectedDate(e.target.value)}
              className="text-sm border border-slate-300 rounded-lg px-3 py-1.5 bg-white text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              {dates.map((d) => (
                <option key={d} value={d}>{d}</option>
              ))}
            </select>
          </div>
        }
      />

      {/* Pipeline Visualization */}
      <Card title="Batch Pipeline" className="mb-6">
        <div className="flex flex-col lg:flex-row items-stretch lg:items-center gap-2 lg:gap-0">
          {pipelineJobs.map(({ step, job }, i) => (
            <div key={step.id} className="flex items-center flex-1 min-w-0">
              <div
                className={`flex-1 rounded-lg ring-2 p-4 ${statusRingColor(job?.status ?? 'W')}`}
              >
                <div className="flex items-center gap-2 mb-1">
                  {statusIcon(job?.status ?? 'W')}
                  <span className="text-sm font-semibold text-slate-900 truncate">{step.name}</span>
                </div>
                <p className="text-xs text-slate-500 mb-2 line-clamp-2">{step.description}</p>
                <div className="flex items-center justify-between text-xs">
                  <StatusBadge label={getBatchStatusLabel(job?.status ?? 'W')} variant={getBatchStatusVariant(job?.status ?? 'W')} />
                  <span className="text-slate-500 font-mono">
                    {job?.startTime && job?.endTime ? calcDuration(job.startTime, job.endTime) : job?.startTime ? 'Running...' : '—'}
                  </span>
                </div>
              </div>
              {i < pipelineJobs.length - 1 && (
                <div className="hidden lg:flex items-center px-1">
                  <div className={`h-0.5 w-6 ${connectorColor(job?.status ?? 'W')}`} />
                  <ArrowRight className={`w-4 h-4 shrink-0 ${job?.status === 'C' ? 'text-emerald-400' : job?.status === 'E' ? 'text-red-400' : 'text-slate-300'}`} />
                </div>
              )}
            </div>
          ))}
        </div>
      </Card>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6 mb-6">
        {/* Batch Jobs Table */}
        <div className="lg:col-span-3">
          <Card
            title="Batch Jobs"
            actions={
              <select
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
                className="text-sm border border-slate-300 rounded-lg px-3 py-1.5 bg-white text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="ALL">All Statuses</option>
                <option value="C">Completed</option>
                <option value="P">In Process</option>
                <option value="W">Waiting</option>
                <option value="E">Error</option>
              </select>
            }
          >
            <div className="overflow-x-auto -m-6 mt-0">
              <table className="min-w-full divide-y divide-slate-200">
                <thead className="bg-slate-50">
                  <tr>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase w-8" />
                    <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase">Date</th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase">Process ID</th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase">Status</th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase">Start</th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase">End</th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase">Duration</th>
                    <th className="px-4 py-3 text-right text-xs font-semibold text-slate-600 uppercase">Records</th>
                    <th className="px-4 py-3 text-right text-xs font-semibold text-slate-600 uppercase">Errors</th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase">RC</th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase">Message</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200">
                  {allJobs.map((job) => {
                    const key = `${job.processDate}-${job.processId}`;
                    const isExpanded = expandedRow === key;
                    const cp = checkpointData.find(
                      (c) => c.processDate === job.processDate && c.processId === job.processId,
                    );
                    return (
                      <JobRow
                        key={key}
                        job={job}
                        isExpanded={isExpanded}
                        onToggle={() => toggleRow(key)}
                        checkpoint={cp}
                      />
                    );
                  })}
                </tbody>
              </table>
            </div>
          </Card>
        </div>

        {/* Batch Schedule Reference */}
        <div>
          <Card title="Batch Schedule">
            <div className="space-y-3">
              {BATCH_SCHEDULE.map((s) => (
                <div key={s.id} className="flex items-start gap-3">
                  <div className="p-1.5 rounded bg-slate-100 mt-0.5">
                    <Calendar className="w-3.5 h-3.5 text-slate-500" />
                  </div>
                  <div className="min-w-0">
                    <p className="text-sm font-medium text-slate-900 truncate">{s.name}</p>
                    <p className="text-xs text-slate-500 font-mono">{s.id}</p>
                    <p className="text-xs text-slate-600 mt-0.5">{s.window}</p>
                  </div>
                </div>
              ))}
            </div>
            <div className="mt-4 pt-4 border-t border-slate-200 flex items-start gap-2 text-xs text-slate-500">
              <Info className="w-3.5 h-3.5 mt-0.5 shrink-0" />
              <span>Standard nightly batch window: 18:00 – 20:00 ET. Jobs execute sequentially; downstream jobs depend on successful upstream completion.</span>
            </div>
          </Card>
        </div>
      </div>
    </div>
  );
}

function JobRow({
  job,
  isExpanded,
  onToggle,
  checkpoint,
}: {
  job: BatchJob;
  isExpanded: boolean;
  onToggle: () => void;
  checkpoint?: (typeof checkpointData)[number];
}) {
  const schedule = BATCH_SCHEDULE.find((s) => s.id === job.processId);
  return (
    <>
      <tr
        className="hover:bg-slate-50 transition-colors cursor-pointer"
        onClick={onToggle}
      >
        <td className="px-4 py-3 text-slate-400">
          {isExpanded ? <ChevronDown className="w-4 h-4" /> : <ChevronRight className="w-4 h-4" />}
        </td>
        <td className="px-4 py-3 text-sm text-slate-700 whitespace-nowrap">{job.processDate}</td>
        <td className="px-4 py-3 text-sm font-mono text-slate-900 whitespace-nowrap">{job.processId}</td>
        <td className="px-4 py-3">
          <StatusBadge label={getBatchStatusLabel(job.status)} variant={getBatchStatusVariant(job.status)} />
        </td>
        <td className="px-4 py-3 text-sm font-mono text-slate-600 whitespace-nowrap">{job.startTime || '—'}</td>
        <td className="px-4 py-3 text-sm font-mono text-slate-600 whitespace-nowrap">{job.endTime || '—'}</td>
        <td className="px-4 py-3 text-sm font-mono text-slate-600 whitespace-nowrap">{calcDuration(job.startTime, job.endTime)}</td>
        <td className="px-4 py-3 text-sm text-slate-700 text-right whitespace-nowrap">{job.recordCount.toLocaleString()}</td>
        <td className="px-4 py-3 text-sm text-right whitespace-nowrap">
          <span className={job.errorCount > 0 ? 'text-red-600 font-semibold' : 'text-slate-500'}>{job.errorCount}</span>
        </td>
        <td className="px-4 py-3 text-sm font-mono whitespace-nowrap">
          <span className={returnCodeColor(job.returnCode)}>{job.returnCode || '—'}</span>
        </td>
        <td className="px-4 py-3 text-sm text-slate-600 max-w-xs truncate">{job.message}</td>
      </tr>
      {isExpanded && (
        <tr className="bg-slate-50">
          <td colSpan={11} className="px-6 py-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 text-sm">
              {/* Checkpoint Info */}
              <div>
                <h4 className="font-semibold text-slate-900 mb-2">Checkpoint Info</h4>
                {checkpoint ? (
                  <dl className="grid grid-cols-2 gap-x-4 gap-y-1 text-slate-600">
                    <dt className="text-slate-500">Records Processed</dt>
                    <dd className="font-mono">{checkpoint.recordsProcessed.toLocaleString()}</dd>
                    <dt className="text-slate-500">Last Transaction</dt>
                    <dd className="font-mono">{checkpoint.lastTransId || '—'}</dd>
                    <dt className="text-slate-500">Last Account</dt>
                    <dd className="font-mono">{checkpoint.lastAccount || '—'}</dd>
                    <dt className="text-slate-500">Last Fund</dt>
                    <dd className="font-mono">{checkpoint.lastFund || '—'}</dd>
                    <dt className="text-slate-500">Checkpoint Time</dt>
                    <dd className="font-mono">{checkpoint.timestamp}</dd>
                  </dl>
                ) : (
                  <p className="text-slate-400 italic">No checkpoint data available</p>
                )}
              </div>
              {/* Processing Window */}
              <div>
                <h4 className="font-semibold text-slate-900 mb-2">Processing Window</h4>
                <dl className="grid grid-cols-2 gap-x-4 gap-y-1 text-slate-600">
                  <dt className="text-slate-500">Expected</dt>
                  <dd className="font-mono">{schedule?.window ?? '—'}</dd>
                  <dt className="text-slate-500">Actual Start</dt>
                  <dd className="font-mono">{job.startTime || '—'}</dd>
                  <dt className="text-slate-500">Actual End</dt>
                  <dd className="font-mono">{job.endTime || '—'}</dd>
                  <dt className="text-slate-500">Duration</dt>
                  <dd className="font-mono">{calcDuration(job.startTime, job.endTime)}</dd>
                </dl>
                {job.status === 'E' && (
                  <div className="mt-3 p-3 rounded-lg bg-red-50 border border-red-200 text-red-700 text-xs">
                    <strong>Error Detail:</strong> {job.message}
                    {job.returnCode && <span className="ml-2 font-mono">(RC={job.returnCode})</span>}
                  </div>
                )}
              </div>
            </div>
          </td>
        </tr>
      )}
    </>
  );
}
