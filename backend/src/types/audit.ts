/**
 * Audit logging types derived from AUDITLOG.cpy COBOL copybook.
 * Maps mainframe audit record structure to TypeScript interfaces.
 */

/** Audit event type — maps to AUD-TYPE PIC X(4) */
export enum AuditType {
  TRANSACTION = 'TRAN',
  USER_ACTION = 'USER',
  SYSTEM_EVENT = 'SYST',
}

/** Audit action — maps to AUD-ACTION PIC X(8) */
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

/** Audit status — maps to AUD-STATUS PIC X(4) */
export enum AuditStatus {
  SUCCESS = 'SUCC',
  FAILURE = 'FAIL',
  WARNING = 'WARN',
}

/** Maps HTTP methods to COBOL audit actions */
export const HTTP_METHOD_TO_ACTION: Record<string, AuditAction> = {
  GET: AuditAction.INQUIRE,
  HEAD: AuditAction.INQUIRE,
  POST: AuditAction.CREATE,
  PUT: AuditAction.UPDATE,
  PATCH: AuditAction.UPDATE,
  DELETE: AuditAction.DELETE,
};

/** Auth endpoint patterns that map to special actions */
export const AUTH_ACTION_PATTERNS: Array<{ pattern: RegExp; action: AuditAction }> = [
  { pattern: /\/auth\/login\b/i, action: AuditAction.LOGIN },
  { pattern: /\/auth\/logout\b/i, action: AuditAction.LOGOUT },
];

/** Maximum size in bytes for before/after image fields */
export const MAX_IMAGE_SIZE = 10240;

/** Default system identifier */
export const SYSTEM_ID = 'PORTFOLIO-API';

/**
 * Audit log entry interface — maps to AUDIT-RECORD in AUDITLOG.cpy.
 *
 * COBOL field mapping:
 *   AUD-TIMESTAMP     -> timestamp
 *   AUD-SYSTEM-ID     -> systemId
 *   AUD-USER-ID       -> userId
 *   AUD-PROGRAM       -> resource
 *   AUD-TERMINAL      -> terminal
 *   AUD-TYPE           -> type
 *   AUD-ACTION         -> action
 *   AUD-STATUS         -> status
 *   AUD-PORTFOLIO-ID   -> portfolioId
 *   AUD-ACCOUNT-NO     -> accountNo
 *   AUD-BEFORE-IMAGE   -> beforeImage
 *   AUD-AFTER-IMAGE    -> afterImage
 *   AUD-MESSAGE        -> message
 */
export interface AuditLogEntry {
  id: string;
  timestamp: string;
  systemId: string;
  userId: string;
  resource: string;
  terminal: string;
  type: AuditType;
  action: AuditAction;
  status: AuditStatus;
  portfolioId?: string;
  accountNo?: string;
  beforeImage?: string;
  afterImage?: string;
  message?: string;
}

/** Query filters for audit log retrieval */
export interface AuditLogQuery {
  userId?: string;
  action?: AuditAction;
  type?: AuditType;
  status?: AuditStatus;
  portfolioId?: string;
  startDate?: string;
  endDate?: string;
  limit?: number;
  offset?: number;
}

/** Paginated result wrapper */
export interface PaginatedResult<T> {
  data: T[];
  total: number;
  limit: number;
  offset: number;
}
