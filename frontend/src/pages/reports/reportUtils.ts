import { portfolios, positions, transactions } from '@/data/mockData';
import type { Portfolio, Position } from '@/data/types';

export function formatCurrency(value: number): string {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value);
}

export function formatGainLoss(value: number): string {
  const formatted = formatCurrency(Math.abs(value));
  if (value > 0) return `+${formatted}`;
  if (value < 0) return `-${formatted}`;
  return formatted;
}

export function gainLossColor(value: number): string {
  if (value > 0) return 'text-emerald-600';
  if (value < 0) return 'text-red-600';
  return 'text-slate-700';
}

export function formatShares(value: number): string {
  return new Intl.NumberFormat('en-US', { minimumFractionDigits: 3, maximumFractionDigits: 3 }).format(value);
}

export function getPortfolioForAccount(accountNo: string): Portfolio | undefined {
  const index = parseInt(accountNo.slice(-2), 10) - 1;
  return portfolios[index];
}

const fundPrices: Record<string, number> = {};
for (const txn of [...transactions].sort((a, b) => a.transDate.localeCompare(b.transDate))) {
  if (txn.price > 0) fundPrices[txn.fundId] = txn.price;
}

export function getCurrentPrice(position: Position): number {
  return fundPrices[position.fundId] ?? position.avgCost;
}

export interface PositionReportRow extends Record<string, unknown> {
  portfolioId: string;
  portfolioName: string;
  accountNo: string;
  fundId: string;
  cusip: string;
  shareBalance: number;
  costBasis: number;
  marketValue: number;
  gainLoss: number;
}

export function buildPositionReportRows(): PositionReportRow[] {
  return positions
    .filter((p) => p.status === 'A')
    .map((p) => {
      const portfolio = getPortfolioForAccount(p.accountNo);
      const marketValue = p.shareBalance * getCurrentPrice(p);
      return {
        portfolioId: portfolio?.id ?? 'UNKNOWN',
        portfolioName: portfolio?.name ?? 'Unknown Portfolio',
        accountNo: p.accountNo,
        fundId: p.fundId,
        cusip: p.cusip,
        shareBalance: p.shareBalance,
        costBasis: p.costBasis,
        marketValue,
        gainLoss: marketValue - p.costBasis,
      };
    });
}

export function exportToCsv(filename: string, headers: string[], rows: (string | number)[][]): void {
  const escape = (value: string | number): string => {
    const str = String(value);
    return /[",\n]/.test(str) ? `"${str.replace(/"/g, '""')}"` : str;
  };
  const csv = [headers, ...rows].map((row) => row.map(escape).join(',')).join('\n');
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}
