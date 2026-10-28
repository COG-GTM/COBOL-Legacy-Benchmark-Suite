import { describe, expect, it } from 'vitest'

import { MOCK_AS_OF_DATE, mockPortfolios, mockTransactions } from '@/data/mockData'
import type { Portfolio } from '@/types/portfolio'
import type { Transaction } from '@/types/transaction'

import { buildDashboardSummary, RECENT_TRANSACTIONS_LIMIT } from './dashboardService'

const portfolio = (overrides: Partial<Portfolio>): Portfolio => ({
  portfolioId: 'PORT0001',
  accountNo: '1000000001',
  clientName: 'Test',
  clientType: 'I',
  createDate: '2020-01-01',
  lastMaint: '2026-10-01',
  status: 'A',
  totalValue: 0,
  cashBalance: 0,
  lastUser: 'OPER01',
  lastTrans: '2026-10-01',
  ...overrides,
})

const txn = (overrides: Partial<Transaction>): Transaction => ({
  date: '2026-10-14',
  time: '10:00:00',
  portfolioId: 'PORT0001',
  sequenceNo: '000001',
  investmentId: 'IBM0000001',
  type: 'BU',
  quantity: 1,
  price: 1,
  amount: 1,
  currency: 'USD',
  status: 'D',
  ...overrides,
})

describe('buildDashboardSummary', () => {
  it('counts portfolios by status', () => {
    const summary = buildDashboardSummary(
      [portfolio({ status: 'A' }), portfolio({ status: 'A' }), portfolio({ status: 'S' }), portfolio({ status: 'C' })],
      [],
      '2026-10-14',
    )
    expect(summary.totalPortfolios).toBe(4)
    expect(summary.portfoliosByStatus).toEqual({ A: 2, S: 1, C: 1 })
  })

  it('sums V99 money fields without floating point drift', () => {
    const summary = buildDashboardSummary(
      [portfolio({ totalValue: 0.1, cashBalance: 0.7 }), portfolio({ totalValue: 0.2, cashBalance: 0.1 })],
      [],
      '2026-10-14',
    )
    expect(summary.totalValueCents).toBe(30n)
    expect(summary.totalCashCents).toBe(80n)
  })

  it('keeps cents exact for totals beyond Number.MAX_SAFE_INTEGER cents', () => {
    const max = portfolio({ totalValue: 9999999999999.99, cashBalance: 9999999999999.99 })
    const summary = buildDashboardSummary(Array(10).fill(max), [], '2026-10-14')
    expect(summary.totalValueCents).toBe(9999999999999990n)
    expect(summary.totalCashCents).toBe(9999999999999990n)
  })

  it('only counts transactions inside the window ending on the as-of date', () => {
    const summary = buildDashboardSummary(
      [],
      [
        txn({ date: '2026-10-14', sequenceNo: '000001', status: 'P' }),
        txn({ date: '2026-10-08', sequenceNo: '000002' }),
        txn({ date: '2026-10-07', sequenceNo: '000003' }),
        txn({ date: '2026-10-15', sequenceNo: '000004' }),
      ],
      '2026-10-14',
      7,
    )
    expect(summary.windowStart).toBe('2026-10-08')
    expect(summary.recentTransactionCount).toBe(2)
    expect(summary.pendingTransactionCount).toBe(1)
    expect(summary.recentTransactions.map((t) => t.sequenceNo)).toEqual(['000001', '000002'])
  })

  it('lists recent transactions newest first, capped at the limit', () => {
    const transactions = Array.from({ length: 8 }, (_, i) =>
      txn({ date: '2026-10-13', time: `0${i}:00:00`, sequenceNo: `00000${i}` }),
    )
    const summary = buildDashboardSummary([], transactions, '2026-10-14')
    expect(summary.recentTransactions).toHaveLength(RECENT_TRANSACTIONS_LIMIT)
    expect(summary.recentTransactions[0].time).toBe('07:00:00')
  })

  it('summarises the mock data set', () => {
    const summary = buildDashboardSummary(mockPortfolios, mockTransactions, MOCK_AS_OF_DATE)
    expect(summary.totalPortfolios).toBe(8)
    expect(summary.portfoliosByStatus).toEqual({ A: 6, S: 1, C: 1 })
    expect(summary.totalValueCents).toBe(2869795045n)
    expect(summary.totalCashCents).toBe(202578153n)
    expect(summary.recentTransactionCount).toBe(9)
    expect(summary.pendingTransactionCount).toBe(1)
  })
})
