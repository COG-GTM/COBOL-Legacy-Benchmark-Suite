import { sumCents } from '@/lib/money'
import type { Portfolio, PortfolioStatus } from '@/types/portfolio'
import type { Transaction } from '@/types/transaction'

export const RECENT_WINDOW_DAYS = 7
export const RECENT_TRANSACTIONS_LIMIT = 5

export interface DashboardSummary {
  totalPortfolios: number
  portfoliosByStatus: Record<PortfolioStatus, number>
  /** Exact integer cents: aggregates of S9(13)V99 values can exceed Number.MAX_SAFE_INTEGER cents. */
  totalValueCents: bigint
  totalCashCents: bigint
  recentTransactionCount: number
  pendingTransactionCount: number
  recentTransactions: Transaction[]
  windowStart: string
  asOf: string
}

const DAY_MS = 24 * 60 * 60 * 1000

function toUtcMs(isoDate: string): number {
  return Date.parse(`${isoDate}T00:00:00Z`)
}

function toIsoDate(ms: number): string {
  return new Date(ms).toISOString().slice(0, 10)
}

export function compareNewestFirst(a: Transaction, b: Transaction): number {
  return `${b.date}T${b.time}`.localeCompare(`${a.date}T${a.time}`) || b.sequenceNo.localeCompare(a.sequenceNo)
}

/**
 * Builds the dashboard summary cards. "Recent" means the RECENT_WINDOW_DAYS calendar
 * days ending on (and including) `asOf`. Totals are reported in the base currency;
 * the mock portfolios are all USD-denominated.
 */
export function buildDashboardSummary(
  portfolios: Portfolio[],
  transactions: Transaction[],
  asOf: string,
  windowDays = RECENT_WINDOW_DAYS,
): DashboardSummary {
  const asOfMs = toUtcMs(asOf)
  const windowStart = toIsoDate(asOfMs - (windowDays - 1) * DAY_MS)

  const portfoliosByStatus: Record<PortfolioStatus, number> = { A: 0, S: 0, C: 0 }
  for (const portfolio of portfolios) portfoliosByStatus[portfolio.status] += 1

  const recent = transactions
    .filter((txn) => txn.date >= windowStart && txn.date <= asOf)
    .sort(compareNewestFirst)

  return {
    totalPortfolios: portfolios.length,
    portfoliosByStatus,
    totalValueCents: sumCents(portfolios.map((p) => p.totalValue)),
    totalCashCents: sumCents(portfolios.map((p) => p.cashBalance)),
    recentTransactionCount: recent.length,
    pendingTransactionCount: recent.filter((t) => t.status === 'P').length,
    recentTransactions: recent.slice(0, RECENT_TRANSACTIONS_LIMIT),
    windowStart,
    asOf,
  }
}
