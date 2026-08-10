import { describe, expect, it } from 'vitest';
import { MockHistoryService } from './mockHistoryService';
import type { HistoryRecord } from '../types/history';
import type { Portfolio } from '../types/portfolio';

const PORTFOLIOS: Portfolio[] = [
  {
    portId: 'PORT0001',
    accountNo: 'ACCT100001',
    clientName: 'Margaret Chen',
    clientType: 'I',
    createDate: '20240115',
    lastMaintDate: '20240320',
    status: 'A',
    totalValue: '100.00',
    cashBalance: '0.00',
    lastUser: 'JSMITH',
    lastTransId: '00010023',
  },
  {
    portId: 'PORT0002',
    accountNo: 'ACCT100002',
    clientName: 'Atlas Holdings LLC',
    clientType: 'C',
    createDate: '20230903',
    lastMaintDate: '20240218',
    status: 'A',
    totalValue: '200.00',
    cashBalance: '0.00',
    lastUser: 'JSMITH',
    lastTransId: '00010044',
  },
];

function record(overrides: Partial<HistoryRecord>): HistoryRecord {
  return {
    portfolioId: 'PORT0001',
    date: '20240301',
    time: '101500',
    seqNo: '0001',
    recordType: 'TR',
    actionCode: 'A',
    reasonCode: 'TRDE',
    beforeImage: '',
    afterImage: 'PORT0001|FND0000001',
    processDate: '2024-03-01-10.15.00.000000',
    processUser: 'JSMITH',
    investmentId: 'FND0000001',
    transactionType: 'BU',
    units: '10.0000',
    price: '5.0000',
    amount: '50.00',
    currency: 'USD',
    ...overrides,
  };
}

const HISTORY: HistoryRecord[] = [
  record({ date: '20240301', time: '101500', seqNo: '0001' }),
  record({ date: '20240301', time: '101500', seqNo: '0002' }),
  record({ date: '20240315', time: '090000', seqNo: '0003', recordType: 'PS' }),
  record({ date: '20240420', time: '090000', seqNo: '0004', recordType: 'PT' }),
  record({ portfolioId: 'PORT0002', date: '20240310', seqNo: '0005' }),
];

function service() {
  return new MockHistoryService(HISTORY, PORTFOLIOS);
}

describe('MockHistoryService', () => {
  it('resolves the account to its portfolio and orders rows newest first', async () => {
    const results = await service().listByAccount('ACCT100001');
    expect(results.map((r) => r.seqNo)).toEqual([
      '0004',
      '0003',
      '0002',
      '0001',
    ]);
  });

  it('matches the account number case-insensitively and trims it', async () => {
    const results = await service().listByAccount('  acct100002 ');
    expect(results).toHaveLength(1);
    expect(results[0].portfolioId).toBe('PORT0002');
  });

  it('returns nothing for a blank or unknown account', async () => {
    expect(await service().listByAccount('   ')).toEqual([]);
    expect(await service().listByAccount('ACCT999999')).toEqual([]);
  });

  it('applies an inclusive date range', async () => {
    const results = await service().listByAccount('ACCT100001', {
      startDate: '20240301',
      endDate: '20240315',
    });
    expect(results.map((r) => r.date)).toEqual([
      '20240315',
      '20240301',
      '20240301',
    ]);
  });

  it('filters by record type', async () => {
    const results = await service().listByAccount('ACCT100001', {
      recordType: 'PS',
    });
    expect(results.map((r) => r.seqNo)).toEqual(['0003']);
  });

  it('looks a record up by its HIST-KEY', async () => {
    const found = await service().get('PORT0001202403011015000002');
    expect(found?.seqNo).toBe('0002');
    expect(await service().get('PORT0001999999999999990001')).toBeUndefined();
  });

  it('returns copies so callers cannot mutate the fixture', async () => {
    const [first] = await service().listByAccount('ACCT100002');
    first.reasonCode = 'XXXX';
    const [again] = await service().listByAccount('ACCT100002');
    expect(again.reasonCode).toBe('TRDE');
  });
});
