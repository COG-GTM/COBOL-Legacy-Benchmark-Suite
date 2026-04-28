import { useState, useMemo } from 'react';
import { mockAuditRecords } from '../mocks/mockData';
import type { AuditType } from '../types';
import { AUDIT_TYPE_LABELS, AUDIT_STATUS_LABELS } from '../types';

/**
 * Filterable audit log
 * Event types: TRAN, USER, SYST per AUDITLOG.cpy lines 14-17
 */
export function AuditReportPage() {
  const [typeFilter, setTypeFilter] = useState<AuditType | ''>('');
  const [actionFilter, setActionFilter] = useState('');

  const filtered = useMemo(() => {
    return mockAuditRecords.filter(r => {
      if (typeFilter && r.type !== typeFilter) return false;
      if (actionFilter && r.action !== actionFilter) return false;
      return true;
    });
  }, [typeFilter, actionFilter]);

  const uniqueActions = [...new Set(mockAuditRecords.map(r => r.action))];

  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-2">Audit Report</h1>
      <p className="text-sm text-gray-500 mb-6">Audit Trail Log (AUDITLOG.cpy)</p>

      {/* Filters */}
      <div className="flex gap-4 mb-4">
        <div>
          <label className="block text-xs text-gray-600 mb-1">Event Type</label>
          <select
            value={typeFilter}
            onChange={e => setTypeFilter(e.target.value as AuditType | '')}
            className="border border-gray-300 rounded px-2 py-1 text-sm"
          >
            <option value="">All Types</option>
            {(Object.entries(AUDIT_TYPE_LABELS) as [AuditType, string][]).map(([k, v]) => (
              <option key={k} value={k}>{v} ({k})</option>
            ))}
          </select>
        </div>
        <div>
          <label className="block text-xs text-gray-600 mb-1">Action</label>
          <select
            value={actionFilter}
            onChange={e => setActionFilter(e.target.value)}
            className="border border-gray-300 rounded px-2 py-1 text-sm"
          >
            <option value="">All Actions</option>
            {uniqueActions.map(a => (
              <option key={a} value={a}>{a}</option>
            ))}
          </select>
        </div>
        <div className="flex items-end">
          <span className="text-xs text-gray-400">{filtered.length} records</span>
        </div>
      </div>

      {/* Table */}
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b-2 border-gray-300">
              <th className="text-left py-2 px-3 font-semibold">Timestamp</th>
              <th className="text-left py-2 px-3 font-semibold">Type</th>
              <th className="text-left py-2 px-3 font-semibold">Action</th>
              <th className="text-left py-2 px-3 font-semibold">Status</th>
              <th className="text-left py-2 px-3 font-semibold">User</th>
              <th className="text-left py-2 px-3 font-semibold">Portfolio</th>
              <th className="text-left py-2 px-3 font-semibold">Message</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map((r, i) => (
              <tr key={i} className="border-b border-gray-100 hover:bg-gray-50">
                <td className="py-2 px-3 font-mono text-xs">{r.timestamp}</td>
                <td className="py-2 px-3">
                  <span className={`inline-block px-2 py-0.5 rounded text-xs font-medium ${
                    r.type === 'TRAN' ? 'bg-blue-100 text-blue-800' :
                    r.type === 'USER' ? 'bg-purple-100 text-purple-800' :
                    'bg-gray-100 text-gray-800'
                  }`}>
                    {r.type}
                  </span>
                </td>
                <td className="py-2 px-3">{r.action}</td>
                <td className="py-2 px-3">
                  <span className={`inline-block px-2 py-0.5 rounded text-xs font-medium ${
                    r.status === 'SUCC' ? 'bg-green-100 text-green-800' :
                    r.status === 'FAIL' ? 'bg-red-100 text-red-800' :
                    'bg-yellow-100 text-yellow-800'
                  }`}>
                    {AUDIT_STATUS_LABELS[r.status]}
                  </span>
                </td>
                <td className="py-2 px-3 font-mono">{r.userId}</td>
                <td className="py-2 px-3 font-mono">{r.portfolioId || '-'}</td>
                <td className="py-2 px-3 text-xs text-gray-600 max-w-xs truncate">{r.message}</td>
              </tr>
            ))}
            {filtered.length === 0 && (
              <tr><td colSpan={7} className="text-center py-8 text-gray-400">No audit records match the filter</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
