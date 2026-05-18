export interface PositionRecord {
  accountNo: string;
  fundId: string;
  cusip: string;
  shareBalance: number;
  avgCost: number;
  costBasis: number;
  lastDate: string;
  lastTrans: string;
  status: 'A' | 'C';
}

export interface HistoryRecord {
  timestamp: string;
  accountNo: string;
  fundId: string;
  transId: string;
  transType: 'BY' | 'SL' | 'FE';
  shareQty: number;
  price: number;
  amount: number;
  resultCode: string;
  beforeBal: number;
  afterBal: number;
}

export interface BatchControlRecord {
  processDate: string;
  processId: string;
  status: 'W' | 'P' | 'C' | 'E';
  startTime: string;
  endTime: string;
  recordCount: number;
  errorCount: number;
  lastPos: number;
  returnCode: string;
  message: string;
}

export interface Portfolio {
  portfolioId: string;
  name: string;
  status: 'A' | 'I' | 'C';
  createDate: string;
  totalValue: number;
}

export interface ErrorCode {
  code: string;
  description: string;
  severity: 'Error' | 'Warning';
  action: string;
}

export interface User {
  username: string;
  role: 'Read' | 'Update' | 'Admin';
}

export interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
}

export type TransTypeLabel = {
  [key in HistoryRecord['transType']]: string;
};

export const TRANS_TYPE_LABELS: TransTypeLabel = {
  BY: 'Buy',
  SL: 'Sell',
  FE: 'Fee',
};

export type PositionStatusLabel = {
  [key in PositionRecord['status']]: string;
};

export const POSITION_STATUS_LABELS: PositionStatusLabel = {
  A: 'Active',
  C: 'Closed',
};

export type PortfolioStatusLabel = {
  [key in Portfolio['status']]: string;
};

export const PORTFOLIO_STATUS_LABELS: PortfolioStatusLabel = {
  A: 'Active',
  I: 'Inactive',
  C: 'Closed',
};

export type BatchStatusLabel = {
  [key in BatchControlRecord['status']]: string;
};

export const BATCH_STATUS_LABELS: BatchStatusLabel = {
  W: 'Waiting',
  P: 'In-Process',
  C: 'Complete',
  E: 'Error',
};

export const ERROR_CODES: ErrorCode[] = [
  { code: 'E001', description: 'Invalid Account Number', severity: 'Error', action: 'Reject' },
  { code: 'E002', description: 'Invalid Fund ID', severity: 'Error', action: 'Reject' },
  { code: 'E003', description: 'Invalid Transaction Type', severity: 'Error', action: 'Reject' },
  { code: 'E004', description: 'Insufficient Position Balance', severity: 'Error', action: 'Reject' },
  { code: 'W001', description: 'Zero Dollar Transaction', severity: 'Warning', action: 'Process' },
  { code: 'W002', description: 'Duplicate Transaction ID', severity: 'Warning', action: 'Log' },
];
