import type { Portfolio } from '@/types/portfolio'
import type { Transaction } from '@/types/transaction'

import portfoliosJson from './portfolios.json'
import transactionsJson from './transactions.json'

/** Fixed "business date" for the mock data set so the dashboard is deterministic. */
export const MOCK_AS_OF_DATE = '2026-10-14'

export const mockPortfolios = portfoliosJson as Portfolio[]
export const mockTransactions = transactionsJson as Transaction[]
