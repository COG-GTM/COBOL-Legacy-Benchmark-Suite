import { useState, useMemo, useCallback } from 'react';
import { FileText, Download, Briefcase, TrendingUp, DollarSign, BarChart3 } from 'lucide-react';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card } from '@/components/ui/Card';
import { SearchInput } from '@/components/ui/SearchInput';
import { StatusBadge, getPositionStatusVariant, getPositionStatusLabel } from '@/components/ui/StatusBadge';
import { positions, portfolios } from '@/data/mockData';
import type { Position } from '@/data/types';

const currencyFmt = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', minimumFractionDigits: 2 });
const numberFmt = new Intl.NumberFormat('en-US', { minimumFractionDigits: 3, maximumFractionDigits: 3 });
const costFmt = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', minimumFractionDigits: 2 });

type StatusFilter = 'All' | 'A' | 'C';

interface GroupedPositions {
  accountNo: string;
  portfolioName: string;
  positions: Position[];
  totalCostBasis: number;
  totalMarketValue: number;
  totalGainLoss: number;
}

function getMarketValue(pos: Position): number {
  return pos.shareBalance * pos.avgCost;
}

function getGainLoss(pos: Position): number {
  return getMarketValue(pos) - pos.costBasis;
}

function getPortfolioName(accountNo: string): string {
  const index = parseInt(accountNo.slice(-3), 10) - 1;
  return portfolios[index]?.name ?? '';
}

export function PositionReportPage() {
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('All');
  const [searchQuery, setSearchQuery] = useState('');

  const filteredPositions = useMemo(() => {
    let result = positions;
    if (statusFilter !== 'All') {
      result = result.filter((p) => p.status === statusFilter);
    }
    if (searchQuery.trim()) {
      const q = searchQuery.trim().toLowerCase();
      result = result.filter((p) => p.accountNo.toLowerCase().includes(q));
    }
    return result;
  }, [statusFilter, searchQuery]);

  const groups = useMemo((): GroupedPositions[] => {
    const map = new Map<string, Position[]>();
    for (const pos of filteredPositions) {
      const existing = map.get(pos.accountNo);
      if (existing) {
        existing.push(pos);
      } else {
        map.set(pos.accountNo, [pos]);
      }
    }
    return Array.from(map.entries()).map(([accountNo, acctPositions]) => ({
      accountNo,
      portfolioName: getPortfolioName(accountNo),
      positions: acctPositions,
      totalCostBasis: acctPositions.reduce((s, p) => s + p.costBasis, 0),
      totalMarketValue: acctPositions.reduce((s, p) => s + getMarketValue(p), 0),
      totalGainLoss: acctPositions.reduce((s, p) => s + getGainLoss(p), 0),
    }));
  }, [filteredPositions]);

  const summary = useMemo(() => {
    const uniqueAccounts = new Set(filteredPositions.map((p) => p.accountNo));
    const activePositions = filteredPositions.filter((p) => p.status === 'A').length;
    const totalCostBasis = filteredPositions.reduce((s, p) => s + p.costBasis, 0);
    const totalMarketValue = filteredPositions.reduce((s, p) => s + getMarketValue(p), 0);
    return { totalPortfolios: uniqueAccounts.size, activePositions, totalCostBasis, totalMarketValue };
  }, [filteredPositions]);

  const grandTotals = useMemo(() => ({
    costBasis: groups.reduce((s, g) => s + g.totalCostBasis, 0),
    marketValue: groups.reduce((s, g) => s + g.totalMarketValue, 0),
    gainLoss: groups.reduce((s, g) => s + g.totalGainLoss, 0),
  }), [groups]);

  const handleExportCsv = useCallback(() => {
    const headers = ['Account No', 'Fund ID', 'CUSIP', 'Share Balance', 'Avg Cost', 'Cost Basis', 'Market Value', 'Gain/Loss', 'Status'];
    const rows = filteredPositions.map((p) => [
      p.accountNo,
      p.fundId,
      p.cusip,
      p.shareBalance.toFixed(3),
      p.avgCost.toFixed(2),
      p.costBasis.toFixed(2),
      getMarketValue(p).toFixed(2),
      getGainLoss(p).toFixed(2),
      p.status === 'A' ? 'Active' : 'Closed',
    ]);
    const csv = [headers.join(','), ...rows.map((r) => r.join(','))].join('\n');
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `position-report-${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }, [filteredPositions]);

  const now = new Date();
  const reportDate = now.toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' });
  const reportTime = now.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });

  const summaryCards = [
    { label: 'Total Portfolios', value: summary.totalPortfolios.toString(), icon: <Briefcase className="w-6 h-6" />, color: 'text-blue-600 bg-blue-50' },
    { label: 'Active Positions', value: summary.activePositions.toString(), icon: <TrendingUp className="w-6 h-6" />, color: 'text-emerald-600 bg-emerald-50' },
    { label: 'Total Cost Basis', value: currencyFmt.format(summary.totalCostBasis), icon: <DollarSign className="w-6 h-6" />, color: 'text-violet-600 bg-violet-50' },
    { label: 'Total Market Value', value: currencyFmt.format(summary.totalMarketValue), icon: <BarChart3 className="w-6 h-6" />, color: 'text-amber-600 bg-amber-50' },
  ];

  return (
    <div>
      <PageHeader
        title="Position Report"
        description={`Generated on ${reportDate} at ${reportTime}`}
        actions={
          <button
            onClick={handleExportCsv}
            className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors"
          >
            <Download className="w-4 h-4" />
            Export CSV
          </button>
        }
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
        title="Position Details"
        actions={
          <div className="flex items-center gap-3">
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value as StatusFilter)}
              className="text-sm border border-slate-300 rounded-lg px-3 py-2 bg-white text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="All">All Statuses</option>
              <option value="A">Active</option>
              <option value="C">Closed</option>
            </select>
            <SearchInput
              value={searchQuery}
              onChange={setSearchQuery}
              placeholder="Search account no..."
              className="w-48"
            />
          </div>
        }
      >
        <div className="overflow-x-auto -m-6 mt-0">
          <table className="min-w-full divide-y divide-slate-200">
            <thead className="bg-slate-50">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase">Fund ID</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase">CUSIP</th>
                <th className="px-4 py-3 text-right text-xs font-semibold text-slate-600 uppercase">Share Balance</th>
                <th className="px-4 py-3 text-right text-xs font-semibold text-slate-600 uppercase">Avg Cost</th>
                <th className="px-4 py-3 text-right text-xs font-semibold text-slate-600 uppercase">Cost Basis</th>
                <th className="px-4 py-3 text-right text-xs font-semibold text-slate-600 uppercase">Market Value</th>
                <th className="px-4 py-3 text-right text-xs font-semibold text-slate-600 uppercase">Gain/Loss</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200">
              {groups.map((group) => (
                <GroupRows key={group.accountNo} group={group} />
              ))}
              {groups.length > 0 && (
                <tr className="bg-slate-100 font-bold">
                  <td colSpan={4} className="px-4 py-3 text-sm text-slate-900">Grand Total</td>
                  <td className="px-4 py-3 text-sm text-slate-900 text-right">{currencyFmt.format(grandTotals.costBasis)}</td>
                  <td className="px-4 py-3 text-sm text-slate-900 text-right">{currencyFmt.format(grandTotals.marketValue)}</td>
                  <td className={`px-4 py-3 text-sm text-right font-bold ${grandTotals.gainLoss >= 0 ? 'text-emerald-700' : 'text-red-700'}`}>
                    {currencyFmt.format(grandTotals.gainLoss)}
                  </td>
                  <td />
                </tr>
              )}
              {groups.length === 0 && (
                <tr>
                  <td colSpan={8} className="px-4 py-12 text-center text-sm text-slate-500">
                    No positions found matching the current filters.
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

function GroupRows({ group }: { group: GroupedPositions }) {
  return (
    <>
      <tr className="bg-blue-50">
        <td colSpan={8} className="px-4 py-2">
          <div className="flex items-center gap-2">
            <FileText className="w-4 h-4 text-blue-600" />
            <span className="text-sm font-semibold text-slate-900">{group.accountNo}</span>
            {group.portfolioName && (
              <span className="text-sm text-slate-600">— {group.portfolioName}</span>
            )}
          </div>
        </td>
      </tr>
      {group.positions.map((pos) => {
        const mv = getMarketValue(pos);
        const gl = getGainLoss(pos);
        return (
          <tr key={`${pos.accountNo}-${pos.fundId}`} className="hover:bg-slate-50 transition-colors">
            <td className="px-4 py-3 text-sm font-mono text-slate-900">{pos.fundId}</td>
            <td className="px-4 py-3 text-sm font-mono text-slate-600">{pos.cusip}</td>
            <td className="px-4 py-3 text-sm text-slate-700 text-right">{numberFmt.format(pos.shareBalance)}</td>
            <td className="px-4 py-3 text-sm text-slate-700 text-right">{costFmt.format(pos.avgCost)}</td>
            <td className="px-4 py-3 text-sm text-slate-700 text-right">{currencyFmt.format(pos.costBasis)}</td>
            <td className="px-4 py-3 text-sm text-slate-700 text-right">{currencyFmt.format(mv)}</td>
            <td className={`px-4 py-3 text-sm text-right ${gl >= 0 ? 'text-emerald-700' : 'text-red-700'}`}>
              {currencyFmt.format(gl)}
            </td>
            <td className="px-4 py-3">
              <StatusBadge label={getPositionStatusLabel(pos.status)} variant={getPositionStatusVariant(pos.status)} />
            </td>
          </tr>
        );
      })}
      <tr className="bg-slate-50">
        <td colSpan={4} className="px-4 py-2 text-sm font-medium text-slate-700 text-right">Subtotal</td>
        <td className="px-4 py-2 text-sm font-semibold text-slate-900 text-right">{currencyFmt.format(group.totalCostBasis)}</td>
        <td className="px-4 py-2 text-sm font-semibold text-slate-900 text-right">{currencyFmt.format(group.totalMarketValue)}</td>
        <td className={`px-4 py-2 text-sm font-semibold text-right ${group.totalGainLoss >= 0 ? 'text-emerald-700' : 'text-red-700'}`}>
          {currencyFmt.format(group.totalGainLoss)}
        </td>
        <td />
      </tr>
    </>
  );
}
