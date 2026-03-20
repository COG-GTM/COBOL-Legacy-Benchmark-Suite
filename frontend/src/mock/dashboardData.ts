import type { SummaryMetrics, RecentTransaction, SystemStatus } from '@/types';

export const summaryMetrics: SummaryMetrics = {
  totalAccounts: 1247,
  totalPositions: 8432,
  todayTransactions: 156,
  systemStatus: 'operational' as const,
};

export const recentTransactions: RecentTransaction[] = [
  { date: '2024-01-15', account: '0000012345', type: 'BUY' as const, fund: 'GRWTH00001', fundName: 'US Large Cap Growth', amount: 25500.00 },
  { date: '2024-01-15', account: '0000067890', type: 'SELL' as const, fund: 'BOND00003', fundName: 'International Bond Fund', amount: 12750.50 },
  { date: '2024-01-15', account: '0000012345', type: 'FEE' as const, fund: 'GRWTH00001', fundName: 'US Large Cap Growth', amount: 45.00 },
  { date: '2024-01-14', account: '0000054321', type: 'XFER' as const, fund: 'EMKT00002', fundName: 'Emerging Markets Equity', amount: 8200.00 },
  { date: '2024-01-14', account: '0000098765', type: 'BUY' as const, fund: 'REIT00004', fundName: 'Real Estate Investment Trust', amount: 15000.00 },
];

export const systemStatus: SystemStatus = {
  status: 'operational' as const,
  lastBatchRun: '2024-01-15 02:00:00',
  nextBatchRun: '2024-01-16 02:00:00',
};
