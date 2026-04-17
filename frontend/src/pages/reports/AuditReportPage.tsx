import { useState, useMemo } from 'react';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card } from '@/components/ui/Card';
import { StatusBadge, getAuditStatusVariant } from '@/components/ui/StatusBadge';
import { auditEntries, errorEntries } from '@/data/mockData';

type StatusFilter = 'All' | 'Success' | 'Failed';

export function AuditReportPage() {
  const [programFilter, setProgramFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('All');
  const [startDate, setStartDate] = useState('2024-08-15');
  const [endDate, setEndDate] = useState('2024-08-15');

  const programs = useMemo(() => {
    const set = new Set(auditEntries.map((e) => e.program));
    return Array.from(set).sort();
  }, []);

  const filteredAudit = useMemo(() => {
    return auditEntries
      .filter((e) => {
        if (programFilter && e.program !== programFilter) return false;
        if (statusFilter === 'Success' && e.status !== 'SUCC') return false;
        if (statusFilter === 'Failed' && e.status !== 'FAIL') return false;
        const entryDate = e.timestamp.split(' ')[0];
        if (entryDate < startDate || entryDate > endDate) return false;
        return true;
      })
      .sort((a, b) => b.timestamp.localeCompare(a.timestamp));
  }, [programFilter, statusFilter, startDate, endDate]);

  const filteredErrors = useMemo(() => {
    return errorEntries
      .filter((e) => {
        const entryDate = e.timestamp.split(' ')[0];
        return entryDate >= startDate && entryDate <= endDate;
      })
      .sort((a, b) => b.timestamp.localeCompare(a.timestamp));
  }, [startDate, endDate]);

  const errorsByProgram = useMemo(() => {
    const map = new Map<string, number>();
    filteredErrors.forEach((e) => {
      map.set(e.program, (map.get(e.program) ?? 0) + 1);
    });
    return Array.from(map.entries()).sort((a, b) => b[1] - a[1]);
  }, [filteredErrors]);

  const totalAudit = filteredAudit.length;
  const successCount = filteredAudit.filter((e) => e.status === 'SUCC').length;
  const failCount = filteredAudit.filter((e) => e.status === 'FAIL').length;

  return (
    <div>
      <PageHeader
        title="Audit Report"
        description="Modernized from COBOL program RPTAUD00 \u2014 Audit Report Generator"
      />

      {/* Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
        <div className="bg-white rounded-lg border border-slate-200 shadow-sm p-4 text-center">
          <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Total Audit Records</p>
          <p className="mt-1 text-2xl font-bold text-slate-900">{totalAudit}</p>
        </div>
        <div className="bg-white rounded-lg border border-slate-200 shadow-sm p-4 text-center">
          <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Success Count</p>
          <p className="mt-1 text-2xl font-bold text-emerald-600">{successCount}</p>
        </div>
        <div className="bg-white rounded-lg border border-slate-200 shadow-sm p-4 text-center">
          <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Failure Count</p>
          <p className="mt-1 text-2xl font-bold text-red-600">{failCount}</p>
        </div>
      </div>

      <Card>
        <div className="text-center mb-4">
          <h2 className="text-xl font-bold text-slate-900 tracking-wide">SYSTEM AUDIT REPORT</h2>
          <p className="text-sm text-slate-500 mt-1">
            Report Period: {startDate} to {endDate}
          </p>
        </div>

        {/* Filters */}
        <div className="flex flex-wrap items-center gap-3 mb-4 pb-4 border-b border-slate-200">
          <div>
            <label className="block text-xs font-medium text-slate-500 mb-1">Start Date</label>
            <input
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              className="rounded-md border border-slate-300 px-3 py-1.5 text-sm text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-slate-500 mb-1">End Date</label>
            <input
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              className="rounded-md border border-slate-300 px-3 py-1.5 text-sm text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-slate-500 mb-1">Program</label>
            <select
              value={programFilter}
              onChange={(e) => setProgramFilter(e.target.value)}
              className="rounded-md border border-slate-300 px-3 py-1.5 text-sm text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">All Programs</option>
              {programs.map((p) => (
                <option key={p} value={p}>{p}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-xs font-medium text-slate-500 mb-1">Status</label>
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value as StatusFilter)}
              className="rounded-md border border-slate-300 px-3 py-1.5 text-sm text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="All">All</option>
              <option value="Success">Success</option>
              <option value="Failed">Failed</option>
            </select>
          </div>
        </div>

        {/* Security Audit Trail */}
        <h3 className="text-lg font-semibold text-slate-900 mb-3">Security Audit Trail</h3>
        <div className="overflow-x-auto mb-8">
          <table className="min-w-full divide-y divide-slate-200">
            <thead className="bg-slate-50">
              <tr>
                <th className="px-3 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Timestamp</th>
                <th className="px-3 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Program</th>
                <th className="px-3 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Type</th>
                <th className="px-3 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Action</th>
                <th className="px-3 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Status</th>
                <th className="px-3 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Portfolio ID</th>
                <th className="px-3 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Account No</th>
                <th className="px-3 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Message</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-slate-200">
              {filteredAudit.map((entry, idx) => (
                <tr key={`${entry.timestamp}-${idx}`} className="hover:bg-slate-50 transition-colors">
                  <td className="px-3 py-2 text-xs font-mono text-slate-600 whitespace-nowrap">{entry.timestamp}</td>
                  <td className="px-3 py-2 text-xs font-mono text-slate-700">{entry.program}</td>
                  <td className="px-3 py-2 text-xs text-slate-600">{entry.type}</td>
                  <td className="px-3 py-2 text-xs text-slate-600">{entry.action}</td>
                  <td className="px-3 py-2 text-xs">
                    <StatusBadge label={entry.status} variant={getAuditStatusVariant(entry.status)} />
                  </td>
                  <td className="px-3 py-2 text-xs font-mono text-slate-600">{entry.portfolioId || '\u2014'}</td>
                  <td className="px-3 py-2 text-xs font-mono text-slate-600">{entry.accountNo || '\u2014'}</td>
                  <td className="px-3 py-2 text-xs text-slate-600 max-w-xs truncate">{entry.message}</td>
                </tr>
              ))}
              {filteredAudit.length === 0 && (
                <tr>
                  <td colSpan={8} className="px-3 py-8 text-center text-sm text-slate-500">No audit entries found for the selected filters.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Error Summary */}
        <h3 className="text-lg font-semibold text-slate-900 mb-3">Error Summary</h3>

        {errorsByProgram.length > 0 && (
          <div className="flex flex-wrap gap-2 mb-4">
            {errorsByProgram.map(([program, count]) => (
              <div key={program} className="inline-flex items-center gap-1.5 bg-red-50 text-red-700 rounded-full px-3 py-1 text-xs font-medium ring-1 ring-inset ring-red-600/20">
                {program}: {count} error{count !== 1 ? 's' : ''}
              </div>
            ))}
          </div>
        )}

        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-slate-200">
            <thead className="bg-slate-50">
              <tr>
                <th className="px-3 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Timestamp</th>
                <th className="px-3 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Program</th>
                <th className="px-3 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Error Code</th>
                <th className="px-3 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Message</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-slate-200">
              {filteredErrors.map((entry, idx) => (
                <tr key={`${entry.timestamp}-${idx}`} className="hover:bg-slate-50 transition-colors">
                  <td className="px-3 py-2 text-xs font-mono text-slate-600 whitespace-nowrap">{entry.timestamp}</td>
                  <td className="px-3 py-2 text-xs font-mono text-slate-700">{entry.program}</td>
                  <td className="px-3 py-2 text-xs font-mono text-red-600 font-medium">{entry.code}</td>
                  <td className="px-3 py-2 text-xs text-slate-600">{entry.description}</td>
                </tr>
              ))}
              {filteredErrors.length === 0 && (
                <tr>
                  <td colSpan={4} className="px-3 py-8 text-center text-sm text-slate-500">No errors found for the selected date range.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  );
}
