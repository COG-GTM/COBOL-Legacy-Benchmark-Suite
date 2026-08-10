/**
 * Domain types for the Audit Trail Record.
 *
 * These mirror the COBOL copybook `src/copybook/common/AUDITLOG.cpy`
 * (01 AUDIT-RECORD), the input file of the RPTAUD00 batch audit report.
 * Field names are translated from COBOL hyphenated names to camelCase; the
 * lengths and allowed values of the 88-levels are preserved.
 */

/** AUD-TYPE PIC X(4) — 88-level values from AUDITLOG.cpy. */
export type AuditType = 'TRAN' | 'USER' | 'SYST';

/** AUD-ACTION PIC X(8) — 88-level values from AUDITLOG.cpy (trimmed). */
export type AuditAction =
  | 'CREATE'
  | 'UPDATE'
  | 'DELETE'
  | 'INQUIRE'
  | 'LOGIN'
  | 'LOGOUT'
  | 'STARTUP'
  | 'SHUTDOWN';

/** AUD-STATUS PIC X(4) — 88-level values from AUDITLOG.cpy. */
export type AuditStatus = 'SUCC' | 'FAIL' | 'WARN';

export const AUDIT_TYPE_LABELS: Record<AuditType, string> = {
  TRAN: 'Transaction',
  USER: 'User Action',
  SYST: 'System',
};

export const AUDIT_ACTION_LABELS: Record<AuditAction, string> = {
  CREATE: 'Create',
  UPDATE: 'Update',
  DELETE: 'Delete',
  INQUIRE: 'Inquire',
  LOGIN: 'Login',
  LOGOUT: 'Logout',
  STARTUP: 'Startup',
  SHUTDOWN: 'Shutdown',
};

export const AUDIT_STATUS_LABELS: Record<AuditStatus, string> = {
  SUCC: 'Success',
  FAIL: 'Failure',
  WARN: 'Warning',
};

export const AUDIT_TYPES = Object.keys(AUDIT_TYPE_LABELS) as AuditType[];
export const AUDIT_ACTIONS = Object.keys(AUDIT_ACTION_LABELS) as AuditAction[];
export const AUDIT_STATUSES = Object.keys(AUDIT_STATUS_LABELS) as AuditStatus[];

/** A single audit trail event (01 AUDIT-RECORD). */
export interface AuditEvent {
  /** AUD-TIMESTAMP PIC X(26) — DB2 timestamp form, e.g. 2024-04-01-09.31.22.000000. */
  timestamp: string;
  /** AUD-SYSTEM-ID PIC X(8). */
  systemId: string;
  /** AUD-USER-ID PIC X(8). */
  userId: string;
  /** AUD-PROGRAM PIC X(8). */
  program: string;
  /** AUD-TERMINAL PIC X(8). */
  terminal: string;
  /** AUD-TYPE. */
  type: AuditType;
  /** AUD-ACTION (trailing spaces trimmed). */
  action: AuditAction;
  /** AUD-STATUS. */
  status: AuditStatus;
  /** AUD-PORTFOLIO-ID PIC X(8) — blank for events with no portfolio context. */
  portfolioId: string;
  /** AUD-ACCOUNT-NO PIC X(10) — blank for events with no account context. */
  accountNo: string;
  /** AUD-MESSAGE PIC X(100). */
  message: string;
}
