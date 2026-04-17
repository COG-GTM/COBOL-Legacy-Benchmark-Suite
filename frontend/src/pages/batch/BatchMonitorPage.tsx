import { useState, useMemo } from 'react';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card } from '@/components/ui/Card';
import { StatusBadge, getBatchStatusVariant, getBatchStatusLabel } from '@/components/ui/StatusBadge';
import { batchJobs } from '@/data/mockData';
import { RefreshCw } from 'lucide-react';

interface JobMeta {
  processId: string;
  description: string;
  timeWindow: string;
  dependencies: string[];
}

const JOB_METADATA: JobMeta[] = [
  { processId: 'TRNVAL00', description: 'Transaction Validation \u2014 validates incoming transaction batches for data integrity', timeWindow: '18:00-18:15', dependencies: [] },
  { processId: 'POSUPD00', description: 'Position Update \u2014 updates investment positions in VSAM Master File', timeWindow: '18:15-19:00', dependencies: ['TRNVAL00'] },
  { processId: 'HISTLD00', description: 'History Load \u2014 ETL from VSAM/sequential files into DB2 history tables', timeWindow: '19:00-19:30', dependencies: ['POSUPD00'] },
  { processId: 'RPTPOS00', description: 'Position Report \u2014 generates daily position report from current holdings', timeWindow: '19:30-20:00', dependencies: ['HISTLD00'] },
  { processId: 'RPTAUD00', description: 'Audit Report \u2014 generates system audit trail and compliance report', timeWindow: '19:30-20:00', dependencies: ['HISTLD00'] },
  { processId: 'RPTSTA00', description: 'Statistics Report \u2014 generates system performance and processing statistics', timeWindow: '19:30-20:00', dependencies: ['HISTLD00'] },
  { processId: 'UTLMNT00', description: 'File Maintenance \u2014 performs VSAM and DB2 data cleanup and archiving', timeWindow: '20:00-20:30', dependencies: ['RPTPOS00', 'RPTAUD00', 'RPTSTA00'] },
  { processId: 'UTLMON00', description: 'System Monitor \u2014 monitors system health and resource utilization', timeWindow: '20:00-20:30', dependencies: ['RPTPOS00', 'RPTAUD00', 'RPTSTA00'] },
];

const FLOW_STAGES: { label: string; jobs: string[] }[] = [
  { label: 'Validation', jobs: ['TRNVAL00'] },
  { label: 'Processing', jobs: ['POSUPD00'] },
  { label: 'History', jobs: ['HISTLD00'] },
  { label: 'Reports', jobs: ['RPTPOS00', 'RPTAUD00', 'RPTSTA00'] },
  { label: 'Utilities', jobs: ['UTLMNT00', 'UTLMON00'] },
];

const SCHEDULE_ENTRIES = [
  { job: 'TRNVAL00', start: '18:00', end: '18:15' },
  { job: 'POSUPD00', start: '18:15', end: '19:00' },
  { job: 'HISTLD00', start: '19:00', end: '19:30' },
  { job: 'Reports', start: '19:30', end: '20:00' },
  { job: 'Utilities', start: '20:00', end: '20:30' },
];

function calcDuration(start: string, end: string): string {
  if (!start || !end) return '\u2014';
  const [sh, sm, ss] = start.split(':').map(Number);
  const [eh, em, es] = end.split(':').map(Number);
  const seconds = (eh * 3600 + em * 60 + es) - (sh * 3600 + sm * 60 + ss);
  if (seconds <= 0) return '\u2014';
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${m}m ${s}s`;
}

function statusDotClass(status: string): string {
  switch (status) {
    case 'C': return 'bg-emerald-500';
    case 'P': return 'bg-blue-500 animate-pulse';
    case 'E': return 'bg-red-500';
    case 'W':
    default: return 'bg-slate-300';
  }
}

export function BatchMonitorPage() {
  const [autoRefresh, setAutoRefresh] = useState(false);

  const jobMap = useMemo(() => {
    const map = new Map<string, typeof batchJobs[number]>();
    batchJobs.forEach((j) => map.set(j.processId, j));
    return map;
  }, []);

  const activeStageIdx = useMemo(() => {
    for (let i = FLOW_STAGES.length - 1; i >= 0; i--) {
      const stage = FLOW_STAGES[i];
      if (stage.jobs.some((jId) => jobMap.get(jId)?.status === 'P')) return i;
      if (stage.jobs.some((jId) => jobMap.get(jId)?.status === 'C')) return i;
    }
    return 0;
  }, [jobMap]);

  const lastCompleted = useMemo(() => {
    const completed = batchJobs.filter((j) => j.status === 'C');
    if (completed.length === 0) return null;
    return completed[completed.length - 1];
  }, []);

  return (
    <div>
      <PageHeader
        title="Batch Job Monitor"
        description="Modernized from COBOL programs BCHCTL00 (Batch Control) and UTLMON00 (System Monitor)"
        actions={
          <div className="flex items-center gap-3">
            <label className="inline-flex items-center gap-2 text-sm text-slate-600 cursor-pointer">
              <div
                role="switch"
                aria-checked={autoRefresh}
                onClick={() => setAutoRefresh(!autoRefresh)}
                className={`relative inline-flex h-5 w-9 items-center rounded-full transition-colors ${autoRefresh ? 'bg-blue-600' : 'bg-slate-300'}`}
              >
                <span className={`inline-block h-3.5 w-3.5 rounded-full bg-white transition-transform ${autoRefresh ? 'translate-x-4.5' : 'translate-x-0.5'}`} />
              </div>
              <RefreshCw className={`w-4 h-4 ${autoRefresh ? 'animate-spin' : ''}`} />
              Auto-refresh
            </label>
          </div>
        }
      />

      {/* Job Dependency Flow */}
      <Card title="Execution Flow" className="mb-6">
        <div className="flex items-center justify-between overflow-x-auto pb-2">
          {FLOW_STAGES.map((stage, idx) => {
            const isActive = idx === activeStageIdx;
            const isPast = idx < activeStageIdx;
            const stageJobs = stage.jobs.map((jId) => jobMap.get(jId));
            const hasError = stageJobs.some((j) => j?.status === 'E');

            return (
              <div key={stage.label} className="flex items-center">
                <div
                  className={`flex flex-col items-center px-4 py-3 rounded-lg border-2 min-w-[120px] transition-all ${
                    hasError
                      ? 'border-red-400 bg-red-50'
                      : isActive
                      ? 'border-blue-500 bg-blue-50 ring-2 ring-blue-200'
                      : isPast
                      ? 'border-emerald-400 bg-emerald-50'
                      : 'border-slate-200 bg-slate-50'
                  }`}
                >
                  <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">{stage.label}</span>
                  <div className="flex gap-1 mt-1">
                    {stage.jobs.map((jId) => {
                      const job = jobMap.get(jId);
                      return (
                        <div key={jId} className="flex items-center gap-1">
                          <span className={`w-2.5 h-2.5 rounded-full ${statusDotClass(job?.status ?? 'W')}`} />
                          <span className="text-[10px] font-mono text-slate-600">{jId}</span>
                        </div>
                      );
                    })}
                  </div>
                </div>
                {idx < FLOW_STAGES.length - 1 && (
                  <div className="mx-2 flex-shrink-0">
                    <svg width="24" height="16" viewBox="0 0 24 16" fill="none">
                      <path d="M0 8h20M16 2l6 6-6 6" stroke={isPast ? '#10b981' : '#cbd5e1'} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                    </svg>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </Card>

      {/* Job Schedule Timeline */}
      <Card title="Job Schedule" className="mb-6">
        <div className="space-y-2">
          {SCHEDULE_ENTRIES.map((entry) => {
            const startMin = parseInt(entry.start.split(':')[0]) * 60 + parseInt(entry.start.split(':')[1]);
            const endMin = parseInt(entry.end.split(':')[0]) * 60 + parseInt(entry.end.split(':')[1]);
            const totalRange = 150;
            const baseMin = 18 * 60;
            const leftPct = ((startMin - baseMin) / totalRange) * 100;
            const widthPct = ((endMin - startMin) / totalRange) * 100;

            return (
              <div key={entry.job} className="flex items-center gap-3">
                <span className="text-xs font-mono text-slate-600 w-20 text-right flex-shrink-0">{entry.job}</span>
                <div className="flex-1 relative h-7 bg-slate-100 rounded">
                  <div
                    className="absolute top-0.5 bottom-0.5 bg-blue-200 border border-blue-400 rounded text-[10px] flex items-center justify-center font-mono text-blue-700 overflow-hidden"
                    style={{ left: `${leftPct}%`, width: `${widthPct}%` }}
                  >
                    {entry.start}\u2013{entry.end}
                  </div>
                </div>
              </div>
            );
          })}
          <div className="flex items-center gap-3 pt-1">
            <span className="w-20 flex-shrink-0" />
            <div className="flex-1 flex justify-between text-[10px] font-mono text-slate-400 px-1">
              <span>18:00</span>
              <span>18:30</span>
              <span>19:00</span>
              <span>19:30</span>
              <span>20:00</span>
              <span>20:30</span>
            </div>
          </div>
        </div>
      </Card>

      {/* Job Status Table */}
      <Card title="Job Status" className="mb-6">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-slate-200">
            <thead className="bg-slate-50">
              <tr>
                <th className="px-3 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Job</th>
                <th className="px-3 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Description</th>
                <th className="px-3 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Status</th>
                <th className="px-3 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Start</th>
                <th className="px-3 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">End</th>
                <th className="px-3 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Duration</th>
                <th className="px-3 py-3 text-right text-xs font-semibold text-slate-600 uppercase tracking-wider">Records</th>
                <th className="px-3 py-3 text-right text-xs font-semibold text-slate-600 uppercase tracking-wider">Errors</th>
                <th className="px-3 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">RC</th>
                <th className="px-3 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Message</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-slate-200">
              {JOB_METADATA.map((meta) => {
                const job = jobMap.get(meta.processId);
                return (
                  <tr key={meta.processId} className="hover:bg-slate-50 transition-colors">
                    <td className="px-3 py-2 text-sm font-mono font-medium text-slate-800">{meta.processId}</td>
                    <td className="px-3 py-2 text-xs text-slate-600 max-w-xs">{meta.description}</td>
                    <td className="px-3 py-2 text-xs">
                      <div className="flex items-center gap-1.5">
                        <span className={`w-2.5 h-2.5 rounded-full flex-shrink-0 ${statusDotClass(job?.status ?? 'W')}`} />
                        <StatusBadge label={getBatchStatusLabel(job?.status ?? 'W')} variant={getBatchStatusVariant(job?.status ?? 'W')} />
                      </div>
                    </td>
                    <td className="px-3 py-2 text-xs font-mono text-slate-600">{job?.startTime || '\u2014'}</td>
                    <td className="px-3 py-2 text-xs font-mono text-slate-600">{job?.endTime || '\u2014'}</td>
                    <td className="px-3 py-2 text-xs font-mono text-slate-600">{calcDuration(job?.startTime ?? '', job?.endTime ?? '')}</td>
                    <td className="px-3 py-2 text-xs text-slate-700 text-right tabular-nums">{job?.recordCount ? job.recordCount.toLocaleString() : '\u2014'}</td>
                    <td className={`px-3 py-2 text-xs text-right tabular-nums ${(job?.errorCount ?? 0) > 0 ? 'text-red-600 font-medium' : 'text-slate-700'}`}>
                      {job?.errorCount !== undefined ? job.errorCount : '\u2014'}
                    </td>
                    <td className="px-3 py-2 text-xs font-mono text-slate-600">{job?.returnCode || '\u2014'}</td>
                    <td className="px-3 py-2 text-xs text-slate-600 max-w-xs truncate">{job?.message || '\u2014'}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </Card>

      {/* Checkpoint/Restart Info */}
      <Card title="Checkpoint / Restart Information">
        {lastCompleted ? (
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="bg-slate-50 rounded-lg p-4 text-center">
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Last Checkpoint</p>
              <p className="mt-1 text-sm font-mono font-bold text-slate-900">
                {lastCompleted.processDate} {lastCompleted.endTime}
              </p>
            </div>
            <div className="bg-slate-50 rounded-lg p-4 text-center">
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Records at Checkpoint</p>
              <p className="mt-1 text-lg font-bold text-slate-900">{lastCompleted.recordCount.toLocaleString()}</p>
            </div>
            <div className="bg-slate-50 rounded-lg p-4 text-center">
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Last Processed Job</p>
              <p className="mt-1 text-sm font-mono font-bold text-slate-900">{lastCompleted.processId}</p>
            </div>
          </div>
        ) : (
          <p className="text-sm text-slate-500 text-center py-4">No checkpoint data available.</p>
        )}
      </Card>
    </div>
  );
}
