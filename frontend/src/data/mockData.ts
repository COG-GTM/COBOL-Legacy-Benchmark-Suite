import type { Portfolio } from '@/types/portfolio'
import type { Position } from '@/types/position'
import type { Transaction } from '@/types/transaction'

import portfoliosJson from './portfolios.json'
import positionsJson from './positions.json'
import transactionsJson from './transactions.json'

/** Fixed "business date" for the mock data set so the dashboard is deterministic. */
export const MOCK_AS_OF_DATE = '2026-10-14'

export const mockPortfolios = portfoliosJson as Portfolio[]
export const mockTransactions = transactionsJson as Transaction[]
export const mockPositions = positionsJson as Position[]
