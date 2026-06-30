import { PORTFOLIO_FIXTURE } from '../data/portfolios.fixture';
import { TRANSACTION_FIXTURE } from '../data/transactions.fixture';
import type { Portfolio } from '../types/portfolio';
import type { Transaction } from '../types/transaction';
import { addDecimals } from '../utils/decimal';

export interface DashboardMetrics {
  /** Total assets under management — sum of every portfolio's total value. */
  readonly totalAum: string;
  /** Count of portfolios with an active (status 'A') record. */
  readonly activePortfolios: number;
  /** Total number of portfolio master records. */
  readonly totalPortfolios: number;
  /** Most recent transactions, newest first. */
  readonly recentTransactions: readonly Transaction[];
}

/** Sorts transactions newest-first by their YYYYMMDD date + HHMMSS time key. */
function byMostRecent(a: Transaction, b: Transaction): number {
  return `${b.date}${b.time}`.localeCompare(`${a.date}${a.time}`);
}

/**
 * Computes the dashboard summary metrics from the in-memory fixtures. This
 * stands in for a backend aggregation endpoint until the API is connected;
 * keeping it pure makes it trivially testable.
 */
export function getDashboardMetrics(
  portfolios: readonly Portfolio[] = PORTFOLIO_FIXTURE,
  transactions: readonly Transaction[] = TRANSACTION_FIXTURE,
  recentLimit = 5,
): DashboardMetrics {
  const totalAum = portfolios.reduce(
    (sum, p) => addDecimals(sum, p.totalValue),
    '0.00',
  );
  const activePortfolios = portfolios.filter((p) => p.status === 'A').length;
  const recentTransactions = [...transactions]
    .sort(byMostRecent)
    .slice(0, recentLimit);

  return {
    totalAum,
    activePortfolios,
    totalPortfolios: portfolios.length,
    recentTransactions,
  };
}
