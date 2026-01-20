/**
 * Error Log Model
 * Migrated from: ERRLOG.sql DB2 table, ERRHAND.cpy
 * 
 * Represents error and warning records for system monitoring and debugging.
 */

import { ErrorType, ErrorSeverity, ReturnCode } from '../types';

/**
 * Error Log record
 * From: ERRLOG.sql DB2 table
 */
export interface ErrorLog {
  /** Error timestamp */
  errorTimestamp: Date;
  /** Program ID that generated the error (8 characters) */
  programId: string;
  /** Error type: System, Application, or Data */
  errorType: ErrorType;
  /** Error severity level */
  errorSeverity: ErrorSeverity;
  /** Error code (8 characters) */
  errorCode: string;
  /** Error message (up to 200 characters) */
  errorMessage: string;
  /** Process date */
  processDate: Date;
  /** Process time */
  processTime: string;
  /** User ID (8 characters) */
  userId: string;
  /** Additional information (up to 500 characters) */
  additionalInfo: string;
}

/**
 * Error message structure
 * From: ERRHAND.cpy (ERR-MESSAGE)
 */
export interface ErrorMessage {
  /** Error timestamp */
  timestamp: {
    date: string;
    time: string;
  };
  /** Program name (8 characters) */
  program: string;
  /** Error category (2 characters) */
  category: string;
  /** Error code (4 characters) */
  code: string;
  /** Error severity */
  severity: ErrorSeverity;
  /** Error text (up to 80 characters) */
  text: string;
  /** Error details (up to 256 characters) */
  details: string;
}

/**
 * Error categories
 * From: ERRHAND.cpy (ERR-CATEGORIES)
 */
export enum ErrorCategory {
  VSAM = 'VS',
  VALIDATION = 'VL',
  PROCESSING = 'PR',
  SYSTEM = 'SY',
}

/**
 * Standard error codes
 * From: data-dictionary.md
 */
export const StandardErrorCodes = {
  E001: { code: 'E001', message: 'Invalid Account Number', severity: ErrorSeverity.ERROR },
  E002: { code: 'E002', message: 'Invalid Fund ID', severity: ErrorSeverity.ERROR },
  E003: { code: 'E003', message: 'Invalid Transaction Type', severity: ErrorSeverity.ERROR },
  E004: { code: 'E004', message: 'Insufficient Position Balance', severity: ErrorSeverity.ERROR },
  W001: { code: 'W001', message: 'Zero Dollar Transaction', severity: ErrorSeverity.WARNING },
  W002: { code: 'W002', message: 'Duplicate Transaction ID', severity: ErrorSeverity.WARNING },
} as const;

/**
 * VSAM status codes
 * From: ERRHAND.cpy (ERR-VSAM-STATUSES)
 */
export const VsamStatusCodes = {
  SUCCESS: '00',
  DUPLICATE_KEY: '22',
  NOT_FOUND: '23',
  END_OF_FILE: '10',
} as const;

/**
 * Error log creation request
 */
export interface CreateErrorLogRequest {
  programId: string;
  errorType: ErrorType;
  errorSeverity: ErrorSeverity;
  errorCode: string;
  errorMessage: string;
  userId: string;
  additionalInfo?: string;
}

/**
 * Error log search criteria
 */
export interface ErrorLogSearchCriteria {
  errorTimestampFrom?: Date;
  errorTimestampTo?: Date;
  programId?: string;
  errorType?: ErrorType;
  errorSeverity?: ErrorSeverity;
  errorCode?: string;
  userId?: string;
  processDateFrom?: Date;
  processDateTo?: Date;
}

/**
 * Error log summary for list views
 */
export interface ErrorLogSummary {
  errorTimestamp: Date;
  programId: string;
  errorType: ErrorType;
  errorSeverity: ErrorSeverity;
  errorCode: string;
  errorMessage: string;
}

/**
 * Error log page result
 * Used for paginated error queries
 */
export interface ErrorLogPage {
  records: ErrorLog[];
  totalCount: number;
  pageNumber: number;
  pageSize: number;
  hasMore: boolean;
}

/**
 * Error statistics for reporting
 */
export interface ErrorStatistics {
  periodStart: Date;
  periodEnd: Date;
  totalErrors: number;
  byType: Record<ErrorType, number>;
  bySeverity: Record<ErrorSeverity, number>;
  byProgram: Record<string, number>;
  byErrorCode: Record<string, number>;
}

/**
 * Return code tracking
 * From: RTNCODES.sql DB2 table
 */
export interface ReturnCodeRecord {
  /** Timestamp */
  timestamp: Date;
  /** Program ID (8 characters) */
  programId: string;
  /** Return code */
  returnCode: ReturnCode;
  /** Highest return code encountered */
  highestCode: ReturnCode;
  /** Status code (1 character) */
  statusCode: string;
  /** Message text (up to 80 characters) */
  messageText: string;
}

/**
 * Factory function to create a default ErrorLog object
 */
export function createDefaultErrorLog(): ErrorLog {
  const now = new Date();
  const timeStr = now.toISOString().slice(11, 19).replace(/:/g, '');

  return {
    errorTimestamp: now,
    programId: '',
    errorType: ErrorType.APPLICATION,
    errorSeverity: ErrorSeverity.ERROR,
    errorCode: '',
    errorMessage: '',
    processDate: now,
    processTime: timeStr,
    userId: '',
    additionalInfo: '',
  };
}

/**
 * Factory function to create a default ErrorMessage object
 */
export function createDefaultErrorMessage(): ErrorMessage {
  const now = new Date();
  const dateStr = now.toISOString().slice(0, 10);
  const timeStr = now.toISOString().slice(11, 19);

  return {
    timestamp: {
      date: dateStr,
      time: timeStr,
    },
    program: '',
    category: ErrorCategory.PROCESSING,
    code: '',
    severity: ErrorSeverity.ERROR,
    text: '',
    details: '',
  };
}

/**
 * Get error type display name
 */
export function getErrorTypeDisplayName(type: ErrorType): string {
  switch (type) {
    case ErrorType.SYSTEM:
      return 'System';
    case ErrorType.APPLICATION:
      return 'Application';
    case ErrorType.DATA:
      return 'Data';
    default:
      return 'Unknown';
  }
}

/**
 * Get error severity display name
 */
export function getErrorSeverityDisplayName(severity: ErrorSeverity): string {
  switch (severity) {
    case ErrorSeverity.INFO:
      return 'Info';
    case ErrorSeverity.WARNING:
      return 'Warning';
    case ErrorSeverity.ERROR:
      return 'Error';
    case ErrorSeverity.SEVERE:
      return 'Severe';
    default:
      return 'Unknown';
  }
}

/**
 * Get return code display name
 */
export function getReturnCodeDisplayName(code: ReturnCode): string {
  switch (code) {
    case ReturnCode.SUCCESS:
      return 'Success (0)';
    case ReturnCode.WARNING:
      return 'Warning (4)';
    case ReturnCode.ERROR:
      return 'Error (8)';
    case ReturnCode.SEVERE:
      return 'Severe (12)';
    case ReturnCode.CRITICAL:
      return 'Critical (16)';
    default:
      return 'Unknown';
  }
}

/**
 * Determine if an error is critical and requires immediate attention
 */
export function isCriticalError(error: ErrorLog): boolean {
  return error.errorSeverity === ErrorSeverity.SEVERE ||
    error.errorType === ErrorType.SYSTEM;
}

/**
 * Format error for logging
 */
export function formatErrorForLogging(error: ErrorLog): string {
  return `[${error.errorTimestamp.toISOString()}] ${error.programId} - ` +
    `${getErrorSeverityDisplayName(error.errorSeverity)}: ` +
    `${error.errorCode} - ${error.errorMessage}`;
}
