// Data type mappings from COBOL PIC clauses to TypeScript
// PIC X(n) → string (max length n)
// PIC 9(n) → number (integer)
// PIC S9(n)V9(m) COMP-3 → Decimal (use decimal.js for precision)
// 88-level conditions → TypeScript enums/union types

// Portfolio status — from PORTFLIO.cpy 88-level conditions
export enum PortfolioStatus {
  Active = 'A',
  Closed = 'C',
  Suspended = 'S',
}

// Client type — from PORTFLIO.cpy
export enum ClientType {
  Individual = 'I',
  Corporate = 'C',
  Trust = 'T',
}

// Transaction type — from TRNREC.cpy 88-level conditions
export enum TransactionType {
  Buy = 'BU',
  Sell = 'SL',
  Transfer = 'TR',
  Fee = 'FE',
}

// Transaction status — from TRNREC.cpy 88-level conditions
export enum TransactionStatus {
  Pending = 'P',
  Done = 'D',
  Failed = 'F',
  Reversed = 'R',
}

// Position status — from POSREC.cpy
export enum PositionStatus {
  Active = 'A',
  Closed = 'C',
  Pending = 'P',
}

// Audit record type — from HISTREC.cpy
export enum AuditRecordType {
  Portfolio = 'PT',
  Position = 'PS',
  Transaction = 'TR',
}

// Audit action code — from HISTREC.cpy
export enum AuditActionCode {
  Add = 'A',
  Change = 'C',
  Delete = 'D',
}

// Batch job status — from BCHCTL.cpy
export enum BatchJobStatus {
  Ready = 'R',
  Active = 'A',
  Waiting = 'W',
  Done = 'D',
  Error = 'E',
}

// Error codes — from ERRHAND.cpy
export enum ErrorSeverity {
  Success = 0,
  Warning = 4,
  Error = 8,
  Severe = 12,
  Terminal = 16,
}

// Error category — from ERRHAND.cpy
export enum ErrorCategory {
  VSAM = 'VS',
  Validation = 'VL',
  Processing = 'PR',
  System = 'SY',
}

// Validation return codes — from PORTVAL.cpy
export enum ValidationCode {
  Success = 0,
  InvalidId = 1,
  InvalidAccount = 2,
  InvalidType = 3,
  InvalidAmount = 4,
}

// Investment types — from PORTVALD.cbl
export enum InvestmentType {
  Stock = 'STK',
  Bond = 'BND',
  MoneyMarket = 'MMF',
  ETF = 'ETF',
}

// Delete reason — from PORTDEL.cbl
export enum DeleteReason {
  Closed = '01',
  Transferred = '02',
  Requested = '03',
}

// Risk levels
export type RiskLevel = '1' | '2' | '3' | '4' | '5';

// WebSocket event types
export enum WSEvent {
  TransactionCreated = 'transaction:created',
  TransactionUpdated = 'transaction:updated',
  PositionUpdated = 'position:updated',
  BatchStarted = 'batch:started',
  BatchProgress = 'batch:progress',
  BatchCompleted = 'batch:completed',
  BatchFailed = 'batch:failed',
  SystemAlert = 'system:alert',
}

// API response wrapper
export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: ApiError;
  pagination?: PaginationInfo;
}

export interface ApiError {
  code: string;
  message: string;
  category: ErrorCategory;
  severity: ErrorSeverity;
  details?: string;
  traceId?: string;
}

export interface PaginationInfo {
  page: number;
  pageSize: number;
  totalCount: number;
  totalPages: number;
}

// Create portfolio request
export interface CreatePortfolioRequest {
  portfolioId: string;
  accountType: string;
  branchId: string;
  clientId: string;
  portfolioName: string;
  currencyCode: string;
  riskLevel: string;
}

// Update portfolio request
export interface UpdatePortfolioRequest {
  portfolioName?: string;
  status?: PortfolioStatus;
  riskLevel?: string;
  cashBalance?: number;
}

// Create transaction request
export interface CreateTransactionRequest {
  portfolioId: string;
  investmentId: string;
  transactionType: TransactionType;
  quantity: number;
  price: number;
  currencyCode: string;
}

// Batch run request
export interface BatchRunRequest {
  jobName?: string;
  processDate?: string;
}

// Report query params
export interface ReportQuery {
  startDate?: string;
  endDate?: string;
  portfolioId?: string;
  format?: 'json' | 'csv';
}

// System health response
export interface SystemHealth {
  status: 'healthy' | 'degraded' | 'down';
  uptime: number;
  database: 'connected' | 'disconnected';
  websocket: 'active' | 'inactive';
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

// JWT payload
export interface JwtPayload {
  userId: string;
  username: string;
  role: string;
}
