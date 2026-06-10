import { useEffect, useMemo, useState } from 'react';
import { Download, ChevronUp, ChevronDown, ChevronsUpDown } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { PageHeader } from '@/components/ui/PageHeader';
import { LoadingSpinner } from '@/components/ui/LoadingSpinner';
import { EmptyState } from '@/components/ui/EmptyState';
import {
  buildPositionReportRows,
  exportToCsv,
  formatCurrency,
  formatGainLoss,
  formatShares,
  gainLossColor,
} from './reportUtils';
import type { PositionReportRow } from './reportUtils';

type SortKey = 'fundId' | 'shareBalance' | 'costBasis' | 'marketValue' | 'gainLoss';
type SortDirection = 'asc' | 'desc';

const sortableColumns: { key: SortKey; header: string; numeric: boolean }[] = [
  { key: 'fundId', header: 'Fund', numeric: false },
  { key: 'shareBalance', header: 'Shares', numeric: true },
  { key: 'costBasis', header: 'Cost Basis', numeric: true },
  { key: 'marketValue', header: 'Market Value', numeric: true },
  { key: 'gainLoss', header: 'Gain/Loss', numeric: true },
];

interface PortfolioGroup {
  portfolioId: string;
  portfolioName: string;
  rows: PositionReportRow[];
  subtotalCost: number;
  subtotalMarket: number;
  subtotalGainLoss: number;
}

export function PositionReportPage() {
  const [loading, setLoading] = useState(true);
  const [sortKey, setSortKey] = useState<SortKey>('fundId');
  const [sortDir, setSortDir] = useState<SortDirection>('asc');

  useEffect(() => {
    const timer = setTimeout(() => setLoading(false), 400);
    return () => clearTimeout(timer);
  }, []);

  const rows = useMemo(() => buildPositionReportRows(), []);

  const groups = useMemo<PortfolioGroup[]>(() => {
    const byPortfolio = new Map<string, PositionReportRow[]>();
    for (const row of rows) {
      const existing = byPortfolio.get(row.portfolioId);
      if (existing) existing.push(row);
      else byPortfolio.set(row.portfolioId, [row]);
    }
    const result: PortfolioGroup[] = [];
    for (const [portfolioId, groupRows] of byPortfolio) {
      const sorted = [...groupRows].sort((a, b) => {
        const aVal = a[sortKey];
        const bVal = b[sortKey];
        const cmp =
          typeof aVal === 'number' && typeof bVal === 'number'
            ? aVal - bVal
            : String(aVal).localeCompare(String(bVal));
        return sortDir === 'asc' ? cmp : -cmp;
      });
      result.push({
        portfolioId,
        portfolioName: groupRows[0].portfolioName,
        rows: sorted,
        subtotalCost: groupRows.reduce((sum, r) => sum + r.costBasis, 0),
        subtotalMarket: groupRows.reduce((sum, r) => sum + r.marketValue, 0),
        subtotalGainLoss: groupRows.reduce((sum, r) => sum + r.gainLoss, 0),
      });
    }
    return result.sort((a, b) => a.portfolioId.localeCompare(b.portfolioId));
  }, [rows, sortKey, sortDir]);

  const grandTotals = useMemo(
    () => ({
      cost: rows.reduce((sum, r) => sum + r.costBasis, 0),
      market: rows.reduce((sum, r) => sum + r.marketValue, 0),
      gainLoss: rows.reduce((sum, r) => sum + r.gainLoss, 0),
    }),
    [rows],
  );

  const handleSort = (key: SortKey) => {
    if (sortKey === key) {
      setSortDir(sortDir === 'asc' ? 'desc' : 'asc');
    } else {
      setSortKey(key);
      setSortDir('asc');
    }
  };

  const handleExport = () => {
    exportToCsv(
      'position-report.csv',
      ['Portfolio ID', 'Portfolio Name', 'Account', 'Fund', 'CUSIP', 'Shares', 'Cost Basis', 'Market Value', 'Gain/Loss'],
      rows.map((r) => [
        r.portfolioId,
        r.portfolioName,
        r.accountNo,
        r.fundId,
        r.cusip,
        r.shareBalance.toFixed(3),
        r.costBasis.toFixed(2),
        r.marketValue.toFixed(2),
        r.gainLoss.toFixed(2),
      ]),
    );
  };

  if (loading) {
    return (
      <div>
        <PageHeader title="Position Report" description="Holdings across all portfolios with cost basis, market value, and gain/loss" />
        <LoadingSpinner message="Generating position report..." />
      </div>
    );
  }

  return (
    <div>
      <PageHeader
        title="Position Report"
        description="Holdings across all portfolios with cost basis, market value, and gain/loss"
        actions={
          <button
            onClick={handleExport}
            className="inline-flex items-center gap-2 rounded-md bg-blue-600 px-3 py-2 text-sm font-medium text-white hover:bg-blue-700 transition-colors"
          >
            <Download className="w-4 h-4" />
            Export CSV
          </button>
        }
      />
      {groups.length === 0 ? (
        <Card>
          <EmptyState title="No positions found" message="There are no active positions to report." />
        </Card>
      ) : (
        <div className="space-y-6">
          {groups.map((group) => (
            <Card key={group.portfolioId} title={`${group.portfolioId} — ${group.portfolioName}`}>
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-slate-200">
                  <thead className="bg-slate-50">
                    <tr>
                      <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Account</th>
                      <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">CUSIP</th>
                      {sortableColumns.map((col) => (
                        <th
                          key={col.key}
                          className={`px-4 py-3 text-xs font-semibold text-slate-600 uppercase tracking-wider cursor-pointer select-none hover:bg-slate-100 ${col.numeric ? 'text-right' : 'text-left'}`}
                          onClick={() => handleSort(col.key)}
                        >
                          <div className={`flex items-center gap-1 ${col.numeric ? 'justify-end' : ''}`}>
                            {col.header}
                            <span className="text-slate-400">
                              {sortKey === col.key ? (
                                sortDir === 'asc' ? (
                                  <ChevronUp className="w-3.5 h-3.5" />
                                ) : (
                                  <ChevronDown className="w-3.5 h-3.5" />
                                )
                              ) : (
                                <ChevronsUpDown className="w-3.5 h-3.5" />
                              )}
                            </span>
                          </div>
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody className="bg-white divide-y divide-slate-200">
                    {group.rows.map((row) => (
                      <tr key={`${row.accountNo}-${row.fundId}`} className="hover:bg-slate-50 transition-colors">
                        <td className="px-4 py-3 text-sm text-slate-700 whitespace-nowrap">{row.accountNo}</td>
                        <td className="px-4 py-3 text-sm text-slate-700 whitespace-nowrap">{row.cusip}</td>
                        <td className="px-4 py-3 text-sm text-slate-700 whitespace-nowrap font-medium">{row.fundId}</td>
                        <td className="px-4 py-3 text-sm text-slate-700 whitespace-nowrap text-right">{formatShares(row.shareBalance)}</td>
                        <td className="px-4 py-3 text-sm text-slate-700 whitespace-nowrap text-right">{formatCurrency(row.costBasis)}</td>
                        <td className="px-4 py-3 text-sm text-slate-700 whitespace-nowrap text-right">{formatCurrency(row.marketValue)}</td>
                        <td className={`px-4 py-3 text-sm whitespace-nowrap text-right font-medium ${gainLossColor(row.gainLoss)}`}>
                          {formatGainLoss(row.gainLoss)}
                        </td>
                      </tr>
                    ))}
                    <tr className="bg-slate-50 font-semibold">
                      <td colSpan={4} className="px-4 py-3 text-sm text-slate-900">
                        Subtotal
                      </td>
                      <td className="px-4 py-3 text-sm text-slate-900 whitespace-nowrap text-right">{formatCurrency(group.subtotalCost)}</td>
                      <td className="px-4 py-3 text-sm text-slate-900 whitespace-nowrap text-right">{formatCurrency(group.subtotalMarket)}</td>
                      <td className={`px-4 py-3 text-sm whitespace-nowrap text-right ${gainLossColor(group.subtotalGainLoss)}`}>
                        {formatGainLoss(group.subtotalGainLoss)}
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </Card>
          ))}
          <Card>
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
              <h3 className="text-lg font-semibold text-slate-900">Grand Total</h3>
              <div className="flex flex-col sm:flex-row gap-4 sm:gap-8">
                <div className="text-right">
                  <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Cost Basis</p>
                  <p className="text-lg font-semibold text-slate-900">{formatCurrency(grandTotals.cost)}</p>
                </div>
                <div className="text-right">
                  <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Market Value</p>
                  <p className="text-lg font-semibold text-slate-900">{formatCurrency(grandTotals.market)}</p>
                </div>
                <div className="text-right">
                  <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Gain/Loss</p>
                  <p className={`text-lg font-semibold ${gainLossColor(grandTotals.gainLoss)}`}>{formatGainLoss(grandTotals.gainLoss)}</p>
                </div>
              </div>
            </div>
          </Card>
        </div>
      )}
    </div>
  );
}
