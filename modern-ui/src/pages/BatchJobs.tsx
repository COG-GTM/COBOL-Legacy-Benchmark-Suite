import { useState } from 'react';
import { ArrowRight, RefreshCw, CheckCircle, XCircle, Clock, Loader, AlertCircle } from 'lucide-react';
import Card from '../components/Card';
import StatusBadge from '../components/StatusBadge';
import { batchJobs } from '../data/mockData';
import { BATCH_STATUS_LABELS } from '../types';
import type { BatchJob } from '../types';

const STATUS_ICONS: Record<BatchJob['status'], typeof CheckCircle> = {
  R: Clock,
  A: Loader,
  W: AlertCircle,
  D: CheckCircle,
  E: XCircle,
};

const STATUS_ICON_COLORS: Record<BatchJob['status'], string> = {
  R: 'text-indigo-500',
  A: 'text-blue-500 animate-spin',
  W: 'text-amber-500',
  D: 'text-emerald-500',
  E: 'text-red-500',
};

export default function BatchJobs() {
  const [dateFilter, setDateFilter] = useState<string>('2026-04-29');
  const dates = [...new Set(batchJobs.map(j => j.processDate))].sort().reverse();
  const filtered = batchJobs.filter(j => j.processDate === dateFilter).sort((a, b) => a.sequenceNo - b.sequenceNo);

  const completed = filtered.filter(j => j.status === 'D').length;
  const errors = filtered.filter(j => j.status === 'E').length;
  const active = filtered.filter(j => j.status === 'A').length;

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-xl font-bold text-slate-900">Batch Jobs</h2>
          <p className="text-sm text-slate-500 mt-0.5">
            Job pipeline monitoring — modernized from BCHCTL00 / PRCSEQ00
          </p>
        </div>
        <button className="flex items-center gap-2 px-4 py-2 bg-white border border-slate-200 text-slate-600 text-sm font-medium rounded-lg hover:bg-slate-50">
          <RefreshCw size={14} /> Refresh
        </button>
      </div>

      <div className="flex gap-2">
        {dates.map(d => (
          <button
            key={d}
            onClick={() => setDateFilter(d)}
            className={`px-3 py-1.5 text-xs font-medium rounded-lg transition-colors ${
              dateFilter === d ? 'bg-blue-600 text-white' : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-50'
            }`}
          >
            {d}
          </button>
        ))}
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
        <div className="bg-white rounded-xl border border-slate-200 p-4 text-center">
          <div className="text-2xl font-bold text-slate-900">{filtered.length}</div>
          <div className="text-xs text-slate-500 mt-1">Total Jobs</div>
        </div>
        <div className="bg-emerald-50 rounded-xl border border-emerald-200 p-4 text-center">
          <div className="text-2xl font-bold text-emerald-700">{completed}</div>
          <div className="text-xs text-slate-500 mt-1">Completed</div>
        </div>
        <div className="bg-blue-50 rounded-xl border border-blue-200 p-4 text-center">
          <div className="text-2xl font-bold text-blue-700">{active}</div>
          <div className="text-xs text-slate-500 mt-1">Running</div>
        </div>
        <div className="bg-red-50 rounded-xl border border-red-200 p-4 text-center">
          <div className="text-2xl font-bold text-red-700">{errors}</div>
          <div className="text-xs text-slate-500 mt-1">Errors</div>
        </div>
      </div>

      <Card title="Processing Pipeline">
        <div className="flex items-center gap-2 overflow-x-auto pb-2">
          {filtered.map((job, i) => {
            const Icon = STATUS_ICONS[job.status];
            return (
              <div key={job.id} className="flex items-center gap-2 shrink-0">
                <div className={`flex flex-col items-center p-3 rounded-xl border-2 min-w-[120px] ${
                  job.status === 'D' ? 'border-emerald-200 bg-emerald-50' :
                  job.status === 'A' ? 'border-blue-200 bg-blue-50' :
                  job.status === 'E' ? 'border-red-200 bg-red-50' :
                  job.status === 'W' ? 'border-amber-200 bg-amber-50' :
                  'border-slate-200 bg-slate-50'
                }`}>
                  <Icon size={20} className={STATUS_ICON_COLORS[job.status]} />
                  <div className="text-sm font-bold text-slate-800 mt-1">{job.jobName}</div>
                  <div className="text-[10px] text-slate-500">{job.programName}</div>
                  <StatusBadge status={job.status} label={BATCH_STATUS_LABELS[job.status]} />
                </div>
                {i < filtered.length - 1 && (
                  <ArrowRight size={16} className="text-slate-300 shrink-0" />
                )}
              </div>
            );
          })}
        </div>
      </Card>

      <Card title="Job Details">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-100">
                <th className="text-left py-2.5 px-3 text-xs font-medium text-slate-500 uppercase">Seq</th>
                <th className="text-left py-2.5 px-3 text-xs font-medium text-slate-500 uppercase">Job</th>
                <th className="text-left py-2.5 px-3 text-xs font-medium text-slate-500 uppercase">Program</th>
                <th className="text-left py-2.5 px-3 text-xs font-medium text-slate-500 uppercase">Status</th>
                <th className="text-left py-2.5 px-3 text-xs font-medium text-slate-500 uppercase">Start</th>
                <th className="text-left py-2.5 px-3 text-xs font-medium text-slate-500 uppercase">End</th>
                <th className="text-left py-2.5 px-3 text-xs font-medium text-slate-500 uppercase">RC</th>
                <th className="text-left py-2.5 px-3 text-xs font-medium text-slate-500 uppercase">Prerequisites</th>
                <th className="text-left py-2.5 px-3 text-xs font-medium text-slate-500 uppercase">Error</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map(job => (
                <tr key={job.id} className={`border-b border-slate-50 ${job.status === 'E' ? 'bg-red-50/50' : 'hover:bg-slate-50'}`}>
                  <td className="py-2.5 px-3 text-slate-600">{job.sequenceNo}</td>
                  <td className="py-2.5 px-3 font-medium text-slate-800">{job.jobName}</td>
                  <td className="py-2.5 px-3 font-mono text-xs text-slate-600">{job.programName}</td>
                  <td className="py-2.5 px-3">
                    <StatusBadge status={job.status} label={BATCH_STATUS_LABELS[job.status]} />
                  </td>
                  <td className="py-2.5 px-3 text-slate-600">{job.startTime || '—'}</td>
                  <td className="py-2.5 px-3 text-slate-600">{job.endTime || '—'}</td>
                  <td className="py-2.5 px-3">
                    {job.returnCode >= 0 ? (
                      <span className={`font-mono text-xs ${job.returnCode === 0 ? 'text-emerald-600' : 'text-red-600'}`}>
                        RC={job.returnCode}
                      </span>
                    ) : '—'}
                  </td>
                  <td className="py-2.5 px-3 text-xs text-slate-500">
                    {job.prereqs.length > 0
                      ? job.prereqs.map(p => `${p.name}(RC=${p.returnCode})`).join(', ')
                      : 'None'}
                  </td>
                  <td className="py-2.5 px-3 text-xs text-red-600">{job.errorDesc || ''}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  );
}
