import { describe, expect, it } from 'vitest';
import { getDashboardMetrics } from './dashboardService';
import type { Portfolio } from '../types/portfolio';
import type { Transaction } from '../types/transaction';

const portfolio = (
  portId: string,
  status: Portfolio['status'],
  totalValue: string,
): Portfolio => ({
  portId,
  accountNo: `ACCT-${portId}`,
  clientName: portId,
  clientType: 'I',
  createDate: '20240101',
  lastMaintDate: '20240101',
  status,
  totalValue,
  cashBalance: '0.00',
  lastUser: 'TEST',
  lastTransId: '00000001',
});

const txn = (date: string, time: string, seq: string): Transaction => ({
  date,
  time,
  portfolioId: 'PORT0001',
  sequenceNo: seq,
  investmentId: 'EQ-TEST',
  type: 'BU',
  amount: '100.00',
  currency: 'USD',
  status: 'D',
});

describe('getDashboardMetrics', () => {
  it('sums total AUM with full decimal precision', () => {
    const portfolios = [
      portfolio('P1', 'A', '1284530.75'),
      portfolio('P2', 'A', '8845200.00'),
      portfolio('P3', 'S', '2390115.55'),
    ];
    const metrics = getDashboardMetrics(portfolios, []);
    expect(metrics.totalAum).toBe('12519846.30');
  });

  it('counts only active portfolios but reports the total', () => {
    const portfolios = [
      portfolio('P1', 'A', '10.00'),
      portfolio('P2', 'C', '20.00'),
      portfolio('P3', 'A', '30.00'),
    ];
    const metrics = getDashboardMetrics(portfolios, []);
    expect(metrics.activePortfolios).toBe(2);
    expect(metrics.totalPortfolios).toBe(3);
  });

  it('returns the most recent transactions first, limited', () => {
    const transactions = [
      txn('20240101', '090000', '001'),
      txn('20240103', '120000', '003'),
      txn('20240102', '080000', '002'),
    ];
    const metrics = getDashboardMetrics([], transactions, 2);
    expect(metrics.recentTransactions.map((t) => t.sequenceNo)).toEqual([
      '003',
      '002',
    ]);
  });
});
