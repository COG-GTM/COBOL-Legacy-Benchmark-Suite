import { useState, useMemo } from 'react';
import { Download, FileText } from 'lucide-react';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { portfolios } from '@/data/mockData';

type PositionReportRow = Record<string, unknown> & {
  portfolioId: string;
  portfolioName: string;
  fundId: string;
  currentValue: number;
  previousValue: number;
  changePercent: number;
};

const positionReportData: PositionReportRow[] = [
  { portfolioId: 'PORT0001', portfolioName: 'Growth Equity Fund', fundId: 'GRWEQF', currentValue: 712500.00, previousValue: 682500.00, changePercent: 4.40 },
  { portfolioId: 'PORT0001', portfolioName: 'Growth Equity Fund', fundId: 'BLUCDP', currentValue: 1044750.00, previousValue: 1022125.00, changePercent: 2.21 },
  { portfolioId: 'PORT0002', portfolioName: 'Blue Chip Dividend Portfolio', fundId: 'FIXINC', currentValue: 2512500.00, previousValue: 2468750.00, changePercent: 1.77 },
  { portfolioId: 'PORT0002', portfolioName: 'Blue Chip Dividend Portfolio', fundId: 'EMERGE', currentValue: 408000.00, previousValue: 393600.00, changePercent: 3.66 },
  { portfolioId: 'PORT0003', portfolioName: 'Fixed Income Treasury', fundId: 'TECHSF', currentValue: 1182500.00, previousValue: 1155000.00, changePercent: 2.38 },
  { portfolioId: 'PORT0003', portfolioName: 'Fixed Income Treasury', fundId: 'HLTHIF', currentValue: 678200.00, previousValue: 692760.00, changePercent: -2.10 },
  { portfolioId: 'PORT0004', portfolioName: 'International Emerging Markets', fundId: 'REITPF', currentValue: 1026000.00, previousValue: 1000800.00, changePercent: 2.52 },
  { portfolioId: 'PORT0004', portfolioName: 'International Emerging Markets', fundId: 'BALGIF', currentValue: 2685000.00, previousValue: 2655000.00, changePercent: 1.13 },
  { portfolioId: 'PORT0005', portfolioName: 'Technology Sector Fund', fundId: 'SMCAPV', currentValue: 638000.00, previousValue: 624800.00, changePercent: 2.11 },
  { portfolioId: 'PORT0006', portfolioName: 'Healthcare Innovation Fund', fundId: 'ESGSUS', currentValue: 922250.00, previousValue: 900450.00, changePercent: 2.42 },
  { portfolioId: 'PORT0006', portfolioName: 'Healthcare Innovation Fund', fundId: 'RETINC', currentValue: 4280000.00, previousValue: 4200000.00, changePercent: 1.90 },
  { portfolioId: 'PORT0007', portfolioName: 'Real Estate Investment Trust', fundId: 'GRWEQF', currentValue: 351000.00, previousValue: 360360.00, changePercent: -2.60 },
  { portfolioId: 'PORT0007', portfolioName: 'Real Estate Investment Trust', fundId: 'TECHSF', currentValue: 700800.00, previousValue: 689600.00, changePercent: 1.62 },
  { portfolioId: 'PORT0008', portfolioName: 'Balanced Growth & Income', fundId: 'FIXINC', currentValue: 2010000.00, previousValue: 1982000.00, changePercent: 1.41 },
  { portfolioId: 'PORT0008', portfolioName: 'Balanced Growth & Income', fundId: 'BALGIF', currentValue: 1327500.00, previousValue: 1308750.00, changePercent: 1.43 },
  { portfolioId: 'PORT0009', portfolioName: 'Small Cap Value Fund', fundId: 'HLTHIF', currentValue: 836000.00, previousValue: 822800.00, changePercent: 1.60 },
  { portfolioId: 'PORT0009', portfolioName: 'Small Cap Value Fund', fundId: 'EMERGE', currentValue: 280500.00, previousValue: 284750.00, changePercent: -1.49 },
  { portfolioId: 'PORT0010', portfolioName: 'Municipal Bond Portfolio', fundId: 'SMCAPV', currentValue: 472000.00, previousValue: 465600.00, changePercent: 1.37 },
  { portfolioId: 'PORT0010', portfolioName: 'Municipal Bond Portfolio', fundId: 'RETINC', currentValue: 2650000.00, previousValue: 2612500.00, changePercent: 1.44 },
  { portfolioId: 'PORT0011', portfolioName: 'ESG Sustainable Growth', fundId: 'REITPF', currentValue: 575000.00, previousValue: 563000.00, changePercent: 2.13 },
];

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', minimumFractionDigits: 2 }).format(value);
}

function downloadCsv(data: PositionReportRow[], filename: string) {
  const headers = ['Portfolio ID', 'Portfolio Name', 'Fund ID', 'Current Value', 'Previous Value', 'Change (%)', 'Change ($)'];
  const rows = data.map((r) => [
    r.portfolioId,
    r.portfolioName,
    r.fundId,
    r.currentValue.toFixed(2),
    r.previousValue.toFixed(2),
    r.changePercent.toFixed(2),
    (r.currentValue - r.previousValue).toFixed(2),
  ]);
  const csv = [headers.join(','), ...rows.map((r) => r.join(','))].join('\n');
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}

export function PositionReportPage() {
  const [filterPortfolioId, setFilterPortfolioId] = useState('');

  const portfolioIds = useMemo(
    () => [...new Set(positionReportData.map((r) => r.portfolioId))].sort(),
    [],
  );

  const filteredData = useMemo(
    () => filterPortfolioId ? positionReportData.filter((r) => r.portfolioId === filterPortfolioId) : positionReportData,
    [filterPortfolioId],
  );

  const summary = useMemo(() => {
    const totalCurrent = filteredData.reduce((s, r) => s + r.currentValue, 0);
    const totalPrevious = filteredData.reduce((s, r) => s + r.previousValue, 0);
    const overallChange = totalPrevious !== 0 ? ((totalCurrent - totalPrevious) / totalPrevious) * 100 : 0;
    return { totalCurrent, totalPrevious, overallChange };
  }, [filteredData]);

  const columns: Column<PositionReportRow>[] = [
    { key: 'portfolioId', header: 'Portfolio', sortable: true, render: (r) => (
      <div>
        <span className="font-medium text-slate-900">{r.portfolioId}</span>
        <span className="block text-xs text-slate-500">{r.portfolioName}</span>
      </div>
    )},
    { key: 'fundId', header: 'Fund ID', sortable: true, render: (r) => (
      <span className="font-mono text-sm">{r.fundId}</span>
    )},
    { key: 'currentValue', header: 'Current Value', sortable: true, className: 'text-right', render: (r) => (
      <span className="text-right block">{formatCurrency(r.currentValue)}</span>
    )},
    { key: 'previousValue', header: 'Previous Value', sortable: true, className: 'text-right', render: (r) => (
      <span className="text-right block">{formatCurrency(r.previousValue)}</span>
    )},
    { key: 'changePercent', header: 'Change (%)', sortable: true, className: 'text-right', render: (r) => (
      <span className={`text-right block font-medium ${r.changePercent >= 0 ? 'text-emerald-600' : 'text-red-600'}`}>
        {r.changePercent >= 0 ? '+' : ''}{r.changePercent.toFixed(2)}%
      </span>
    )},
    { key: 'changeDollar', header: 'Change ($)', sortable: true, className: 'text-right', render: (r) => {
      const change = r.currentValue - r.previousValue;
      return (
        <span className={`text-right block font-medium ${change >= 0 ? 'text-emerald-600' : 'text-red-600'}`}>
          {change >= 0 ? '+' : ''}{formatCurrency(change)}
        </span>
      );
    }},
  ];

  const today = new Date().toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' });
  const now = new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });

  return (
    <div>
      <PageHeader
        title="Position Report"
        description="Portfolio valuations generated by RPTPOS00"
        actions={
          <button
            onClick={() => downloadCsv(filteredData, `position-report-${new Date().toISOString().slice(0, 10)}.csv`)}
            className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors"
          >
            <Download className="w-4 h-4" />
            Download CSV
          </button>
        }
      />

      <div className="flex items-center gap-3 mb-4 text-sm text-slate-500">
        <FileText className="w-4 h-4" />
        <span>Generated: {today} at {now}</span>
      </div>

      <div className="flex items-center gap-4 mb-6">
        <label htmlFor="portfolio-filter" className="text-sm font-medium text-slate-700">
          Filter by Portfolio:
        </label>
        <select
          id="portfolio-filter"
          value={filterPortfolioId}
          onChange={(e) => setFilterPortfolioId(e.target.value)}
          className="px-3 py-2 text-sm border border-slate-300 rounded-lg bg-white text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
        >
          <option value="">All Portfolios</option>
          {portfolioIds.map((id) => {
            const portfolio = portfolios.find((p) => p.id === id);
            return (
              <option key={id} value={id}>
                {id} — {portfolio?.name ?? id}
              </option>
            );
          })}
        </select>
      </div>

      <Card>
        <div className="-m-6">
          <DataTable<PositionReportRow>
            columns={columns}
            data={filteredData}
            keyExtractor={(r) => `${r.portfolioId}-${r.fundId}`}
            emptyMessage="No positions match the selected filter"
          />

          {filteredData.length > 0 && (
            <div className="border-t-2 border-slate-300 bg-slate-50">
              <div className="grid grid-cols-6 px-4 py-3 text-sm font-semibold text-slate-900">
                <div className="col-span-2">Totals ({filteredData.length} positions)</div>
                <div className="text-right">{formatCurrency(summary.totalCurrent)}</div>
                <div className="text-right">{formatCurrency(summary.totalPrevious)}</div>
                <div className={`text-right font-medium ${summary.overallChange >= 0 ? 'text-emerald-600' : 'text-red-600'}`}>
                  {summary.overallChange >= 0 ? '+' : ''}{summary.overallChange.toFixed(2)}%
                </div>
                <div className={`text-right font-medium ${summary.totalCurrent - summary.totalPrevious >= 0 ? 'text-emerald-600' : 'text-red-600'}`}>
                  {summary.totalCurrent - summary.totalPrevious >= 0 ? '+' : ''}{formatCurrency(summary.totalCurrent - summary.totalPrevious)}
                </div>
              </div>
            </div>
          )}
        </div>
      </Card>
    </div>
  );
}
