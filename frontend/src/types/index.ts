export interface SummaryMetrics {
  totalAccounts: number;
  totalPositions: number;
  todayTransactions: number;
  systemStatus: 'operational' | 'degraded' | 'down';
}

export interface RecentTransaction {
  date: string;
  account: string;
  type: 'BUY' | 'SELL' | 'XFER' | 'FEE';
  fund: string;
  fundName: string;
  amount: number;
}

export interface SystemStatus {
  status: 'operational' | 'degraded' | 'down';
  lastBatchRun: string;
  nextBatchRun: string;
}
