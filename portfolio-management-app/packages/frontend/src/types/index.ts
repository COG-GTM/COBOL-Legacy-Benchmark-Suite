// Mirrors backend types with frontend-friendly additions

export interface Portfolio {
  portfolioId: string;
  accountType: string;
  branchId: string;
  clientId: string;
  portfolioName: string;
  currencyCode: string;
  riskLevel: string;
  status: string;
  openDate: string;
  closeDate: string | null;
  lastMaintDate: string;
  lastMaintUser: string;
  totalValue: number;
  cashBalance: number;
  positions?: InvestmentPosition[];
}

export interface InvestmentPosition {
  portfolioId: string;
  investmentId: string;
  positionDate: string;
  quantity: number;
  costBasis: number;
  marketValue: number;
  currencyCode: string;
  lastMaintDate: string;
  lastMaintUser: string;
}

export interface Transaction {
  transactionId: string;
  portfolioId: string;
  transactionDate: string;
  transactionTime: string;
  investmentId: string;
  transactionType: string;
  quantity: number;
  price: number;
  amount: number;
  currencyCode: string;
  status: string;
  processDate: string;
  processUser: string;
}

export interface BatchJob {
  id: number;
  jobName: string;
  processDate: string;
  sequenceNo: number;
  status: string;
  stepName: string | null;
  programName: string | null;
  startTime: string | null;
  endTime: string | null;
  returnCode: number | null;
  errorDesc: string | null;
  recordsRead: number;
  recordsWritten: number;
  errorCount: number;
}

export interface AuditLog {
  id: number;
  portfolioId: string;
  date: string;
  time: string;
  seqNo: string;
  recordType: string;
  actionCode: string;
  beforeImage: string | null;
  afterImage: string | null;
  reasonCode: string | null;
  processDate: string;
  processUser: string;
}

export interface SystemHealth {
  status: 'healthy' | 'degraded' | 'down';
  uptime: number;
  database: string;
  websocket: string;
  metrics: SystemMetrics;
}

export interface SystemMetrics {
  totalPortfolios: number;
  activePortfolios: number;
  pendingTransactions: number;
  totalTransactions: number;
  lastBatchRun?: string;
  batchJobsToday: number;
}

export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: { code: string; message: string };
  pagination?: PaginationInfo;
}

export interface PaginationInfo {
  page: number;
  pageSize: number;
  totalCount: number;
  totalPages: number;
}

export interface AuthUser {
  userId: string;
  username: string;
  role: string;
}

export interface LoginResponse {
  token: string;
  user: AuthUser;
}

export interface StatisticsReport {
  reportDate: string;
  portfolios: { total: number; active: number; closed: number; suspended: number };
  transactions: { total: number; pending: number; done: number; failed: number };
  positions: { total: number };
  batch: { totalJobs: number; recentJobs: BatchJob[]; successRate: number };
}
