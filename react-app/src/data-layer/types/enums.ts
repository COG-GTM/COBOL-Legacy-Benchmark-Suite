/**
 * Enums for the Investment Portfolio Management System
 * Migrated from COBOL copybooks (COMMON.cpy, TRNREC.cpy, POSREC.cpy, etc.)
 */

/**
 * Portfolio status codes
 * From: PORTFLIO.cpy (PORT-STATUS field)
 */
export enum PortfolioStatus {
  ACTIVE = 'A',
  CLOSED = 'C',
  SUSPENDED = 'S',
}

/**
 * Position status codes
 * From: POSREC.cpy (POS-STATUS field)
 */
export enum PositionStatus {
  ACTIVE = 'A',
  CLOSED = 'C',
  PENDING = 'P',
}

/**
 * Transaction types
 * From: TRNREC.cpy (TRN-TYPE field) and COMMON.cpy
 */
export enum TransactionType {
  BUY = 'BU',
  SELL = 'SL',
  TRANSFER = 'TR',
  FEE = 'FE',
}

/**
 * Transaction status codes
 * From: TRNREC.cpy (TRN-STATUS field)
 */
export enum TransactionStatus {
  PENDING = 'P',
  DONE = 'D',
  FAILED = 'F',
  REVERSED = 'R',
}

/**
 * Client types
 * From: PORTFLIO.cpy (PORT-CLIENT-TYPE field)
 */
export enum ClientType {
  INDIVIDUAL = 'I',
  CORPORATE = 'C',
  TRUST = 'T',
}

/**
 * Batch control status codes
 * From: BCHCTL.cpy (BCT-STATUS field)
 */
export enum BatchStatus {
  READY = 'R',
  ACTIVE = 'A',
  WAITING = 'W',
  DONE = 'D',
  ERROR = 'E',
}

/**
 * Audit action types
 * From: AUDITLOG.cpy (AUD-ACTION field)
 */
export enum AuditAction {
  CREATE = 'CREATE',
  UPDATE = 'UPDATE',
  DELETE = 'DELETE',
  INQUIRE = 'INQUIRE',
  LOGIN = 'LOGIN',
  LOGOUT = 'LOGOUT',
  STARTUP = 'STARTUP',
  SHUTDOWN = 'SHUTDOWN',
}

/**
 * Audit types
 * From: AUDITLOG.cpy (AUD-TYPE field)
 */
export enum AuditType {
  TRANSACTION = 'TRAN',
  USER_ACTION = 'USER',
  SYSTEM_EVENT = 'SYST',
}

/**
 * Audit status codes
 * From: AUDITLOG.cpy (AUD-STATUS field)
 */
export enum AuditStatus {
  SUCCESS = 'SUCC',
  FAILURE = 'FAIL',
  WARNING = 'WARN',
}

/**
 * Error types
 * From: ERRLOG.sql (ERROR_TYPE column)
 */
export enum ErrorType {
  SYSTEM = 'S',
  APPLICATION = 'A',
  DATA = 'D',
}

/**
 * Error severity levels
 * From: ERRLOG.sql (ERROR_SEVERITY column)
 */
export enum ErrorSeverity {
  INFO = 1,
  WARNING = 2,
  ERROR = 3,
  SEVERE = 4,
}

/**
 * Return codes
 * From: COMMON.cpy (RETURN-CODES) and ERRHAND.cpy
 */
export enum ReturnCode {
  SUCCESS = 0,
  WARNING = 4,
  ERROR = 8,
  SEVERE = 12,
  CRITICAL = 16,
}

/**
 * Currency codes
 * From: COMMON.cpy (CURRENCY-CODES)
 */
export enum CurrencyCode {
  USD = 'USD',
  EUR = 'EUR',
  GBP = 'GBP',
  JPY = 'JPY',
  CAD = 'CAD',
}

/**
 * History record types
 * From: HISTREC.cpy (HIST-RECORD-TYPE field)
 */
export enum HistoryRecordType {
  PORTFOLIO = 'PT',
  POSITION = 'PS',
  TRANSACTION = 'TR',
}

/**
 * History action codes
 * From: HISTREC.cpy (HIST-ACTION-CODE field)
 */
export enum HistoryActionCode {
  ADD = 'A',
  CHANGE = 'C',
  DELETE = 'D',
}

/**
 * Inquiry function codes
 * From: INQCOM.cpy (INQCOM-FUNCTION field)
 */
export enum InquiryFunction {
  MENU = 'MENU',
  PORTFOLIO = 'INQP',
  HISTORY = 'INQH',
  EXIT = 'EXIT',
}
