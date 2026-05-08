/**
 * Shared Error Types for the COBOL-to-React Error Handling System
 *
 * Maps COBOL error handling concepts from:
 * - ERRHND.cpy: Severity levels (F/W/I), actions (R/C/A)
 * - ERRHAND.cpy: Error categories (VS/VL/PR/SY), return codes (0/4/8/12/16)
 * - RETHND.cpy: Error types (V/P/D/F/S), standard error codes (E001-E010)
 * - ERRMAP BMS: Error display screen layout
 */

export type ErrorSeverity = 'error' | 'warning' | 'info'

export type ErrorCategory = 'vsam' | 'validation' | 'processing' | 'system'

export type ErrorType = 'validation' | 'processing' | 'database' | 'file' | 'security'

export type ErrorAction = 'return' | 'continue' | 'abort' | 'retry'

export interface AppError {
  code: string
  message: string
  severity: ErrorSeverity
  category?: ErrorCategory
  type?: ErrorType
  details?: string
  action?: ErrorAction
  timestamp?: string
  retryCount?: number
  maxRetries?: number
}

export const RETURN_CODES = {
  SUCCESS: 0,
  WARNING: 4,
  ERROR: 8,
  SEVERE: 12,
  TERMINAL: 16,
} as const

export type ReturnCode = (typeof RETURN_CODES)[keyof typeof RETURN_CODES]

export const ERROR_CODES = {
  E001: { code: 'E001', message: 'Record not found', severity: 'error' as ErrorSeverity },
  E002: { code: 'E002', message: 'Duplicate record', severity: 'error' as ErrorSeverity },
  E003: { code: 'E003', message: 'Invalid input data', severity: 'warning' as ErrorSeverity },
  E004: { code: 'E004', message: 'Database error', severity: 'error' as ErrorSeverity },
  E005: { code: 'E005', message: 'File access error', severity: 'error' as ErrorSeverity },
  E006: { code: 'E006', message: 'Authorization failed', severity: 'error' as ErrorSeverity },
  E007: { code: 'E007', message: 'Processing error', severity: 'error' as ErrorSeverity },
  E008: { code: 'E008', message: 'Communication error', severity: 'warning' as ErrorSeverity },
  E009: { code: 'E009', message: 'Resource unavailable', severity: 'warning' as ErrorSeverity },
  E010: { code: 'E010', message: 'System error', severity: 'error' as ErrorSeverity },
} as const

export type ErrorCode = keyof typeof ERROR_CODES

export function createAppError(
  code: string,
  message: string,
  severity: ErrorSeverity = 'error',
  options?: Partial<Omit<AppError, 'code' | 'message' | 'severity'>>,
): AppError {
  return {
    code,
    message,
    severity,
    timestamp: new Date().toISOString(),
    ...options,
  }
}

export function mapReturnCodeToSeverity(returnCode: ReturnCode): ErrorSeverity {
  switch (returnCode) {
    case RETURN_CODES.SUCCESS:
      return 'info'
    case RETURN_CODES.WARNING:
      return 'warning'
    case RETURN_CODES.ERROR:
    case RETURN_CODES.SEVERE:
    case RETURN_CODES.TERMINAL:
      return 'error'
    default:
      return 'error'
  }
}

export function mapHttpStatusToSeverity(status: number): ErrorSeverity {
  if (status >= 400 && status < 500) return 'warning'
  if (status >= 500) return 'error'
  return 'info'
}
