export type ClientType = 'I' | 'C' | 'T';
export type PortfolioStatus = 'A' | 'C' | 'S';
export type PositionStatus = 'A' | 'C' | 'P';
export type TransactionType = 'BU' | 'SL' | 'TR' | 'FE';
export type TransactionStatus = 'P' | 'D' | 'F' | 'R';

export interface Portfolio {
  portfolioId: string;
  accountNumber: string;
  clientName: string;
  clientType: ClientType;
  createDate: string;
  lastMaintenance: string;
  status: PortfolioStatus;
  totalValue: number;
  cashBalance: number;
  lastUser: string;
  lastTransaction: string;
}

export interface Position {
  portfolioId: string;
  date: string;
  investmentId: string;
  investmentName: string;
  quantity: number;
  costBasis: number;
  marketValue: number;
  currency: string;
  status: PositionStatus;
  lastMaintDate: string;
  lastMaintUser: string;
}

export interface Transaction {
  date: string;
  time: string;
  portfolioId: string;
  sequenceNo: string;
  investmentId: string;
  investmentName: string;
  type: TransactionType;
  quantity: number;
  price: number;
  amount: number;
  currency: string;
  status: TransactionStatus;
  processDate: string;
  processUser: string;
}

export interface PortfolioSummary {
  portfolio: Portfolio;
  positions: Position[];
  totalMarketValue: number;
  totalCostBasis: number;
  totalGainLoss: number;
  totalGainLossPercent: number;
}

export interface PaginatedResult<T> {
  data: T[];
  totalItems: number;
  currentPage: number;
  totalPages: number;
  pageSize: number;
}

export const CLIENT_TYPE_LABELS: Record<ClientType, string> = {
  I: 'Individual',
  C: 'Corporate',
  T: 'Trust',
};

export const PORTFOLIO_STATUS_LABELS: Record<PortfolioStatus, string> = {
  A: 'Active',
  C: 'Closed',
  S: 'Suspended',
};

export const POSITION_STATUS_LABELS: Record<PositionStatus, string> = {
  A: 'Active',
  C: 'Closed',
  P: 'Pending',
};

export const TRANSACTION_TYPE_LABELS: Record<TransactionType, string> = {
  BU: 'Buy',
  SL: 'Sell',
  TR: 'Transfer',
  FE: 'Fee',
};

export const TRANSACTION_STATUS_LABELS: Record<TransactionStatus, string> = {
  P: 'Pending',
  D: 'Completed',
  F: 'Failed',
  R: 'Reversed',
};
