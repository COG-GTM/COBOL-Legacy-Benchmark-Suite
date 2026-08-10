import { describe, expect, it } from 'vitest';
import type { Portfolio } from '../types/portfolio';
import type { Position } from '../types/position';
import { MockPositionService } from './mockPositionService';

const portfolios: Portfolio[] = [
  {
    portId: 'PORT0001',
    accountNo: 'ACCT100001',
    clientName: 'Margaret Chen',
    clientType: 'I',
    createDate: '20240115',
    lastMaintDate: '20240320',
    status: 'A',
    totalValue: '1000.00',
    cashBalance: '100.00',
    lastUser: 'JSMITH',
    lastTransId: '00000005',
  },
  {
    portId: 'PORT0009',
    accountNo: 'ACCT100001',
    clientName: 'Margaret Chen',
    clientType: 'I',
    createDate: '20240115',
    lastMaintDate: '20240320',
    status: 'A',
    totalValue: '500.00',
    cashBalance: '0.00',
    lastUser: 'JSMITH',
    lastTransId: '00000006',
  },
  {
    portId: 'PORT0002',
    accountNo: 'ACCT100002',
    clientName: 'Atlas Holdings LLC',
    clientType: 'C',
    createDate: '20230903',
    lastMaintDate: '20240218',
    status: 'A',
    totalValue: '2000.00',
    cashBalance: '0.00',
    lastUser: 'MGARCIA',
    lastTransId: '00000010',
  },
];

const audit = { lastMaintDate: '2024-04-01-09.00.00.000000', lastMaintUser: 'X' };

const positions: Position[] = [
  {
    portfolioId: 'PORT0001',
    date: '20240401',
    investmentId: 'FND0000002',
    fundName: 'B Fund',
    quantity: '10.0000',
    costBasis: '100.00',
    marketValue: '120.00',
    currency: 'USD',
    status: 'A',
    ...audit,
  },
  {
    portfolioId: 'PORT0001',
    date: '20240401',
    investmentId: 'FND0000001',
    fundName: 'A Fund',
    quantity: '5.0000',
    costBasis: '50.00',
    marketValue: '40.00',
    currency: 'USD',
    status: 'C',
    ...audit,
  },
  {
    portfolioId: 'PORT0009',
    date: '20240401',
    investmentId: 'FND0000003',
    fundName: 'C Fund',
    quantity: '3.0000',
    costBasis: '30.00',
    marketValue: '35.00',
    currency: 'USD',
    status: 'P',
    ...audit,
  },
  {
    portfolioId: 'PORT0002',
    date: '20240401',
    investmentId: 'FND0000009',
    fundName: 'Other',
    quantity: '99.0000',
    costBasis: '990.00',
    marketValue: '990.00',
    currency: 'USD',
    status: 'A',
    ...audit,
  },
];

describe('MockPositionService', () => {
  const service = new MockPositionService(positions, portfolios);

  it('resolves an account to all its portfolios and orders results', async () => {
    const result = await service.listByAccount('ACCT100001');
    expect(
      result.map((p) => `${p.portfolioId}:${p.investmentId}`),
    ).toEqual([
      'PORT0001:FND0000001',
      'PORT0001:FND0000002',
      'PORT0009:FND0000003',
    ]);
  });

  it('is case-insensitive and ignores surrounding whitespace', async () => {
    expect(await service.listByAccount('  acct100002 ')).toHaveLength(1);
  });

  it('filters by POS-STATUS', async () => {
    const active = await service.listByAccount('ACCT100001', { status: 'A' });
    expect(active.map((p) => p.investmentId)).toEqual(['FND0000002']);
    const pending = await service.listByAccount('ACCT100001', { status: 'P' });
    expect(pending.map((p) => p.investmentId)).toEqual(['FND0000003']);
  });

  it('returns an empty list for an unknown or blank account', async () => {
    expect(await service.listByAccount('NOPE')).toHaveLength(0);
    expect(await service.listByAccount('   ')).toHaveLength(0);
  });

  it('does not leak internal state via returned copies', async () => {
    const [first] = await service.listByAccount('ACCT100002');
    first.marketValue = '0.00';
    const [again] = await service.listByAccount('ACCT100002');
    expect(again.marketValue).toBe('990.00');
  });
});
