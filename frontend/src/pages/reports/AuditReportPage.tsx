import { useState, useMemo } from 'react';
import { Shield, CheckCircle, XCircle, AlertTriangle } from 'lucide-react';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card } from '@/components/ui/Card';
import { SearchInput } from '@/components/ui/SearchInput';
import { StatusBadge, getAuditStatusVariant } from '@/components/ui/StatusBadge';
import { auditEntries } from '@/data/mockData';

const ROWS_PER_PAGE = 20;

type AuditStatusFilter = 'All' | 'SUCC' | 'FAIL';

export function AuditReportPage() {
  const [statusFilter, setStatusFilter] = useState<AuditStatusFilter>('All');
  const [programFilter, setProgramFilter] = useState('All');
  const [searchQuery, setSearchQuery] = useState('');
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  const [currentPage, setCurrentPage] = useState(1);

  const uniquePrograms = useMemo(
    () => Array.from(new Set(auditEntries.map((e) => e.program))).sort(),
    [],
  );

  const filtered = useMemo(() => {
    let result = auditEntries;
    if (statusFilter !== 'All') {
      result = result.filter((e) => e.status === statusFilter);
    }
    if (programFilter !== 'All') {
      result = result.filter((e) => e.program === programFilter);
    }
    if (dateFrom) {
      result = result.filter((e) => e.timestamp >= dateFrom);
    }
    if (dateTo) {
      const toEnd = dateTo + ' 23:59:59';
      result = result.filter((e) => e.timestamp <= toEnd);
    }
    if (searchQuery.trim()) {
      const q = searchQuery.trim().toLowerCase();
      result = result.filter(
        (e) =>
          e.message.toLowerCase().includes(q) ||
          e.portfolioId.toLowerCase().includes(q),
      );
    }
    return [...result].sort((a, b) => b.timestamp.localeCompare(a.timestamp));
  }, [statusFilter, programFilter, searchQuery, dateFrom, dateTo]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / ROWS_PER_PAGE));
  const safeCurrentPage = Math.min(currentPage, totalPages);
  const paginatedData = filtered.slice(
    (safeCurrentPage - 1) * ROWS_PER_PAGE,
    safeCurrentPage * ROWS_PER_PAGE,
  );

  const summary = useMemo(() => {
    const total = filtered.length;
    const successful = filtered.filter((e) => e.status === 'SUCC').length;
    const failed = filtered.filter((e) => e.status === 'FAIL').length;
    return { total, successful, failed, warnings: 0 };
  }, [filtered]);

  const handlePageChange = (page: number) => {
    setCurrentPage(Math.max(1, Math.min(page, totalPages)));
  };

  const summaryCards = [
    { label: 'Total Events', value: summary.total.toString(), icon: <Shield className="w-6 h-6" />, color: 'text-blue-600 bg-blue-50' },
    { label: 'Successful', value: summary.successful.toString(), icon: <CheckCircle className="w-6 h-6" />, color: 'text-emerald-600 bg-emerald-50' },
    { label: 'Failed', value: summary.failed.toString(), icon: <XCircle className="w-6 h-6" />, color: 'text-red-600 bg-red-50' },
    { label: 'Warnings', value: summary.warnings.toString(), icon: <AlertTriangle className="w-6 h-6" />, color: 'text-amber-600 bg-amber-50' },
  ];

  return (
    <div>
      <PageHeader
        title="Audit Report"
        description="Review system audit trail and compliance records"
      />

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        {summaryCards.map((card) => (
          <div key={card.label} className="bg-white rounded-lg border border-slate-200 shadow-sm p-5">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-slate-500">{card.label}</p>
                <p className="text-2xl font-bold text-slate-900 mt-1">{card.value}</p>
              </div>
              <div className={`p-3 rounded-lg ${card.color}`}>{card.icon}</div>
            </div>
          </div>
        ))}
      </div>

      <Card
        title="Audit Log"
        actions={
          <div className="flex flex-wrap items-center gap-3">
            <div className="flex items-center gap-2">
              <label className="text-xs font-medium text-slate-500">From</label>
              <input
                type="date"
                value={dateFrom}
                onChange={(e) => { setDateFrom(e.target.value); setCurrentPage(1); }}
                className="text-sm border border-slate-300 rounded-lg px-3 py-2 bg-white text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <div className="flex items-center gap-2">
              <label className="text-xs font-medium text-slate-500">To</label>
              <input
                type="date"
                value={dateTo}
                onChange={(e) => { setDateTo(e.target.value); setCurrentPage(1); }}
                className="text-sm border border-slate-300 rounded-lg px-3 py-2 bg-white text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <select
              value={programFilter}
              onChange={(e) => { setProgramFilter(e.target.value); setCurrentPage(1); }}
              className="text-sm border border-slate-300 rounded-lg px-3 py-2 bg-white text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="All">All Programs</option>
              {uniquePrograms.map((p) => (
                <option key={p} value={p}>{p}</option>
              ))}
            </select>
            <select
              value={statusFilter}
              onChange={(e) => { setStatusFilter(e.target.value as AuditStatusFilter); setCurrentPage(1); }}
              className="text-sm border border-slate-300 rounded-lg px-3 py-2 bg-white text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="All">All Statuses</option>
              <option value="SUCC">Success</option>
              <option value="FAIL">Fail</option>
            </select>
            <SearchInput
              value={searchQuery}
              onChange={(v) => { setSearchQuery(v); setCurrentPage(1); }}
              placeholder="Search message or portfolio..."
              className="w-56"
            />
          </div>
        }
      >
        <div className="overflow-x-auto -m-6 mt-0">
          <table className="min-w-full divide-y divide-slate-200">
            <thead className="bg-slate-50">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase">Timestamp</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase">Program</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase">Action Type</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase">Status</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase">Portfolio ID</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase">Account No</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase">Message</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200">
              {paginatedData.map((entry, i) => (
                <tr key={`${entry.timestamp}-${i}`} className="hover:bg-slate-50 transition-colors">
                  <td className="px-4 py-3 text-sm font-mono text-slate-700 whitespace-nowrap">{entry.timestamp}</td>
                  <td className="px-4 py-3 text-sm font-mono text-slate-700">{entry.program}</td>
                  <td className="px-4 py-3 text-sm text-slate-700">{entry.action}</td>
                  <td className="px-4 py-3">
                    <StatusBadge
                      label={entry.status === 'SUCC' ? 'Success' : 'Fail'}
                      variant={getAuditStatusVariant(entry.status)}
                    />
                  </td>
                  <td className="px-4 py-3 text-sm font-mono text-slate-600">{entry.portfolioId || '—'}</td>
                  <td className="px-4 py-3 text-sm font-mono text-slate-600">{entry.accountNo || '—'}</td>
                  <td className="px-4 py-3 text-sm text-slate-600 max-w-md truncate">{entry.message}</td>
                </tr>
              ))}
              {paginatedData.length === 0 && (
                <tr>
                  <td colSpan={7} className="px-4 py-12 text-center text-sm text-slate-500">
                    No audit entries found matching the current filters.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {totalPages > 1 && (
          <div className="flex items-center justify-between mt-4 pt-4 border-t border-slate-200">
            <p className="text-sm text-slate-600">
              Showing {(safeCurrentPage - 1) * ROWS_PER_PAGE + 1}–{Math.min(safeCurrentPage * ROWS_PER_PAGE, filtered.length)} of {filtered.length} entries
            </p>
            <div className="flex items-center gap-1">
              <button
                onClick={() => handlePageChange(safeCurrentPage - 1)}
                disabled={safeCurrentPage === 1}
                className="px-3 py-1 text-sm rounded-lg border border-slate-300 text-slate-700 hover:bg-slate-50 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Previous
              </button>
              {Array.from({ length: totalPages }, (_, i) => i + 1).map((page) => (
                <button
                  key={page}
                  onClick={() => handlePageChange(page)}
                  className={`px-3 py-1 text-sm rounded-lg border ${
                    page === safeCurrentPage
                      ? 'bg-blue-600 text-white border-blue-600'
                      : 'border-slate-300 text-slate-700 hover:bg-slate-50'
                  }`}
                >
                  {page}
                </button>
              ))}
              <button
                onClick={() => handlePageChange(safeCurrentPage + 1)}
                disabled={safeCurrentPage === totalPages}
                className="px-3 py-1 text-sm rounded-lg border border-slate-300 text-slate-700 hover:bg-slate-50 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Next
              </button>
            </div>
          </div>
        )}
      </Card>
    </div>
  );
}
