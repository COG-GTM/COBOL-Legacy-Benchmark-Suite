export type ClientType = "I" | "C" | "T";
export type PortfolioStatus = "A" | "C" | "S";
export type TransactionType = "BUY" | "SELL" | "TRANSFER" | "FEE";
export type InvestmentType = "STK" | "BND" | "MMF" | "ETF";
export type AuditAction = "CREATE" | "UPDATE" | "DELETE" | "INQUIRE";
export type AuditStatus = "SUCC" | "FAIL" | "WARN";
export type BatchStatus = "PENDING" | "RUNNING" | "COMPLETED" | "FAILED";

export const CLIENT_TYPE_LABELS: Record<ClientType, string> = {
  I: "Individual",
  C: "Corporate",
  T: "Trust",
};

export const PORTFOLIO_STATUS_LABELS: Record<PortfolioStatus, string> = {
  A: "Active",
  C: "Closed",
  S: "Suspended",
};

export const TRANSACTION_TYPE_LABELS: Record<TransactionType, string> = {
  BUY: "Buy",
  SELL: "Sell",
  TRANSFER: "Transfer",
  FEE: "Fee",
};

export const INVESTMENT_TYPE_LABELS: Record<InvestmentType, string> = {
  STK: "Stock",
  BND: "Bond",
  MMF: "Money Market Fund",
  ETF: "Exchange Traded Fund",
};

export interface Portfolio {
  id: string;
  accountNo: string;
  clientName: string;
  clientType: ClientType;
  status: PortfolioStatus;
  totalValue: number;
  cashBalance: number;
  lastUser: string;
  lastTrans: string;
  createdAt: string;
  updatedAt: string;
}

export interface Position {
  id: string;
  fundId: string;
  fundName: string;
  units: number;
  costBasis: number;
  marketValue: number;
  portfolioId: string;
}

export interface Transaction {
  id: string;
  transactionType: TransactionType;
  investmentType: InvestmentType;
  units: number;
  price: number;
  amount: number;
  sequenceNo: string;
  portfolioId: string;
  createdAt: string;
}

export interface AuditLog {
  id: string;
  action: AuditAction;
  key: string;
  reason: string;
  status: AuditStatus;
  portfolioId: string | null;
  createdAt: string;
}

export interface BatchRun {
  id: string;
  status: BatchStatus;
  totalItems: number;
  processed: number;
  errors: number;
  startedAt: string;
  completedAt: string | null;
}

export interface PortfolioListResponse {
  portfolios: Portfolio[];
  total: number;
}

export interface PortfolioDetail extends Portfolio {
  positions: Position[];
}

export interface TransactionListResponse {
  transactions: (Transaction & { portfolio?: { accountNo: string; clientName: string } })[];
  total: number;
}

export interface TransactionFilters {
  portfolioId?: string;
  transactionType?: TransactionType;
  startDate?: string;
  endDate?: string;
}

export interface CreatePortfolioInput {
  accountNo: string;
  clientName: string;
  clientType: ClientType;
  cashBalance?: number;
}

export interface SubmitTransactionInput {
  portfolioId: string;
  transactionType: TransactionType;
  investmentType: InvestmentType;
  units: number;
  price: number;
}

export interface ReportStats {
  totalPortfolios: number;
  totalAUM: number;
  totalTransactions: number;
  avgPortfolioValue: number;
  transactionsByType: { type: string; count: number; totalAmount: number }[];
  portfolioValueTrend: { month: string; value: number }[];
  transactionVolumeTrend: { month: string; count: number }[];
  topHoldings: { fundId: string; fundName: string; totalUnits: number; totalMarketValue: number }[];
}

export interface PositionReport {
  portfolios: (Portfolio & { positions: Position[] })[];
}

export interface AuditReport {
  logs: AuditLog[];
  total: number;
}
