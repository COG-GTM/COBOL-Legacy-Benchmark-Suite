import type { Position, Transaction } from '../types';

/**
 * In-memory stand-in for the legacy VSAM POSFILE (positions) and the DB2
 * POSHIST table (transaction history). Keyed by the 10-char account number.
 *
 * Swap these maps for a real backend behind the mock service layer
 * (src/services/inquiryService.ts) without touching the UI.
 */

/** Special account that forces a simulated data-access failure (INQPORT P999). */
export const ERROR_ACCOUNT = '0000000000';

export const MOCK_POSITIONS: Record<string, Position> = {
  '0000001001': {
    accountNo: '0000001001',
    fundId: 'GRWTH1',
    fundName: 'Global Growth Equity Fund',
    units: 1250.5,
    costBasis: 118_750.0,
    marketValue: 142_320.75,
    currency: 'USD',
    status: 'A',
  },
  '0000001002': {
    accountNo: '0000001002',
    fundId: 'BOND02',
    fundName: 'Core Aggregate Bond Fund',
    units: 8400.0,
    costBasis: 84_000.0,
    marketValue: 81_900.5,
    currency: 'USD',
    status: 'A',
  },
  '0000002001': {
    accountNo: '0000002001',
    fundId: 'INTL03',
    fundName: 'International Value Fund',
    units: 320.25,
    costBasis: 32_500.0,
    marketValue: 41_188.4,
    currency: 'USD',
    status: 'A',
  },
  '0000003001': {
    accountNo: '0000003001',
    fundId: 'MMKT04',
    fundName: 'Government Money Market Fund',
    units: 50_000.0,
    costBasis: 50_000.0,
    marketValue: 50_000.0,
    currency: 'USD',
    status: 'C',
  },
};

/**
 * Transaction history keyed by account. Stored in arbitrary order — the
 * service layer sorts by date descending, mirroring the legacy
 * "ORDER BY TRANS_DATE DESC" cursor in INQHIST.
 */
export const MOCK_HISTORY: Record<string, Transaction[]> = {
  // 23 rows -> exercises multi-page paging (10 per page = 3 pages).
  '0000001001': [
    { date: '2024-01-15', type: 'BUY', units: 100.0, price: 92.5, amount: 9250.0 },
    { date: '2024-02-03', type: 'BUY', units: 150.0, price: 94.1, amount: 14_115.0 },
    { date: '2024-02-20', type: 'DIV', units: 0.0, price: 0.0, amount: 312.4 },
    { date: '2024-03-11', type: 'BUY', units: 75.5, price: 95.8, amount: 7232.9 },
    { date: '2024-04-01', type: 'SELL', units: 50.0, price: 97.2, amount: 4860.0 },
    { date: '2024-04-18', type: 'BUY', units: 200.0, price: 96.4, amount: 19_280.0 },
    { date: '2024-05-02', type: 'DIV', units: 0.0, price: 0.0, amount: 298.15 },
    { date: '2024-05-22', type: 'BUY', units: 120.0, price: 98.7, amount: 11_844.0 },
    { date: '2024-06-10', type: 'SELL', units: 30.0, price: 101.3, amount: 3039.0 },
    { date: '2024-06-28', type: 'BUY', units: 90.0, price: 102.6, amount: 9234.0 },
    { date: '2024-07-15', type: 'DIV', units: 0.0, price: 0.0, amount: 341.8 },
    { date: '2024-08-05', type: 'BUY', units: 60.0, price: 104.0, amount: 6240.0 },
    { date: '2024-08-29', type: 'SELL', units: 45.0, price: 103.1, amount: 4639.5 },
    { date: '2024-09-12', type: 'BUY', units: 110.0, price: 105.5, amount: 11_605.0 },
    { date: '2024-10-01', type: 'DIV', units: 0.0, price: 0.0, amount: 360.2 },
    { date: '2024-10-23', type: 'BUY', units: 85.0, price: 106.9, amount: 9086.5 },
    { date: '2024-11-07', type: 'SELL', units: 70.0, price: 108.4, amount: 7588.0 },
    { date: '2024-11-26', type: 'BUY', units: 95.0, price: 107.2, amount: 10_184.0 },
    { date: '2024-12-09', type: 'DIV', units: 0.0, price: 0.0, amount: 388.6 },
    { date: '2024-12-30', type: 'BUY', units: 130.0, price: 109.8, amount: 14_274.0 },
    { date: '2025-01-14', type: 'SELL', units: 55.0, price: 111.0, amount: 6105.0 },
    { date: '2025-02-04', type: 'BUY', units: 140.0, price: 110.3, amount: 15_442.0 },
    { date: '2025-02-25', type: 'DIV', units: 0.0, price: 0.0, amount: 402.9 },
  ],
  // 4 rows -> single page.
  '0000001002': [
    { date: '2024-03-01', type: 'BUY', units: 5000.0, price: 10.0, amount: 50_000.0 },
    { date: '2024-06-01', type: 'BUY', units: 3400.0, price: 10.0, amount: 34_000.0 },
    { date: '2024-09-01', type: 'DIV', units: 0.0, price: 0.0, amount: 1240.0 },
    { date: '2024-12-01', type: 'DIV', units: 0.0, price: 0.0, amount: 1255.5 },
  ],
  // exactly 10 rows -> single full page, no Next.
  '0000002001': [
    { date: '2024-01-05', type: 'BUY', units: 40.0, price: 100.0, amount: 4000.0 },
    { date: '2024-02-05', type: 'BUY', units: 35.0, price: 101.5, amount: 3552.5 },
    { date: '2024-03-05', type: 'BUY', units: 30.0, price: 103.0, amount: 3090.0 },
    { date: '2024-04-05', type: 'SELL', units: 10.0, price: 104.2, amount: 1042.0 },
    { date: '2024-05-05', type: 'BUY', units: 25.0, price: 105.7, amount: 2642.5 },
    { date: '2024-06-05', type: 'DIV', units: 0.0, price: 0.0, amount: 88.3 },
    { date: '2024-07-05', type: 'BUY', units: 20.0, price: 107.1, amount: 2142.0 },
    { date: '2024-08-05', type: 'SELL', units: 15.0, price: 108.9, amount: 1633.5 },
    { date: '2024-09-05', type: 'BUY', units: 18.0, price: 110.4, amount: 1987.2 },
    { date: '2024-10-05', type: 'DIV', units: 0.0, price: 0.0, amount: 95.6 },
  ],
  // Position exists (status Closed) but no transaction history -> empty page.
  '0000003001': [],
};
