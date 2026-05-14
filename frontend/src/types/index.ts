export interface User {
  id: string;
  username: string;
  password: string;
  name: string;
  role: 'read-only' | 'read-write';
}

export interface Portfolio {
  id: string;
  accountNo: string;
  clientName: string;
  clientType: 'I' | 'C' | 'T';
  createDate: string;
  lastMaintDate: string;
  status: 'A' | 'I' | 'C';
  totalValue: number;
  cashBalance: number;
  lastUser: string;
  lastTrans: string;
}

export interface Position {
  portfolioId: string;
  date: string;
  investmentId: string;
  fundName: string;
  quantity: number;
  costBasis: number;
  marketValue: number;
  currency: string;
  status: 'A' | 'C' | 'P';
}

export type TransactionType = 'BU' | 'SL' | 'TR' | 'FE';
export type TransactionStatus = 'P' | 'D' | 'F' | 'R';

export interface Transaction {
  date: string;
  time: string;
  portfolioId: string;
  sequenceNo: string;
  investmentId: string;
  type: TransactionType;
  quantity: number;
  price: number;
  amount: number;
  currency: string;
  status: TransactionStatus;
  processDate: string;
  processUser: string;
}

export interface AuditEntry {
  timestamp: string;
  systemId: string;
  userId: string;
  program: string;
  terminal: string;
  type: 'TRAN' | 'USER' | 'SYST';
  action: string;
  status: 'SUCC' | 'FAIL' | 'WARN';
  portfolioId: string;
  accountNo: string;
  message: string;
}

export interface PositionReportEntry {
  date: string;
  portfolioId: string;
  investmentId: string;
  fundName: string;
  quantity: number;
  costBasis: number;
  marketValue: number;
  gainLoss: number;
}

export interface StatisticsEntry {
  date: string;
  metric: string;
  value: number;
  unit: string;
  trend: 'up' | 'down' | 'stable';
}

export interface AuthState {
  user: Omit<User, 'password'> | null;
  isAuthenticated: boolean;
}
