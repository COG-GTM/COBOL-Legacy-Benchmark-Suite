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

describe('currentHoldings', () => {
  it('keeps only the latest dated snapshot per investment', () => {
    const detail = buildPortfolioDetail(
      'PORT9001',
      [portfolio()],
      [
        position('ABC0000001', 100, { date: '2026-10-13' }),
        position('ABC0000001', 110, { date: '2026-10-14' }),
        position('XYZ0000001', 50),
      ],
      [],
    )!
    expect(detail.positions.map((p) => [p.investmentId, p.marketValue])).toEqual([
      ['ABC0000001', 110],
      ['XYZ0000001', 50],
    ])
    expect(detail.positionsMarketValueCents).toBe(16000n)
  })

  it('drops investments whose latest snapshot is closed', () => {
    const detail = buildPortfolioDetail(
      'PORT9001',
      [portfolio()],
      [
        position('ABC0000001', 100, { date: '2026-10-13' }),
        position('ABC0000001', 0, { date: '2026-10-14', status: 'C', quantity: 0 }),
        position('PND0000001', 25, { status: 'P' }),
      ],
      [],
    )!
    expect(detail.positions.map((p) => p.investmentId)).toEqual(['PND0000001'])
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
    '%s: PORT-LAST-TRANS is the latest posted (TRN-STATUS D) transaction',
    (id, p) => {
      const posted = mockTransactions.filter((t) => t.portfolioId === id && t.status === 'D')
      const latest = posted.reduce((max, t) => (t.date > max ? t.date : max), '')
      // Portfolios with no posted rows in the fixture last traded before the fixture window.
      if (posted.length === 0) expect(p.lastTrans < mockTransactions.map((t) => t.date).sort()[0]).toBe(true)
      else expect(p.lastTrans).toBe(latest)
    },
  )
})
