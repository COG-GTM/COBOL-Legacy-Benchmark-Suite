/**
 * Audit trail types.
 *
 * Mirrors the AUDIT-RECORD structure defined in
 * src/copybook/common/AUDITLOG.cpy. Field names map to the copybook 88-level
 * condition values so the mock front-end produces records analogous to the
 * security events SECMGR writes to the AUDITLOG table.
 */

/** Maps to AUD-ACTION 88-level values in AUDITLOG.cpy. */
export type AuditAction =
  | 'LOGIN'
  | 'LOGOUT'
  | 'CREATE'
  | 'UPDATE'
  | 'DELETE'
  | 'INQUIRE'
  | 'STARTUP'
  | 'SHUTDOWN';

/** Maps to AUD-STATUS 88-level values (AUD-SUCCESS / AUD-FAILURE / AUD-WARNING). */
export type AuditStatus = 'SUCC' | 'FAIL' | 'WARN';

/** Maps to AUD-TYPE 88-level values. */
export type AuditType = 'TRAN' | 'USER' | 'SYST';

/**
 * A single audit trail entry. Mirrors AUDIT-RECORD / AUD-HEADER in
 * AUDITLOG.cpy. Free-text COBOL fixed-length fields are represented as strings.
 */
export interface AuditRecord {
  /** AUD-TIMESTAMP PIC X(26) — ISO 8601 timestamp here. */
  timestamp: string;
  /** AUD-SYSTEM-ID PIC X(8). */
  systemId: string;
  /** AUD-USER-ID PIC X(8). */
  userId: string;
  /** AUD-PROGRAM PIC X(8) — originating program/component. */
  program: string;
  /** AUD-TERMINAL PIC X(8). */
  terminal: string;
  /** AUD-TYPE. */
  type: AuditType;
  /** AUD-ACTION. */
  action: AuditAction;
  /** AUD-STATUS. */
  status: AuditStatus;
  /** AUD-MESSAGE PIC X(100). */
  message: string;
}
