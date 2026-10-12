import type { ApiError, ApiErrorCode, ErrorAction, ErrorSeverity } from './types';
import { messages } from '../i18n/messages';

/** Thrown by the API client for any non-2xx response or transport failure. */
export class ApiRequestError extends Error {
  readonly status: number;
  readonly body: ApiError;

  constructor(status: number, body: ApiError) {
    super(body.message);
    this.name = 'ApiRequestError';
    this.status = status;
    this.body = body;
  }

  get isServerError(): boolean {
    return this.status >= 500;
  }
}

/** Section 7.3 error catalogue: default text, severity and action per code. */
export const ERROR_CATALOGUE: Record<
  ApiErrorCode,
  { status: number; message: string; legacyCode: string; severity: ErrorSeverity; action: ErrorAction }
> = {
  INVALID_ACCOUNT_FORMAT: { status: 400, message: messages.errors.INVALID_ACCOUNT_FORMAT, legacyCode: 'E008', severity: 'WARNING', action: 'CONTINUE' },
  POSITION_NOT_FOUND: { status: 404, message: messages.errors.POSITION_NOT_FOUND, legacyCode: 'E002', severity: 'WARNING', action: 'CONTINUE' },
  POSITION_READ_ERROR: { status: 500, message: messages.errors.POSITION_READ_ERROR, legacyCode: 'E004', severity: 'ERROR', action: 'CONTINUE' },
  HISTORY_READ_ERROR: { status: 500, message: messages.errors.HISTORY_READ_ERROR, legacyCode: 'E005', severity: 'ERROR', action: 'CONTINUE' },
  DB_UNAVAILABLE: { status: 503, message: messages.errors.DB_UNAVAILABLE, legacyCode: 'E010', severity: 'ERROR', action: 'CONTINUE' },
  UNAUTHENTICATED: { status: 401, message: messages.errors.UNAUTHENTICATED, legacyCode: 'E006', severity: 'ERROR', action: 'RETURN' },
  ACCESS_DENIED: { status: 403, message: messages.errors.ACCESS_DENIED, legacyCode: 'E006', severity: 'ERROR', action: 'RETURN' },
  AUTH_CHECK_FAILED: { status: 503, message: messages.errors.AUTH_CHECK_FAILED, legacyCode: 'E006', severity: 'ERROR', action: 'RETURN' },
  AUDIT_FAILED: { status: 503, message: messages.errors.AUDIT_FAILED, legacyCode: 'E007', severity: 'ERROR', action: 'RETURN' },
  SYSTEM_ERROR: { status: 500, message: messages.errors.SYSTEM_ERROR, legacyCode: 'E007', severity: 'FATAL', action: 'ABEND' },
};

const KNOWN_CODES = new Set(Object.keys(ERROR_CATALOGUE));

function isApiErrorBody(value: unknown): value is Partial<ApiError> & { code: ApiErrorCode } {
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as { code?: unknown }).code === 'string' &&
    KNOWN_CODES.has((value as { code: string }).code)
  );
}

function codeForStatus(status: number): ApiErrorCode {
  if (status === 401) return 'UNAUTHENTICATED';
  if (status === 403) return 'ACCESS_DENIED';
  if (status === 400) return 'INVALID_ACCOUNT_FORMAT';
  return 'SYSTEM_ERROR';
}

/**
 * Builds a complete ApiError from whatever the server sent. Missing fields fall back to the
 * section 7.3 catalogue so the UI always has a message, severity and action to act on.
 */
export function normaliseApiError(status: number, body: unknown, traceIdHeader: string | null): ApiError {
  const code = isApiErrorBody(body) ? body.code : codeForStatus(status);
  const defaults = ERROR_CATALOGUE[code];
  const partial: Partial<ApiError> = isApiErrorBody(body) ? body : {};
  // Unknown non-JSON failures are shown inline rather than as a full-page abend.
  const fallbackAction: ErrorAction = isApiErrorBody(body) ? defaults.action : code === 'SYSTEM_ERROR' ? 'CONTINUE' : defaults.action;
  return {
    code,
    message: partial.message || defaults.message,
    severity: partial.severity ?? defaults.severity,
    action: partial.action ?? fallbackAction,
    traceId: partial.traceId || traceIdHeader || '',
    legacyCode: partial.legacyCode ?? defaults.legacyCode,
    field: partial.field,
    timestamp: partial.timestamp ?? new Date().toISOString(),
  };
}
