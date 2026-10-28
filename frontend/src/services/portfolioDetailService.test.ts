import { describe, expect, it } from 'vitest'

import { mockPortfolios, mockPositions, mockTransactions } from '@/data/mockData'
import { sumCents } from '@/lib/money'
import type { Portfolio } from '@/types/portfolio'
import type { Position } from '@/types/position'
import type { Transaction } from '@/types/transaction'

import { buildPortfolioDetail } from './portfolioDetailService'

const portfolio = (overrides: Partial<Portfolio> = {}): Portfolio => ({
  portfolioId: 'PORT9001',
  accountNo: '9000000001',
  clientName: 'Test Client',
  clientType: 'I',
  createDate: '2020-01-01',
  lastMaint: '2026-10-01',
  status: 'A',
  totalValue: 1000.1,
  cashBalance: 0.2,
  lastUser: 'OPER01',
  lastTrans: '2026-10-01',
  ...overrides,
})

const position = (investmentId: string, marketValue: number, overrides: Partial<Position> = {}): Position => ({
  portfolioId: 'PORT9001',
  date: '2026-10-14',
  investmentId,
  quantity: 1,
  costBasis: marketValue,
  marketValue,
  currency: 'USD',
  status: 'A',
  lastMaintDate: '2026-10-14-00.00.00.000000',
  lastMaintUser: 'OPER01',
  ...overrides,
})

const txn = (sequenceNo: string, date: string, overrides: Partial<Transaction> = {}): Transaction => ({
  date,
  time: '10:00:00',
  portfolioId: 'PORT9001',
  sequenceNo,
  investmentId: 'INV0000001',
  type: 'BU',
  quantity: 1,
  price: 1,
  amount: 1,
  currency: 'USD',
  status: 'D',
  ...overrides,
})

describe('buildPortfolioDetail', () => {
  it('returns null for an unknown portfolio (NOTFND)', () => {
    expect(buildPortfolioDetail('NOPE0001', [portfolio()], [], [])).toBeNull()
  })

  it('matches PORT-ID case-insensitively, ignoring blank padding', () => {
    expect(buildPortfolioDetail(' port9001 ', [portfolio()], [], [])?.portfolio.portfolioId).toBe('PORT9001')
  })

  it('keeps only this portfolio’s positions, largest market value first', () => {
    const detail = buildPortfolioDetail(
      'PORT9001',
      [portfolio()],
      [position('SMALL00001', 10), position('OTHER00001', 999, { portfolioId: 'PORT9002' }), position('BIG0000001', 90)],
      [],
    )!
    expect(detail.positions.map((p) => p.investmentId)).toEqual(['BIG0000001', 'SMALL00001'])
    expect(detail.positionsMarketValueCents).toBe(10000n)
  })

  it('computes invested value in exact cents', () => {
    expect(buildPortfolioDetail('PORT9001', [portfolio()], [], [])!.investedValueCents).toBe(99990n)
  })

  it('returns the latest transactions for this portfolio, newest first', () => {
    const detail = buildPortfolioDetail(
      'PORT9001',
      [portfolio()],
      [],
      [
        txn('000001', '2026-10-01'),
        txn('000003', '2026-10-03'),
        txn('000099', '2026-10-09', { portfolioId: 'PORT9002' }),
        txn('000002', '2026-10-02'),
      ],
      2,
    )!
    expect(detail.recentTransactions.map((t) => t.sequenceNo)).toEqual(['000003', '000002'])
    expect(detail.transactionCount).toBe(3)
  })
})

describe('mock data consistency', () => {
  it.each(mockPortfolios.map((p) => [p.portfolioId, p] as const))(
    '%s: positions market value + cash = PORT-TOTAL-VALUE',
    (id, p) => {
      const held = mockPositions.filter((pos) => pos.portfolioId === id)
      expect(sumCents(held.map((pos) => pos.marketValue)) + sumCents([p.cashBalance])).toBe(sumCents([p.totalValue]))
    },
  )

  it.each(mockPortfolios.map((p) => [p.portfolioId, p] as const))(
    '%s: PORT-LAST-TRANS is not earlier than its latest transaction',
    (id, p) => {
      const latest = mockTransactions.filter((t) => t.portfolioId === id).reduce((max, t) => (t.date > max ? t.date : max), '')
      expect(p.lastTrans >= latest).toBe(true)
    },
  )
})
