import { useState, useEffect } from 'react';
import { Terminal, Clock, CheckCircle2, XCircle, Loader2, CalendarClock } from 'lucide-react';
import { batchJobs } from '../data/mockData';
import StatusBadge, { getBatchStatusVariant } from '../components/StatusBadge';
import { formatDate, cn } from '../lib/format';
import type { BatchJob } from '../types';

const statusIcons = {
  Running: Loader2,
  Completed: CheckCircle2,
  Failed: XCircle,
  Scheduled: CalendarClock,
};

const cobolModernMapping = [
  { cobol: 'TRNVAL00', modern: 'TransactionValidationService', desc: 'Validates incoming transactions' },
  { cobol: 'POSUPDT', modern: 'PositionUpdateService', desc: 'Updates portfolio positions' },
  { cobol: 'HISTLD00', modern: 'HistoryLoadService', desc: 'Loads transaction history to DB2' },
  { cobol: 'RPTPOS00', modern: 'PositionReportService', desc: 'Generates position reports' },
  { cobol: 'RPTAUD00', modern: 'AuditReportService', desc: 'Generates audit trail reports' },
  { cobol: 'BCHCTL00', modern: 'BatchControlService', desc: 'Batch job orchestration' },
  { cobol: 'INQONLN', modern: 'PortfolioInquiryController', desc: 'Online portfolio inquiry' },
  { cobol: 'INQHIST', modern: 'TransactionHistoryController', desc: 'Transaction history inquiry' },
  { cobol: 'SECMGR', modern: 'AuthenticationService', desc: 'User authentication & authorization' },
  { cobol: 'UTLMNT00', modern: 'MaintenanceService', desc: 'File and database maintenance' },
];

export default function BatchMonitor() {
  const [jobs, setJobs] = useState<BatchJob[]>(batchJobs);

  useEffect(() => {
    const timer = setInterval(() => {
      setJobs(prev =>
        prev.map(job => {
          if (job.status !== 'Running') return job;
          const increment = Math.floor(Math.random() * 200) + 50;
          const newProcessed = Math.min(job.recordsProcessed + increment, job.totalRecords);
          if (newProcessed >= job.totalRecords) {
            return {
              ...job,
              recordsProcessed: job.totalRecords,
              status: 'Completed' as const,
              endTime: new Date().toISOString(),
              returnCode: 0,
            };
          }
          return { ...job, recordsProcessed: newProcessed };
        })
      );
    }, 2000);

    return () => clearInterval(timer);
  }, []);

  const completed = jobs.filter(j => j.status === 'Completed').length;
  const running = jobs.filter(j => j.status === 'Running').length;
  const failed = jobs.filter(j => j.status === 'Failed').length;

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold text-text-primary">Batch Monitor</h2>
        <p className="text-text-secondary text-sm mt-1">
          Track batch job execution and COBOL-to-modern mapping
        </p>
      </div>

      {/* Status summary */}
      <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
        <div className="bg-surface rounded-xl border border-border p-4">
          <p className="text-sm text-text-muted">Total Jobs</p>
          <p className="text-xl font-bold mt-1 text-text-primary">{jobs.length}</p>
        </div>
        <div className="bg-surface rounded-xl border border-border p-4">
          <div className="flex items-center gap-2">
            <CheckCircle2 size={16} className="text-gain" />
            <p className="text-sm text-text-muted">Completed</p>
          </div>
          <p className="text-xl font-bold mt-1 text-gain">{completed}</p>
        </div>
        <div className="bg-surface rounded-xl border border-border p-4">
          <div className="flex items-center gap-2">
            <Loader2 size={16} className="text-info animate-spin" />
            <p className="text-sm text-text-muted">Running</p>
          </div>
          <p className="text-xl font-bold mt-1 text-info">{running}</p>
        </div>
        <div className="bg-surface rounded-xl border border-border p-4">
          <div className="flex items-center gap-2">
            <XCircle size={16} className="text-loss" />
            <p className="text-sm text-text-muted">Failed</p>
          </div>
          <p className="text-xl font-bold mt-1 text-loss">{failed}</p>
        </div>
      </div>

      {/* Job List */}
      <div className="bg-surface rounded-xl border border-border overflow-hidden">
        <div className="px-5 py-4 border-b border-border flex items-center gap-2">
          <Terminal size={18} className="text-text-muted" />
          <h3 className="font-semibold text-text-primary">Batch Jobs</h3>
        </div>
        <div className="divide-y divide-border">
          {jobs.map(job => {
            const Icon = statusIcons[job.status];
            const progress = job.totalRecords > 0
              ? Math.round((job.recordsProcessed / job.totalRecords) * 100)
              : 0;

            return (
              <div key={job.jobId} className="px-5 py-4 hover:bg-surface-alt/50 transition-colors">
                <div className="flex items-start justify-between mb-2">
                  <div className="flex items-center gap-3">
                    <Icon
                      size={18}
                      className={cn(
                        job.status === 'Running' && 'text-info animate-spin',
                        job.status === 'Completed' && 'text-gain',
                        job.status === 'Failed' && 'text-loss',
                        job.status === 'Scheduled' && 'text-text-muted'
                      )}
                    />
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-mono text-sm font-semibold text-text-primary">{job.programName}</span>
                        <StatusBadge label={job.status} variant={getBatchStatusVariant(job.status)} />
                      </div>
                      <p className="text-xs text-text-muted mt-0.5">{job.description}</p>
                    </div>
                  </div>
                  <div className="text-right text-xs text-text-muted">
                    <div className="flex items-center gap-1">
                      <Clock size={12} />
                      <span>{formatDate(job.startTime)}</span>
                    </div>
                    {job.returnCode > 0 && (
                      <p className="text-loss font-mono mt-1">RC={job.returnCode}</p>
                    )}
                  </div>
                </div>

                {(job.status === 'Running' || job.status === 'Completed' || job.status === 'Failed') && (
                  <div className="ml-[30px]">
                    <div className="flex items-center justify-between text-xs text-text-muted mb-1">
                      <span>{job.recordsProcessed.toLocaleString()} / {job.totalRecords.toLocaleString()} records</span>
                      <span>{progress}%</span>
                    </div>
                    <div className="w-full h-1.5 bg-surface-secondary rounded-full overflow-hidden">
                      <div
                        className={cn(
                          'h-full rounded-full transition-all duration-500',
                          job.status === 'Failed' ? 'bg-loss' : job.status === 'Running' ? 'bg-accent-1' : 'bg-gain'
                        )}
                        style={{ width: `${progress}%` }}
                      />
                    </div>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </div>

      {/* COBOL Mapping */}
      <div className="bg-surface rounded-xl border border-border overflow-hidden">
        <div className="px-5 py-4 border-b border-border">
          <h3 className="font-semibold text-text-primary">COBOL &rarr; Modern Service Mapping</h3>
          <p className="text-xs text-text-muted mt-1">
            Reference mapping from legacy COBOL programs to modernized services
          </p>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-surface border-b border-border">
                <th className="text-left py-3 px-4 text-text-heading font-medium text-xs uppercase tracking-wider">COBOL Program</th>
                <th className="text-left py-3 px-4 text-text-heading font-medium text-xs uppercase tracking-wider">Modern Service</th>
                <th className="text-left py-3 px-4 text-text-heading font-medium text-xs uppercase tracking-wider">Description</th>
              </tr>
            </thead>
            <tbody>
              {cobolModernMapping.map((m, i) => (
                <tr
                  key={m.cobol}
                  className={cn(
                    'border-b border-border/50 hover:bg-surface-alt/50 transition-colors',
                    i % 2 === 1 && 'bg-surface-alt/30'
                  )}
                >
                  <td className="py-3 px-4 font-mono font-semibold text-accent-1">{m.cobol}</td>
                  <td className="py-3 px-4 font-mono text-text-secondary">{m.modern}</td>
                  <td className="py-3 px-4 text-text-muted">{m.desc}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
