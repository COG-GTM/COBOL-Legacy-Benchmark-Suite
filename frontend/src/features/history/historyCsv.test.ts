import { describe, expect, it } from 'vitest';
import { historyCsvFilename, historyToCsv } from './historyCsv';
import type { HistoryRecord } from '../../types/history';

const TRANSACTION: HistoryRecord = {
  portfolioId: 'PORT0001',
  date: '20240301',
  time: '101500',
  seqNo: '0007',
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
  price: '5.2500',
  amount: '52.50',
  currency: 'USD',
};

const PORTFOLIO_CHANGE: HistoryRecord = {
  ...TRANSACTION,
  seqNo: '0008',
  recordType: 'PT',
  actionCode: 'C',
  reasonCode: 'CASH',
  investmentId: null,
  transactionType: null,
  units: null,
  price: null,
  amount: null,
  currency: null,
};

describe('historyToCsv', () => {
  it('exports raw decimal strings and labelled codes', () => {
    const [header, transaction, portfolioChange] = historyToCsv([
      TRANSACTION,
      PORTFOLIO_CHANGE,
    ]).split('\r\n');

    expect(header).toBe(
      'Date,Time,Portfolio,Sequence,Record Type,Action,Transaction Type,Fund ID,Units,Price,Amount,Currency,Reason Code,Process User',
    );
    expect(transaction).toBe(
      '2024-03-01,10:15:00,PORT0001,0007,Transaction,Add,Buy,FND0000001,10.0000,5.2500,52.50,USD,TRDE,JSMITH',
    );
    expect(portfolioChange).toBe(
      '2024-03-01,10:15:00,PORT0001,0008,Portfolio,Change,,,,,,,CASH,JSMITH',
    );
  });
});

describe('historyCsvFilename', () => {
  it('includes only the bounds that were set', () => {
    expect(historyCsvFilename('ACCT100001', '20240101', '20240430')).toBe(
      'history-ACCT100001-20240101-20240430.csv',
    );
    expect(historyCsvFilename('ACCT100001', '', '')).toBe(
      'history-ACCT100001.csv',
    );
  });
});
