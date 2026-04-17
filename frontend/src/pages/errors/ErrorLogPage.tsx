import { useState, useMemo } from 'react';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card } from '@/components/ui/Card';
import { StatusBadge, getSeverityVariant } from '@/components/ui/StatusBadge';
import { errorEntries } from '@/data/mockData';
import { AlertTriangle, AlertCircle, Clock, Hash } from 'lucide-react';

type SeverityFilter = 'All' | 'Error' | 'Warning';

const ERROR_CODE_DEFS: Record<string, { description: string; severity: 'Error' | 'Warning'; action: string }> = {
  E001: { description: 'Invalid Account Number', severity: 'Error', action: 'Reject' },
  E002: { description: 'Invalid Fund ID', severity: 'Error', action: 'Reject' },
  E003: { description: 'Invalid Transaction Type', severity: 'Error', action: 'Reject' },
  E004: { description: 'Insufficient Position Balance', severity: 'Error', action: 'Reject' },
  W001: { description: 'Zero Dollar Transaction', severity: 'Warning', action: 'Process' },
  W002: { description: 'Duplicate Transaction ID', severity: 'Warning', action: 'Log' },
};

export function ErrorLogPage() {
  const [severityFilter, setSeverityFilter] = useState<SeverityFilter>('All');
  const [programFilter, setProgramFilter] = useState('');
  const [startDate, setStartDate] = useState('2024-08-15');
  const [endDate, setEndDate] = useState('2024-08-15');

  const programs = useMemo(() => {
    const set = new Set(errorEntries.map((e) => e.program));
    return Array.from(set).sort();
  }, []);

  const filtered = useMemo(() => {
    return errorEntries
      .filter((e) => {
        if (severityFilter === 'Error' && e.severity !== 'Error') return false;
        if (severityFilter === 'Warning' && e.severity !== 'Warning') return false;
        if (programFilter && e.program !== programFilter) return false;
        const entryDate = e.timestamp.split(' ')[0];
        if (entryDate < startDate || entryDate > endDate) return false;
        return true;
      })
      .sort((a, b) => b.timestamp.localeCompare(a.timestamp));
  }, [severityFilter, programFilter, startDate, endDate]);

  const totalErrors = filtered.filter((e) => e.severity === 'Error').length;
  const totalWarnings = filtered.filter((e) => e.severity === 'Warning').length;

  const mostCommonCode = useMemo(() => {
    const counts = new Map<string, number>();
    filtered.forEach((e) => {
      counts.set(e.code, (counts.get(e.code) ?? 0) + 1);
    });
    let maxCode = '\u2014';
    let maxCount = 0;
    counts.forEach((count, code) => {
      if (count > maxCount) {
        maxCount = count;
        maxCode = code;
      }
    });
    return maxCode;
  }, [filtered]);

  const lastTimestamp = filtered.length > 0 ? filtered[0].timestamp : '\u2014';

  return (
    <div>
      <PageHeader
        title="Error Log"
        description="Modernized from the ERRLOG DB2 table and COBOL error handling framework"
      />

      {/* Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <div className="bg-white rounded-lg border border-slate-200 shadow-sm p-4">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-red-50 text-red-600">
              <AlertCircle className="w-5 h-5" />
            </div>
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Total Errors</p>
              <p className="text-2xl font-bold text-red-600">{totalErrors}</p>
            </div>
          </div>
        </div>
        <div className="bg-white rounded-lg border border-slate-200 shadow-sm p-4">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-amber-50 text-amber-600">
              <AlertTriangle className="w-5 h-5" />
            </div>
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Total Warnings</p>
              <p className="text-2xl font-bold text-amber-600">{totalWarnings}</p>
            </div>
          </div>
        </div>
        <div className="bg-white rounded-lg border border-slate-200 shadow-sm p-4">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-blue-50 text-blue-600">
              <Hash className="w-5 h-5" />
            </div>
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Most Common Code</p>
              <p className="text-2xl font-bold text-slate-900">{mostCommonCode}</p>
            </div>
          </div>
        </div>
        <div className="bg-white rounded-lg border border-slate-200 shadow-sm p-4">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-slate-100 text-slate-600">
              <Clock className="w-5 h-5" />
            </div>
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Last Error</p>
              <p className="text-sm font-mono font-bold text-slate-900">{lastTimestamp}</p>
            </div>
          </div>
        </div>
      </div>

      {/* Error Code Reference */}
      <Card title="Error Code Reference" className="mb-6">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-slate-200 text-xs">
            <thead className="bg-slate-50">
              <tr>
                <th className="px-3 py-2 text-left font-semibold text-slate-600 uppercase tracking-wider">Code</th>
                <th className="px-3 py-2 text-left font-semibold text-slate-600 uppercase tracking-wider">Description</th>
                <th className="px-3 py-2 text-left font-semibold text-slate-600 uppercase tracking-wider">Severity</th>
                <th className="px-3 py-2 text-left font-semibold text-slate-600 uppercase tracking-wider">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200">
              {Object.entries(ERROR_CODE_DEFS).map(([code, def]) => (
                <tr key={code} className="hover:bg-slate-50">
                  <td className="px-3 py-2 font-mono font-medium text-slate-800">{code}</td>
                  <td className="px-3 py-2 text-slate-600">{def.description}</td>
                  <td className="px-3 py-2">
                    <StatusBadge label={def.severity} variant={getSeverityVariant(def.severity)} />
                  </td>
                  <td className="px-3 py-2 text-slate-600">{def.action}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>

      {/* Error Log Table */}
      <Card title="Error Log">
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
            <label className="block text-xs font-medium text-slate-500 mb-1">Severity</label>
            <select
              value={severityFilter}
              onChange={(e) => setSeverityFilter(e.target.value as SeverityFilter)}
              className="rounded-md border border-slate-300 px-3 py-1.5 text-sm text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="All">All</option>
              <option value="Error">Errors Only</option>
              <option value="Warning">Warnings Only</option>
            </select>
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
        </div>

        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-slate-200">
            <thead className="bg-slate-50">
              <tr>
                <th className="px-3 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Timestamp</th>
                <th className="px-3 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Error Code</th>
                <th className="px-3 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Severity</th>
                <th className="px-3 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Program</th>
                <th className="px-3 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Description</th>
                <th className="px-3 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Action Required</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-slate-200">
              {filtered.map((entry, idx) => (
                <tr key={`${entry.timestamp}-${entry.code}-${idx}`} className="hover:bg-slate-50 transition-colors">
                  <td className="px-3 py-2 text-xs font-mono text-slate-600 whitespace-nowrap">{entry.timestamp}</td>
                  <td className="px-3 py-2 text-xs font-mono font-medium text-slate-800">{entry.code}</td>
                  <td className="px-3 py-2 text-xs">
                    <StatusBadge label={entry.severity} variant={getSeverityVariant(entry.severity)} />
                  </td>
                  <td className="px-3 py-2 text-xs font-mono text-slate-700">{entry.program}</td>
                  <td className="px-3 py-2 text-xs text-slate-600">{entry.description}</td>
                  <td className="px-3 py-2 text-xs text-slate-600">{entry.action}</td>
                </tr>
              ))}
              {filtered.length === 0 && (
                <tr>
                  <td colSpan={6} className="px-3 py-8 text-center text-sm text-slate-500">
                    No error entries found for the selected filters.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  );
}
