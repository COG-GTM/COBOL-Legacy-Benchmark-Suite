export interface Portfolio {
  portfolioId: string;
  accountNumber: string;
  clientName: string;
  clientType: 'Individual' | 'Corporate' | 'Trust';
  createDate: string;
  lastMaintDate: string;
  status: 'Active' | 'Closed' | 'Suspended';
  totalValue: number;
  cashBalance: number;
  riskLevel: 'Low' | 'Medium' | 'High';
  currency: string;
}

export interface Position {
  portfolioId: string;
  investmentId: string;
  symbol: string;
  name: string;
  quantity: number;
  costBasis: number;
  marketValue: number;
  currentPrice: number;
  previousPrice: number;
  currency: string;
  status: 'Active' | 'Closed' | 'Pending';
  lastUpdated: string;
}

export interface Transaction {
  transactionId: string;
  date: string;
  time: string;
  portfolioId: string;
  sequenceNo: string;
  investmentId: string;
  symbol: string;
  type: 'BUY' | 'SELL' | 'TRANSFER' | 'FEE';
  quantity: number;
  price: number;
  amount: number;
  fees: number;
  currency: string;
  status: 'Pending' | 'Done' | 'Failed' | 'Reversed';
  processUser: string;
}

export interface MarketTick {
  symbol: string;
  price: number;
  change: number;
  changePercent: number;
}

export interface PerformancePoint {
  date: string;
  value: number;
  benchmark: number;
}

export interface BatchJob {
  jobId: string;
  programName: string;
  description: string;
  status: 'Running' | 'Completed' | 'Failed' | 'Scheduled';
  startTime: string;
  endTime?: string;
  recordsProcessed: number;
  totalRecords: number;
  returnCode: number;
}

export type TransactionTypeFilter = 'ALL' | 'BUY' | 'SELL' | 'TRANSFER' | 'FEE';
export type StatusFilter = 'ALL' | 'Pending' | 'Done' | 'Failed' | 'Reversed';
