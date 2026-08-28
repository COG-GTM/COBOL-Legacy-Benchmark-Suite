import type { HistoryEntry, Portfolio, Position, Transaction } from './types'

const fundCatalog = [
  ['AAPL', 'Apple Inc.', 'STK'],
  ['VTI', 'Vanguard Total Market ETF', 'ETF'],
  ['BND', 'Vanguard Total Bond Market', 'BND'],
  ['SPAXX', 'Fidelity Government Money Market', 'MMF'],
] as const

export const portfolios: Portfolio[] = Array.from({ length: 10 }, (_, index) => {
  const number = index + 1
  return {
    portfolioId: `PORT${String(number).padStart(4, '0')}`,
    accountNo: `700100${String(number).padStart(4, '0')}`,
    clientName: ['Avery Morgan', 'Blue Ridge Holdings', 'Cedar Trust', 'Drew Patel', 'Evergreen Partners'][index % 5],
    clientType: (index % 3 === 1 ? 'C' : index % 3 === 2 ? 'T' : 'I') as Portfolio['clientType'],
    createDate: `20240${(number % 9) + 1}15`,
    lastMaint: `20260${(number % 9) + 1}0${(number % 8) + 1}`,
    status: (index === 8 ? 'S' : index === 9 ? 'C' : 'A') as Portfolio['status'],
    totalValue: 124500 + number * 18750,
    cashBalance: 12300 + number * 810,
    lastUser: `USR${String((number % 8) + 1).padStart(5, '0')}`,
    lastTransDate: `20260${(number % 9) + 1}1${(number % 8) + 1}`,
  }
})

export const positions: Position[] = portfolios.flatMap((portfolio, portfolioIndex) =>
  fundCatalog.slice(0, 3 + (portfolioIndex % 2)).map(([investmentId, fundName], fundIndex) => ({
    portfolioId: portfolio.portfolioId,
    date: '20260214',
    investmentId,
    quantity: 40 + portfolioIndex * 11 + fundIndex * 23,
    costBasis: 9400 + portfolioIndex * 1250 + fundIndex * 3200,
    marketValue: 11100 + portfolioIndex * 1480 + fundIndex * 3650,
    currency: 'USD',
    status: 'A',
    fundName,
  })),
)

export const transactions: Transaction[] = Array.from({ length: 60 }, (_, index) => {
  const portfolio = portfolios[index % portfolios.length]
  const fund = fundCatalog[index % fundCatalog.length]
  const type = (index % 4 === 0 ? 'SL' : index % 3 === 0 ? 'TR' : 'BU') as Transaction['type']
  const quantity = 10 + (index % 12) * 5
  const price = 88 + (index % 20) * 4.25
  return {
    date: `2026${String((index % 12) + 1).padStart(2, '0')}${String((index % 27) + 1).padStart(2, '0')}`,
    time: `${String(9 + (index % 8)).padStart(2, '0')}${String((index * 7) % 60).padStart(2, '0')}00`,
    portfolioId: portfolio.portfolioId,
    sequenceNo: String(index + 1).padStart(6, '0'),
    investmentId: fund[0],
    type,
    quantity,
    price,
    amount: quantity * price,
    currency: 'USD',
    status: (index % 13 === 0 ? 'P' : 'D') as Transaction['status'],
  }
})

export const history: HistoryEntry[] = portfolios.slice(0, 8).map((portfolio, index) => ({
  portfolioId: portfolio.portfolioId,
  date: `202602${String(index + 5).padStart(2, '0')}`,
  time: `14${String(index).padStart(2, '0')}00`,
  sequenceNo: String(index + 1).padStart(4, '0'),
  recordType: index % 3 === 0 ? 'PT' : index % 3 === 1 ? 'PS' : 'TR',
  actionCode: index % 3 === 0 ? 'A' : 'C',
  reasonCode: index % 2 ? 'MA01' : 'OP01',
  processDate: '2026-02-14T14:00:00Z',
  processUser: 'BATCH001',
  beforeImage: index % 3 === 0 ? undefined : '{"status":"P"}',
  afterImage: '{"status":"A"}',
}))

export const systemJobs = [
  { name: 'Daily Position Update', schedule: '02:00 UTC', status: 'Healthy', lastRun: '2026-02-14 02:04', duration: '04m 12s' },
  { name: 'Transaction Reconciliation', schedule: '03:30 UTC', status: 'Healthy', lastRun: '2026-02-14 03:32', duration: '01m 48s' },
  { name: 'Audit Archive', schedule: '04:00 UTC', status: 'Warning', lastRun: '2026-02-14 04:17', duration: '17m 03s' },
  { name: 'Market Data Import', schedule: 'Every 15 min', status: 'Healthy', lastRun: '2026-02-14 14:45', duration: '00m 32s' },
]
