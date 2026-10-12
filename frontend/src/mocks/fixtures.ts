import type { Position, Transaction } from '../api/types';
import positionsJson from './data/positions.json';
import transactionsJson from './data/transactions.json';

/**
 * Seed data from section 7.4 of the FR doc (verbatim), plus clearly marked synthetic rows:
 * - VFIAX00001 on 1000000001 (the doc asks for a second position to exercise the multi-row table).
 * - Account 1000000005 with 12 positions, to exercise position paging (FR-014, > 10 rows).
 */
export const positionsByAccount: Record<string, Position[]> = {
  ...(positionsJson as Record<string, Position[]>),
  '1000000005': Array.from({ length: 12 }, (_, i): Position => {
    const n = String(i + 1).padStart(2, '0');
    return {
      accountNo: '1000000005',
      portfolioId: 'PORT00005',
      positionDate: '2024-03-20',
      investmentId: `SYN00000${n}`,
      fundName: null,
      units: `${(i + 1) * 10}.5000`,
      costBasis: `${(i + 1) * 1000}.00`,
      marketValue: `${(i + 1) * 1000 + 125}.50`,
      currency: 'USD',
      status: 'A',
      lastMaintainedAt: '2024-03-20T15:33:00.000000Z',
      lastMaintainedBy: 'BATCH01',
    };
  }),
};

export const transactionsByAccount: Record<string, Transaction[]> = transactionsJson as Record<string, Transaction[]>;

/** Trigger accounts from the section 7.4 mock behaviour table (plus two added for FR-002b / FR-029). */
export const TRIGGER_ACCOUNTS = {
  notFound: '1000000004',
  manyPositions: '1000000005',
  accessDenied: '9999999995',
  authCheckFailed: '9999999996',
  dbUnavailable: '9999999997',
  readError: '9999999998',
  systemError: '9999999999',
} as const;
