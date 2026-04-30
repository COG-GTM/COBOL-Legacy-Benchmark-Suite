// TypeScript types migrated from COBOL copybooks and DB2 schemas
// Maps COBOL data structures to TypeScript interfaces

import { Decimal } from "decimal.js";

// --- Portfolio types (from PORTFLIO.cpy / PORT-RECORD) ---

export type PortfolioStatus = "A" | "C" | "S"; // Active, Closed, Suspended
export type ClientType = "I" | "C" | "T"; // Individual, Corporate, Trust
export type RiskLevel = "1" | "2" | "3" | "4" | "5";

export interface Portfolio {
  portfolioId: string;
  accountType: string;
  branchId: string;
  clientId: string;
  portfolioName: string;
  currencyCode: string;
  riskLevel: RiskLevel;
  status: PortfolioStatus;
  openDate: Date;
  closeDate: Date | null;
  lastMaintDate: Date;
  lastMaintUser: string;
}

// --- Position types (from POSREC.cpy / POSITION-RECORD) ---

export type PositionStatus = "A" | "C" | "P"; // Active, Closed, Pending

export interface Position {
  portfolioId: string;
  investmentId: string;
  positionDate: Date;
  quantity: Decimal;
  costBasis: Decimal;
  marketValue: Decimal;
  currencyCode: string;
  lastMaintDate: Date;
  lastMaintUser: string;
}

// --- Transaction types (from TRNREC.cpy / TRANSACTION-RECORD) ---

export type TransactionType = "BU" | "SL" | "TR" | "FE"; // Buy, Sell, Transfer, Fee
export type TransactionStatus = "P" | "D" | "F" | "R"; // Pending, Done, Failed, Reversed

export interface Transaction {
  transactionId: string;
  portfolioId: string;
  transactionDate: Date;
  transactionTime: Date;
  investmentId: string;
  transactionType: TransactionType;
  quantity: Decimal;
  price: Decimal;
  amount: Decimal;
  currencyCode: string;
  status: TransactionStatus;
  processDate: Date;
  processUser: string;
}

// --- Error types (from ERRHAND.cpy / ERR-MESSAGE, ERRLOG.sql) ---

export type ErrorType = "S" | "A" | "D"; // System, Application, Data
export type ErrorSeverity = 1 | 2 | 3 | 4; // Info, Warning, Error, Severe

/** Maps COBOL return codes (0/4/8/12/16) to severity levels */
export type CobolReturnCode = 0 | 4 | 8 | 12 | 16;

export const COBOL_RC_TO_HTTP: Record<CobolReturnCode, number> = {
  0: 200,
  4: 200,
  8: 400,
  12: 500,
  16: 500,
};

export const COBOL_RC_TO_SEVERITY: Record<CobolReturnCode, ErrorSeverity> = {
  0: 1,
  4: 2,
  8: 3,
  12: 4,
  16: 4,
};

export interface ErrorRecord {
  errorTimestamp: Date;
  programId: string;
  errorType: ErrorType;
  errorSeverity: ErrorSeverity;
  errorCode: string;
  errorMessage: string;
  processDate: Date;
  processTime: Date;
  userId: string;
  additionalInfo?: string;
}

// Error categories from ERRHAND.cpy ERR-CATEGORIES
export const ERROR_CATEGORIES = {
  VSAM: "VS",
  VALIDATION: "VL",
  PROCESSING: "PR",
  SYSTEM: "SY",
} as const;

// --- Audit types (from AUDITLOG.cpy / AUDIT-RECORD) ---

export type AuditType = "TRAN" | "USER" | "SYST";
export type AuditAction =
  | "CREATE"
  | "UPDATE"
  | "DELETE"
  | "INQUIRE"
  | "LOGIN"
  | "LOGOUT"
  | "STARTUP"
  | "SHUTDOWN";
export type AuditStatus = "SUCC" | "FAIL" | "WARN";

export interface AuditRecord {
  timestamp: Date;
  systemId: string;
  userId: string;
  programId: string;
  terminal?: string;
  auditType: AuditType;
  action: AuditAction;
  status: AuditStatus;
  portfolioId?: string;
  accountNo?: string;
  beforeImage?: string;
  afterImage?: string;
  message?: string;
}

// --- Report output types ---

export interface PositionReportEntry {
  portfolioId: string;
  portfolioName: string;
  investmentId: string;
  quantity: string;
  costBasis: string;
  marketValue: string;
  gainLoss: string;
  gainLossPct: string;
  currencyCode: string;
}

export interface PositionReportSummary {
  reportDate: string;
  portfolioId: string;
  portfolioName: string;
  totalCostBasis: string;
  totalMarketValue: string;
  totalGainLoss: string;
  totalGainLossPct: string;
  currencyCode: string;
  positions: PositionReportEntry[];
}

export interface PositionReport {
  reportDate: string;
  generatedAt: string;
  portfolios: PositionReportSummary[];
  grandTotalCostBasis: string;
  grandTotalMarketValue: string;
  grandTotalGainLoss: string;
}

export interface AuditReportEntry {
  timestamp: string;
  userId: string;
  programId: string;
  action: string;
  status: string;
  portfolioId: string;
  accountNo: string;
  beforeImage: string;
  afterImage: string;
  message: string;
}

export interface AuditReportFilters {
  startDate?: Date;
  endDate?: Date;
  userId?: string;
  portfolioId?: string;
  action?: AuditAction;
}

export interface AuditReportOutput {
  reportDate: string;
  generatedAt: string;
  filters: Record<string, string>;
  totalRecords: number;
  entries: AuditReportEntry[];
  summary: {
    byAction: Record<string, number>;
    byStatus: Record<string, number>;
    byUser: Record<string, number>;
  };
}

export interface StatisticsMetrics {
  reportDate: string;
  generatedAt: string;
  portfolioMetrics: {
    totalPortfolios: number;
    activePortfolios: number;
    totalMarketValue: string;
    totalCostBasis: string;
    overallReturn: string;
  };
  transactionMetrics: {
    totalTransactions: number;
    byType: Record<string, number>;
    byStatus: Record<string, number>;
    totalVolume: string;
    periodStart: string;
    periodEnd: string;
  };
  errorMetrics: {
    totalErrors: number;
    bySeverity: Record<string, number>;
    byProgram: Record<string, number>;
    errorRate: string;
  };
  trendData: {
    dailyTransactionVolumes: Array<{ date: string; count: number; volume: string }>;
    dailyErrorCounts: Array<{ date: string; count: number }>;
  };
}

// --- API response types ---

export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: {
    code: string;
    message: string;
    details?: string;
    severity: ErrorSeverity;
    httpStatus: number;
  };
  metadata?: {
    generatedAt: string;
    processingTimeMs: number;
  };
}

export interface HealthCheckResponse {
  status: "healthy" | "degraded" | "unhealthy";
  database: {
    connected: boolean;
    responseTimeMs: number;
    error?: string;
  };
  timestamp: string;
  version: string;
}
