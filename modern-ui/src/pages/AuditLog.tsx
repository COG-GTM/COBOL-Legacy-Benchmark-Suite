import { useState } from 'react';
import { Filter, Shield, Terminal, ArrowLeftRight } from 'lucide-react';
import StatusBadge from '../components/StatusBadge';
import { auditRecords } from '../data/mockData';
import { AUDIT_TYPE_LABELS } from '../types';
import type { AuditRecord } from '../types';

const TYPE_ICONS: Record<AuditRecord['type'], typeof Shield> = {
  TRAN: ArrowLeftRight,
  USER: Shield,
  SYST: Terminal,
};

const TYPE_COLORS: Record<AuditRecord['type'], string> = {
  TRAN: 'bg-blue-100 text-blue-600',
  USER: 'bg-purple-100 text-purple-600',
  SYST: 'bg-slate-100 text-slate-600',
};

export default function AuditLog() {
  const [typeFilter, setTypeFilter] = useState<string>('all');
  const [statusFilter, setStatusFilter] = useState<string>('all');

  const filtered = auditRecords
    .filter(a => typeFilter === 'all' || a.type === typeFilter)
    .filter(a => statusFilter === 'all' || a.status === statusFilter);

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-xl font-bold text-slate-900">Audit Log</h2>
        <p className="text-sm text-slate-500 mt-0.5">
          System audit trail — modernized from AUDITLOG / RPTAUD00
        </p>
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <div className="flex items-center gap-2 text-sm text-slate-500">
          <Filter size={14} />
          <span>Type:</span>
        </div>
        {['all', 'TRAN', 'USER', 'SYST'].map(t => (
          <button
            key={t}
            onClick={() => setTypeFilter(t)}
            className={`px-3 py-1.5 text-xs font-medium rounded-lg transition-colors ${
              typeFilter === t ? 'bg-blue-600 text-white' : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-50'
            }`}
          >
            {t === 'all' ? 'All' : AUDIT_TYPE_LABELS[t as AuditRecord['type']]}
          </button>
        ))}
        <div className="w-px h-5 bg-slate-200" />
        <span className="text-sm text-slate-500">Status:</span>
        {['all', 'SUCC', 'FAIL', 'WARN'].map(s => (
          <button
            key={s}
            onClick={() => setStatusFilter(s)}
            className={`px-3 py-1.5 text-xs font-medium rounded-lg transition-colors ${
              statusFilter === s ? 'bg-blue-600 text-white' : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-50'
            }`}
          >
            {s === 'all' ? 'All' : s === 'SUCC' ? 'Success' : s === 'FAIL' ? 'Failed' : 'Warning'}
          </button>
        ))}
      </div>

      <div className="space-y-3">
        {filtered.map(record => {
          const Icon = TYPE_ICONS[record.type];
          return (
            <div key={record.id} className="bg-white rounded-xl border border-slate-200 p-4 hover:border-slate-300 transition-colors">
              <div className="flex items-start gap-3">
                <div className={`p-2 rounded-lg shrink-0 ${TYPE_COLORS[record.type]}`}>
                  <Icon size={16} />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="text-sm font-medium text-slate-800">{record.action}</span>
                    <StatusBadge status={record.status} label={record.status === 'SUCC' ? 'Success' : record.status === 'FAIL' ? 'Failed' : 'Warning'} />
                    <span className="text-xs text-slate-400">{AUDIT_TYPE_LABELS[record.type]}</span>
                  </div>
                  <p className="text-sm text-slate-600 mt-1">{record.message}</p>
                  <div className="flex items-center gap-4 mt-2 text-xs text-slate-400">
                    <span>{record.timestamp}</span>
                    <span>User: {record.userId}</span>
                    <span>Program: {record.program}</span>
                    {record.portfolioId && <span>Portfolio: {record.portfolioId}</span>}
                    <span>Terminal: {record.terminal}</span>
                  </div>
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
