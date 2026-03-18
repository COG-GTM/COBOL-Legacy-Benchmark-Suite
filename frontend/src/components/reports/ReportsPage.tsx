import { useNavigate, useParams } from 'react-router-dom';
import { FileText, Printer, Download } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { PositionReportTab } from './PositionReportTab';
import { AuditReportTab } from './AuditReportTab';
import { StatisticsReportTab } from './StatisticsReportTab';
import { cn } from '@/lib/utils';
import { useState } from 'react';

const tabs = [
  { id: 'positions', label: 'Position Reports', path: '/reports/positions' },
  { id: 'audit', label: 'Audit Reports', path: '/reports/audit' },
  { id: 'statistics', label: 'Statistics Reports', path: '/reports/statistics' },
] as const;

type TabId = (typeof tabs)[number]['id'];

export function ReportsPage() {
  const navigate = useNavigate();
  const { tab } = useParams<{ tab?: string }>();
  const [reportDate, setReportDate] = useState('2024-01-15');

  const activeTab: TabId = (tab as TabId) || 'positions';

  const handleTabChange = (tabId: TabId) => {
    navigate(`/reports/${tabId}`);
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="text-2xl font-bold text-white">Reports</h2>
          <p className="mt-1 text-[#94A3B8]">
            View position summaries, audit logs, and system statistics
          </p>
        </div>
        <div className="flex items-center gap-3">
          <div className="flex flex-col gap-1">
            <label htmlFor="report-date" className="sr-only">
              Report Date
            </label>
            <Input
              id="report-date"
              type="date"
              value={reportDate}
              onChange={(e) => setReportDate(e.target.value)}
              className="w-44"
            />
          </div>
          <Button variant="outline" size="sm" title="Export coming soon" disabled>
            <Download className="mr-1.5 h-4 w-4" />
            Export
          </Button>
          <Button variant="outline" size="sm" title="Print coming soon" disabled>
            <Printer className="mr-1.5 h-4 w-4" />
            Print
          </Button>
        </div>
      </div>

      {/* Report Date Display */}
      <div className="flex items-center gap-2 text-sm text-[#94A3B8]">
        <FileText className="h-4 w-4" />
        <span>
          Report Date: <span className="font-medium text-white">{reportDate}</span>
        </span>
      </div>

      {/* Tab Navigation */}
      <div role="tablist" aria-label="Report types" className="border-b border-[#334155]">
        <div className="flex gap-0">
          {tabs.map((t) => (
            <button
              key={t.id}
              role="tab"
              aria-selected={activeTab === t.id}
              aria-controls={`tabpanel-${t.id}`}
              id={`tab-${t.id}`}
              onClick={() => handleTabChange(t.id)}
              className={cn(
                'relative px-5 py-3 text-sm font-medium transition-colors',
                activeTab === t.id
                  ? 'text-[#22D3EE]'
                  : 'text-[#94A3B8] hover:text-white'
              )}
            >
              {t.label}
              {activeTab === t.id && (
                <span className="absolute bottom-0 left-0 right-0 h-0.5 bg-[#22D3EE]" />
              )}
            </button>
          ))}
        </div>
      </div>

      {/* Tab Content */}
      <div
        role="tabpanel"
        id={`tabpanel-${activeTab}`}
        aria-labelledby={`tab-${activeTab}`}
      >
        {activeTab === 'positions' && <PositionReportTab />}
        {activeTab === 'audit' && <AuditReportTab />}
        {activeTab === 'statistics' && <StatisticsReportTab />}
      </div>
    </div>
  );
}
