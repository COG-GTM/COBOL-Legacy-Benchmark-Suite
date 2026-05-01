import { Portfolio, PortfolioStatus, RiskLevel } from './portfolio';
import { Transaction } from './transaction';
import { Position } from './position';
import { BatchControl } from './batch';

export interface CreatePortfolioRequest {
  portfolioId: string;
  accountType: string;
  branchId: string;
  clientId: string;
  name: string;
  currencyCode: string;
  riskLevel: RiskLevel;
}

export interface UpdatePortfolioRequest {
  name?: string;
  currencyCode?: string;
  riskLevel?: RiskLevel;
  branchId?: string;
  accountType?: string;
}

export interface PortfolioResponse {
  portfolio: Portfolio;
  positions?: Position[];
}

export interface PortfolioListResponse {
  portfolios: Portfolio[];
  total: number;
  page: number;
  limit: number;
}

export interface TransactionListResponse {
  transactions: Transaction[];
  total: number;
  page: number;
  limit: number;
}

export interface PortfolioInquiryResponse {
  portfolio: Portfolio;
  positions: Position[];
  totalCostBasis: number;
  totalMarketValue: number;
  totalGainLoss: number;
}

export interface BatchJobStatus {
  runId: string;
  status: BatchControl['status'];
  currentStep: string;
  progress: number;
  recordCount: number;
  errorCount: number;
  startTime: string;
  endTime?: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  user: {
    id: string;
    username: string;
  };
}

export interface ApiError {
  code: string;
  message: string;
  details?: Record<string, string[]>;
}

export interface PaginationQuery {
  page?: number;
  limit?: number;
}

export interface PortfolioFilterQuery extends PaginationQuery {
  status?: PortfolioStatus;
  branch?: string;
  client?: string;
}

export interface SystemMetrics {
  portfolioCount: number;
  activePortfolioCount: number;
  positionCount: number;
  transactionCount: number;
  errorCount: number;
  lastBatchRun?: BatchJobStatus;
}
