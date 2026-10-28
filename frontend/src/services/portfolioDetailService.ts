import { sumCents } from '@/lib/money'
import type { Portfolio } from '@/types/portfolio'
import type { Position } from '@/types/position'
import type { Transaction } from '@/types/transaction'

import { compareNewestFirst } from './dashboardService'

export const DETAIL_RECENT_TRANSACTIONS_LIMIT = 5

export interface PortfolioDetail {
  portfolio: Portfolio
  /** Current holdings (latest open record per investment), largest market value first. */
  positions: Position[]
  /** PORT-TOTAL-VALUE minus PORT-CASH-BALANCE, in exact cents. */
  investedValueCents: bigint
  positionsMarketValueCents: bigint
  positionsCostBasisCents: bigint
  /** Latest transactions for this portfolio, newest first. */
  recentTransactions: Transaction[]
  transactionCount: number
}

/** PORT-ID is PIC X(8): IDs compare case-insensitively with trailing blanks ignored. */
export function normalizePortfolioId(id: string): string {
  return id.trim().toUpperCase()
}

/**
 * POSREC is keyed by portfolio + date + investment, so one investment can have
 * several dated snapshots. Keep the latest per investment and drop closed ones.
 */
export function currentHoldings(positions: Position[]): Position[] {
  const latest = new Map<string, Position>()
  for (const pos of positions) {
    const seen = latest.get(pos.investmentId)
    if (!seen || pos.date > seen.date) latest.set(pos.investmentId, pos)
  }
  return [...latest.values()].filter((pos) => pos.status !== 'C')
}

/**
 * Assembles the FR-4 detail view for one PORT-RECORD (the equivalent of a keyed
 * VSAM READ on PORTFLIO plus browses of the position and transaction files).
 * Returns null when no portfolio has the given ID (CICS NOTFND).
 */
export function buildPortfolioDetail(
  portfolioId: string,
  portfolios: Portfolio[],
  positions: Position[],
  transactions: Transaction[],
  recentLimit = DETAIL_RECENT_TRANSACTIONS_LIMIT,
): PortfolioDetail | null {
  const id = normalizePortfolioId(portfolioId)
  const portfolio = portfolios.find((p) => p.portfolioId === id)
  if (!portfolio) return null

  const holdings = currentHoldings(positions.filter((pos) => pos.portfolioId === id)).sort(
    (a, b) => b.marketValue - a.marketValue || a.investmentId.localeCompare(b.investmentId),
  )
  const history = transactions.filter((txn) => txn.portfolioId === id).sort(compareNewestFirst)

  return {
    portfolio,
    positions: holdings,
    investedValueCents: sumCents([portfolio.totalValue]) - sumCents([portfolio.cashBalance]),
    positionsMarketValueCents: sumCents(holdings.map((pos) => pos.marketValue)),
    positionsCostBasisCents: sumCents(holdings.map((pos) => pos.costBasis)),
    recentTransactions: history.slice(0, recentLimit),
    transactionCount: history.length,
  }
}
