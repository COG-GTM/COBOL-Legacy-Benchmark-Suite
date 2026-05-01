// Status codes from COBOL copybooks

// Portfolio status (from PORTFLIO.cpy level 88)
export enum ClientType {
  INDIVIDUAL = 'I',
  CORPORATE = 'C',
  TRUST = 'T',
}

export enum PortfolioStatus {
  ACTIVE = 'A',
  CLOSED = 'C',
  SUSPENDED = 'S',
}

// Position status (from POSREC.cpy level 88)
export enum PositionStatus {
  ACTIVE = 'A',
  CLOSED = 'C',
  PENDING = 'P',
}

// Transaction types (from TRNREC.cpy level 88)
export enum TransactionType {
  BUY = 'BU',
  SELL = 'SL',
  TRANSFER = 'TR',
  FEE = 'FE',
}

// Transaction status (from TRNREC.cpy level 88)
export enum TransactionStatus {
  PENDING = 'P',
  DONE = 'D',
  FAILED = 'F',
  REVERSED = 'R',
}

// History record types (from HISTREC.cpy level 88)
export enum RecordType {
  PORTFOLIO = 'PT',
  POSITION = 'PS',
  TRANSACTION = 'TR',
}

// Audit action codes (from HISTREC.cpy level 88)
export enum AuditAction {
  ADD = 'A',
  CHANGE = 'C',
  DELETE = 'D',
}

// Error categories (from ERRHAND.cpy)
export enum ErrorCategory {
  VSAM = 'VS',
  VALIDATION = 'VL',
  PROCESSING = 'PR',
  SYSTEM = 'SY',
}

// Error severity levels (from ERRHAND.cpy return codes)
export enum ErrorSeverity {
  SUCCESS = 0,
  WARNING = 4,
  ERROR = 8,
  SEVERE = 12,
  TERMINAL = 16,
}

// User roles (replacing SECMGR access types)
export enum UserRole {
  READ = 'READ',
  UPDATE = 'UPDATE',
  ADMIN = 'ADMIN',
}

// Batch job status
export enum JobStatus {
  QUEUED = 'QUEUED',
  RUNNING = 'RUNNING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
}
