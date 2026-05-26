import { useState, useMemo } from 'react';
import {
  AlertTriangle,
  AlertCircle,
  XCircle,
  BookOpen,
  ChevronDown,
  ChevronRight,
  Clock,
  Hash,
} from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { PageHeader } from '@/components/ui/PageHeader';
import { SearchInput } from '@/components/ui/SearchInput';
import { StatusBadge, getSeverityVariant } from '@/components/ui/StatusBadge';
import { errorEntries } from '@/data/mockData';

const ERROR_CODE_REFERENCE = [
  { code: 'E001', description: 'Invalid Account Number', severity: 'Error' as const, action: 'Reject' },
  { code: 'E002', description: 'Invalid Fund ID', severity: 'Error' as const, action: 'Reject' },
  { code: 'E003', description: 'Invalid Transaction Type', severity: 'Error' as const, action: 'Reject' },
  { code: 'E004', description: 'Insufficient Position Balance', severity: 'Error' as const, action: 'Reject' },
  { code: 'W001', description: 'Zero Dollar Transaction', severity: 'Warning' as const, action: 'Process' },
  { code: 'W002', description: 'Duplicate Transaction ID', severity: 'Warning' as const, action: 'Log' },
];

export function ErrorLogPage() {
  const [severityFilter, setSeverityFilter] = useState<string>('ALL');
  const [programFilter, setProgramFilter] = useState<string>('ALL');
  const [codeFilter, setCodeFilter] = useState<string>('ALL');
  const [search, setSearch] = useState('');
  const [refPanelOpen, setRefPanelOpen] = useState(false);

  const programs = useMemo(() => {
    const set = new Set(errorEntries.map((e) => e.program));
    return [...set].sort();
  }, []);

  const codes = useMemo(() => {
    const set = new Set(errorEntries.map((e) => e.code));
    return [...set].sort();
  }, []);

  const filtered = useMemo(() => {
    let result = [...errorEntries].sort((a, b) => b.timestamp.localeCompare(a.timestamp));
    if (severityFilter !== 'ALL') {
      result = result.filter((e) => e.severity === severityFilter);
    }
    if (programFilter !== 'ALL') {
      result = result.filter((e) => e.program === programFilter);
    }
    if (codeFilter !== 'ALL') {
      result = result.filter((e) => e.code === codeFilter);
    }
    if (search) {
      const q = search.toLowerCase();
      result = result.filter(
        (e) =>
          e.description.toLowerCase().includes(q) ||
          (e.accountNo && e.accountNo.toLowerCase().includes(q)) ||
          e.code.toLowerCase().includes(q),
      );
    }
    return result;
  }, [severityFilter, programFilter, codeFilter, search]);

  const stats = useMemo(() => {
    const totalErrors = errorEntries.filter((e) => e.severity === 'Error').length;
    const totalWarnings = errorEntries.filter((e) => e.severity === 'Warning').length;
    const sorted = [...errorEntries].sort((a, b) => b.timestamp.localeCompare(a.timestamp));
    const mostRecent = sorted[0];
    const codeCounts = new Map<string, number>();
    for (const e of errorEntries) {
      codeCounts.set(e.code, (codeCounts.get(e.code) ?? 0) + 1);
    }
    let mostCommonCode = '';
    let maxCount = 0;
    for (const [code, count] of codeCounts) {
      if (count > maxCount) {
        mostCommonCode = code;
        maxCount = count;
      }
    }
    return { totalErrors, totalWarnings, mostRecent, mostCommonCode, mostCommonCount: maxCount };
  }, []);

  const summaryCards = [
    {
      label: 'Total Errors',
      value: stats.totalErrors.toString(),
      icon: <XCircle className="w-6 h-6" />,
      color: 'text-red-600 bg-red-50',
    },
    {
      label: 'Total Warnings',
      value: stats.totalWarnings.toString(),
      icon: <AlertTriangle className="w-6 h-6" />,
      color: 'text-amber-600 bg-amber-50',
    },
    {
      label: 'Most Recent',
      value: stats.mostRecent?.code ?? '—',
      sub: stats.mostRecent?.timestamp.split(' ')[1] ?? '',
      icon: <Clock className="w-6 h-6" />,
      color: 'text-blue-600 bg-blue-50',
    },
    {
      label: 'Most Common',
      value: stats.mostCommonCode || '—',
      sub: stats.mostCommonCount > 0 ? `${stats.mostCommonCount} occurrences` : '',
      icon: <Hash className="w-6 h-6" />,
      color: 'text-violet-600 bg-violet-50',
    },
  ];

  return (
    <div>
      <PageHeader
        title="Error Log"
        description="COBOL ERRLOG — system errors and warnings from ERRHNDL handler"
      />

      {/* Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        {summaryCards.map((card) => (
          <div key={card.label} className="bg-white rounded-lg border border-slate-200 shadow-sm p-5">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-slate-500">{card.label}</p>
                <p className="text-2xl font-bold text-slate-900 mt-1">{card.value}</p>
                {'sub' in card && card.sub && (
                  <p className="text-xs text-slate-500 mt-0.5">{card.sub}</p>
                )}
              </div>
              <div className={`p-3 rounded-lg ${card.color}`}>{card.icon}</div>
            </div>
          </div>
        ))}
      </div>

      {/* Filters */}
      <div className="flex flex-wrap items-center gap-3 mb-4">
        <select
          value={severityFilter}
          onChange={(e) => setSeverityFilter(e.target.value)}
          className="text-sm border border-slate-300 rounded-lg px-3 py-2 bg-white text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          <option value="ALL">All Severities</option>
          <option value="Error">Error</option>
          <option value="Warning">Warning</option>
        </select>

        <select
          value={programFilter}
          onChange={(e) => setProgramFilter(e.target.value)}
          className="text-sm border border-slate-300 rounded-lg px-3 py-2 bg-white text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          <option value="ALL">All Programs</option>
          {programs.map((p) => (
            <option key={p} value={p}>{p}</option>
          ))}
        </select>

        <select
          value={codeFilter}
          onChange={(e) => setCodeFilter(e.target.value)}
          className="text-sm border border-slate-300 rounded-lg px-3 py-2 bg-white text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          <option value="ALL">All Codes</option>
          {codes.map((c) => (
            <option key={c} value={c}>{c}</option>
          ))}
        </select>

        <SearchInput
          value={search}
          onChange={setSearch}
          placeholder="Search description or account..."
          className="w-64"
        />
      </div>

      {/* Error Log Table */}
      <Card className="mb-6">
        <div className="overflow-x-auto -m-6">
          <table className="min-w-full divide-y divide-slate-200">
            <thead className="bg-slate-50">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase">Timestamp</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase">Program</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase">Code</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase">Severity</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase">Account</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase">Description</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase">Action Required</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200">
              {filtered.length === 0 ? (
                <tr>
                  <td colSpan={7} className="px-4 py-12 text-center text-slate-500">
                    No errors match the current filters
                  </td>
                </tr>
              ) : (
                filtered.map((entry, idx) => (
                  <tr key={`${entry.timestamp}-${entry.code}-${idx}`} className="hover:bg-slate-50 transition-colors">
                    <td className="px-4 py-3 text-sm font-mono text-slate-700 whitespace-nowrap">{entry.timestamp}</td>
                    <td className="px-4 py-3 text-sm font-mono text-slate-700 whitespace-nowrap">{entry.program}</td>
                    <td className="px-4 py-3 text-sm font-mono text-slate-900 font-semibold whitespace-nowrap">{entry.code}</td>
                    <td className="px-4 py-3">
                      <StatusBadge label={entry.severity} variant={getSeverityVariant(entry.severity)} />
                    </td>
                    <td className="px-4 py-3 text-sm font-mono text-slate-600 whitespace-nowrap">{entry.accountNo ?? '—'}</td>
                    <td className="px-4 py-3 text-sm text-slate-700">{entry.description}</td>
                    <td className="px-4 py-3 text-sm text-slate-600">{entry.action}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </Card>

      {/* Error Code Reference Panel (collapsible) */}
      <Card>
        <button
          type="button"
          onClick={() => setRefPanelOpen((v) => !v)}
          className="flex items-center gap-2 w-full text-left -m-6 px-6 py-4 hover:bg-slate-50 transition-colors rounded-lg"
        >
          <BookOpen className="w-5 h-5 text-slate-500" />
          <span className="text-lg font-semibold text-slate-900 flex-1">Error Code Reference</span>
          {refPanelOpen ? (
            <ChevronDown className="w-5 h-5 text-slate-400" />
          ) : (
            <ChevronRight className="w-5 h-5 text-slate-400" />
          )}
        </button>
        {refPanelOpen && (
          <div className="mt-6 -mx-6 -mb-6">
            <table className="min-w-full divide-y divide-slate-200">
              <thead className="bg-slate-50">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase">Code</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase">Description</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase">Severity</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase">Required Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200">
                {ERROR_CODE_REFERENCE.map((ref) => (
                  <tr key={ref.code} className="hover:bg-slate-50 transition-colors">
                    <td className="px-4 py-3 text-sm font-mono font-semibold text-slate-900">{ref.code}</td>
                    <td className="px-4 py-3 text-sm text-slate-700">{ref.description}</td>
                    <td className="px-4 py-3">
                      <StatusBadge label={ref.severity} variant={getSeverityVariant(ref.severity)} />
                    </td>
                    <td className="px-4 py-3 text-sm text-slate-600">{ref.action}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div className="px-6 py-3 bg-slate-50 text-xs text-slate-500 flex items-start gap-2 rounded-b-lg">
              <AlertCircle className="w-3.5 h-3.5 mt-0.5 shrink-0" />
              <span>Error codes derived from the COBOL data dictionary. E-prefixed codes require transaction rejection; W-prefixed codes allow processing with logging.</span>
            </div>
          </div>
        )}
      </Card>
    </div>
  );
}
