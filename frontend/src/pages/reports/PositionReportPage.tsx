import { useState, useMemo } from 'react';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card } from '@/components/ui/Card';
import { portfolios, positions } from '@/data/mockData';
import { Download, Printer } from 'lucide-react';

interface PositionRow {
  portfolioId: string;
  description: string;
  quantity: number;
  marketValue: number;
  changePercent: number;
}

function buildPositionRows(): PositionRow[] {
  // Map each portfolio to its positions via account number.
  // Account numbers follow the pattern 1000000XX where XX = portfolio index (01-based).
  return portfolios
    .filter((p) => p.status !== 'C')
    .map((p) => {
      const portIndex = parseInt(p.id.replace('PORT', ''), 10);
      const accountNo = `1000000${String(portIndex).padStart(2, '0')}`;
      const portPositions = positions.filter(
        (pos) => pos.status === 'A' && pos.accountNo === accountNo
      );
      const totalQty = portPositions.reduce((sum, pos) => sum + pos.shareBalance, 0);
      const mockChange = ((Math.sin(portIndex * 3.7) * 15) * 100) / 100;
      return {
        portfolioId: p.id,
        description: p.name,
        quantity: totalQty,
        marketValue: p.totalValue,
        changePercent: parseFloat(mockChange.toFixed(2)),
      };
    });
}

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 2,
  }).format(value);
}

function formatNumber(value: number): string {
  return new Intl.NumberFormat('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value);
}

function formatChangePercent(value: number): string {
  const sign = value > 0 ? '+' : '';
  return `${sign}${value.toFixed(2)}%`;
}

function exportToCsv(rows: PositionRow[], reportDate: string): void {
  const header = 'Portfolio ID,Description,Quantity,Market Value,Change %';
  const csvRows = rows.map(
    (r) =>
      `${r.portfolioId},"${r.description}",${r.quantity.toFixed(2)},${r.marketValue.toFixed(2)},${r.changePercent.toFixed(2)}%`
  );
  const csv = [header, ...csvRows].join('\n');
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `position-report-${reportDate}.csv`;
  link.click();
  URL.revokeObjectURL(url);
}

export function PositionReportPage() {
  const [reportDate, setReportDate] = useState('2024-08-15');
  const rows = useMemo(() => buildPositionRows(), []);

  const totalPortfolios = rows.length;
  const totalMarketValue = rows.reduce((sum, r) => sum + r.marketValue, 0);
  const avgChange = rows.reduce((sum, r) => sum + r.changePercent, 0) / rows.length;

  return (
    <div>
      <PageHeader
        title="Position Report"
        description="Modernized from COBOL program RPTPOS00 \u2014 Daily Position Report Generator"
        actions={
          <div className="flex items-center gap-3">
            <input
              type="date"
              value={reportDate}
              onChange={(e) => setReportDate(e.target.value)}
              className="rounded-md border border-slate-300 px-3 py-1.5 text-sm text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            <button
              onClick={() => exportToCsv(rows, reportDate)}
              className="inline-flex items-center gap-2 rounded-md bg-blue-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-blue-700 transition-colors"
            >
              <Download className="w-4 h-4" />
              Export to CSV
            </button>
            <button
              onClick={() => window.print()}
              className="inline-flex items-center gap-2 rounded-md border border-slate-300 bg-white px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-50 transition-colors"
            >
              <Printer className="w-4 h-4" />
              Print Report
            </button>
          </div>
        }
      />

      <Card>
        <div className="text-center mb-6 print:mb-4">
          <h2 className="text-xl font-bold text-slate-900 tracking-wide">DAILY POSITION REPORT</h2>
          <p className="text-sm text-slate-500 mt-1">Report Date: {reportDate}</p>
        </div>

        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-slate-200">
            <thead className="bg-slate-50">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Portfolio ID</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Description</th>
                <th className="px-4 py-3 text-right text-xs font-semibold text-slate-600 uppercase tracking-wider">Quantity</th>
                <th className="px-4 py-3 text-right text-xs font-semibold text-slate-600 uppercase tracking-wider">Market Value</th>
                <th className="px-4 py-3 text-right text-xs font-semibold text-slate-600 uppercase tracking-wider">Change %</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-slate-200">
              {rows.map((row) => {
                const isException = row.changePercent > 10 || row.changePercent < -10;
                return (
                  <tr
                    key={row.portfolioId}
                    className={`hover:bg-slate-50 transition-colors ${isException ? 'bg-yellow-50' : ''}`}
                  >
                    <td className="px-4 py-3 text-sm font-mono text-slate-700">{row.portfolioId}</td>
                    <td className="px-4 py-3 text-sm text-slate-700">{row.description}</td>
                    <td className="px-4 py-3 text-sm text-slate-700 text-right tabular-nums">{formatNumber(row.quantity)}</td>
                    <td className="px-4 py-3 text-sm text-slate-700 text-right tabular-nums">{formatCurrency(row.marketValue)}</td>
                    <td className={`px-4 py-3 text-sm text-right font-medium tabular-nums ${row.changePercent > 0 ? 'text-emerald-600' : row.changePercent < 0 ? 'text-red-600' : 'text-slate-700'}`}>
                      {formatChangePercent(row.changePercent)}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>

        <div className="mt-6 pt-4 border-t border-slate-200">
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="bg-slate-50 rounded-lg p-4 text-center">
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Total Portfolios</p>
              <p className="mt-1 text-2xl font-bold text-slate-900">{totalPortfolios}</p>
            </div>
            <div className="bg-slate-50 rounded-lg p-4 text-center">
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Total Market Value</p>
              <p className="mt-1 text-2xl font-bold text-slate-900">{formatCurrency(totalMarketValue)}</p>
            </div>
            <div className="bg-slate-50 rounded-lg p-4 text-center">
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Average Change %</p>
              <p className={`mt-1 text-2xl font-bold ${avgChange > 0 ? 'text-emerald-600' : avgChange < 0 ? 'text-red-600' : 'text-slate-900'}`}>
                {formatChangePercent(avgChange)}
              </p>
            </div>
          </div>
        </div>
      </Card>
    </div>
  );
}
