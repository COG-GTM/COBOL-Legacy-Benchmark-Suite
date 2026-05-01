// TypeScript interfaces mirroring COBOL copybooks
import {
  ClientType,
  PortfolioStatus,
  PositionStatus,
  TransactionType,
  TransactionStatus,
  RecordType,
  AuditAction,
  UserRole,
  JobStatus,
} from './enums';

// ============================================================
// Portfolio (from PORTFLIO.cpy PORT-RECORD)
// ============================================================
export interface Portfolio {
  id: string;
  portfolioId: string;    // PORT-ID: PIC X(8)
  accountNo: string;      // PORT-ACCOUNT-NO: PIC X(10)
  clientName: string;     // PORT-CLIENT-NAME: PIC X(30)
  clientType: ClientType; // PORT-CLIENT-TYPE: I/C/T
  status: PortfolioStatus;
  totalValue: number;     // PORT-TOTAL-VALUE: PIC S9(13)V99
  cashBalance: number;    // PORT-CASH-BALANCE: PIC S9(13)V99
  currencyCode: string;
  riskLevel: string;
  branchId: string;
  openDate: string;
  closeDate: string | null;
  createdAt: string;
  updatedAt: string;
  lastUser: string;       // PORT-LAST-USER: PIC X(8)
}

export interface CreatePortfolioInput {
  portfolioId: string;
  accountNo: string;
  clientName: string;
  clientType: ClientType;
  status?: PortfolioStatus;
  totalValue?: number;
  cashBalance?: number;
  currencyCode?: string;
  riskLevel?: string;
  branchId?: string;
}

export interface UpdatePortfolioInput {
  clientName?: string;
  clientType?: ClientType;
  status?: PortfolioStatus;
  totalValue?: number;
  cashBalance?: number;
  currencyCode?: string;
  riskLevel?: string;
}

// ============================================================
// Position (from POSREC.cpy POSITION-RECORD)
// ============================================================
export interface Position {
  id: string;
  portfolioId: string;    // POS-PORTFOLIO-ID: PIC X(08)
  investmentId: string;   // POS-INVESTMENT-ID: PIC X(10)
  positionDate: string;   // POS-DATE: PIC X(08)
  quantity: number;       // POS-QUANTITY: PIC S9(11)V9(4)
  costBasis: number;      // POS-COST-BASIS: PIC S9(13)V9(2)
  marketValue: number;    // POS-MARKET-VALUE: PIC S9(13)V9(2)
  currency: string;       // POS-CURRENCY: PIC X(03)
  status: PositionStatus; // POS-STATUS: A/C/P
  updatedAt: string;
  lastUser: string;
}

export interface CreatePositionInput {
  investmentId: string;
  positionDate: string;
  quantity: number;
  costBasis: number;
  marketValue: number;
  currency?: string;
  status?: PositionStatus;
}

// ============================================================
// Transaction (from TRNREC.cpy TRANSACTION-RECORD)
// ============================================================
export interface Transaction {
  id: string;
  transactionId: string;      // TRN-KEY composite
  portfolioId: string;        // TRN-PORTFOLIO-ID: PIC X(08)
  transactionDate: string;    // TRN-DATE: PIC X(08)
  transactionTime: string;    // TRN-TIME: PIC X(06)
  investmentId: string;       // TRN-INVESTMENT-ID: PIC X(10)
  type: TransactionType;      // TRN-TYPE: BU/SL/TR/FE
  quantity: number;           // TRN-QUANTITY: PIC S9(11)V9(4)
  price: number;              // TRN-PRICE: PIC S9(11)V9(4)
  amount: number;             // TRN-AMOUNT: PIC S9(13)V9(2)
  currency: string;           // TRN-CURRENCY: PIC X(03)
  status: TransactionStatus;  // TRN-STATUS: P/D/F/R
  processedAt: string | null;
  processUser: string;        // TRN-PROCESS-USER: PIC X(08)
  createdAt: string;
}

export interface CreateTransactionInput {
  portfolioId: string;
  investmentId: string;
  type: TransactionType;
  quantity: number;
  price: number;
  currency?: string;
}

// ============================================================
// Position History (from POSHIST table)
// ============================================================
export interface PositionHistory {
  id: string;
  accountNo: string;
  portfolioId: string;
  transDate: string;
  transTime: string;
  transType: string;
  securityId: string;
  quantity: number;
  price: number;
  amount: number;
  fees: number;
  totalAmount: number;
  costBasis: number;
  gainLoss: number;
  processDate: string;
  programId: string;
  userId: string;
  createdAt: string;
}

// ============================================================
// Audit Log (from HISTREC.cpy + AUDITLOG.cpy)
// ============================================================
export interface AuditLog {
  id: string;
  portfolioId: string | null;
  recordType: RecordType;     // HIST-RECORD-TYPE: PT/PS/TR
  action: AuditAction;       // HIST-ACTION-CODE: A/C/D
  beforeImage: unknown;       // HIST-BEFORE-IMAGE (JSONB)
  afterImage: unknown;        // HIST-AFTER-IMAGE (JSONB)
  reasonCode: string | null;  // HIST-REASON-CODE: PIC X(04)
  userId: string;
  programId: string | null;
  message: string | null;
  createdAt: string;
}

// ============================================================
// User (replacing SECMGR)
// ============================================================
export interface User {
  id: string;
  username: string;
  email: string;
  role: UserRole;
  isActive: boolean;
  lastLogin: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface LoginInput {
  username: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  user: Omit<User, 'passwordHash'>;
}

// ============================================================
// Batch Job (replacing BCHCTL00)
// ============================================================
export interface BatchJob {
  id: string;
  type: string;
  status: JobStatus;
  progress: number;
  result: unknown;
  error: string | null;
  startedAt: string | null;
  completedAt: string | null;
  createdAt: string;
}

// ============================================================
// API Response Types
// ============================================================
export interface PaginatedResponse<T> {
  data: T[];
  total: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

export interface ApiError {
  code: string;
  message: string;
  category: string;
  severity: number;
  details?: string;
}

export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: ApiError;
}

// ============================================================
// Report Types (replacing RPTPOS00, RPTAUD00, RPTSTA00)
// ============================================================
export interface PositionReport {
  portfolioId: string;
  portfolioName: string;
  positions: Position[];
  totalCostBasis: number;
  totalMarketValue: number;
  totalGainLoss: number;
}

export interface AuditReport {
  entries: AuditLog[];
  total: number;
  dateRange: { from: string; to: string };
}

export interface SystemStatistics {
  totalPortfolios: number;
  activePortfolios: number;
  totalPositions: number;
  totalTransactions: number;
  pendingTransactions: number;
  totalValue: number;
  recentActivity: { date: string; count: number }[];
}
