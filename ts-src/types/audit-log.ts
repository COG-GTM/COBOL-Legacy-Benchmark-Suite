/**
 * Audit Trail Record types.
 * Migrated from: src/copybook/common/AUDITLOG.cpy
 *
 * Records system and user audit events for compliance and debugging.
 */

/** Audit event type (level-88). */
export enum AuditType {
  Transaction = 'TRAN',
  User = 'USER',
  System = 'SYST',
}

/** Audit action (level-88). */
export enum AuditAction {
  Create = 'CREATE  ',
  Update = 'UPDATE  ',
  Delete = 'DELETE  ',
  Inquire = 'INQUIRE ',
  Login = 'LOGIN   ',
  Logout = 'LOGOUT  ',
  Startup = 'STARTUP ',
  Shutdown = 'SHUTDOWN',
}

/** Audit result status (level-88). */
export enum AuditStatus {
  Success = 'SUCC',
  Failure = 'FAIL',
  Warning = 'WARN',
}

/** Audit record header (system context). */
export interface AuditHeader {
  /** PIC X(8) – System / subsystem identifier. */
  audSystemId: string;
  /** PIC X(8) – User who triggered the event. */
  audUserId: string;
  /** PIC X(8) – Executing program. */
  audProgram: string;
  /** PIC X(8) – Terminal or session ID. */
  audTerminal: string;
}

/** Key information attached to the audit event. */
export interface AuditKeyInfo {
  /** PIC X(8) – Portfolio identifier. */
  audPortfolioId: string;
  /** PIC X(10) – Account number. */
  audAccountNo: string;
}

/** Full audit trail record. */
export interface AuditRecord {
  /** PIC X(26) – ISO-style timestamp. */
  audTimestamp: string;
  audHeader: AuditHeader;
  /** PIC X(4) – TRAN/USER/SYST. */
  audType: AuditType | string;
  /** PIC X(8) – CREATE/UPDATE/DELETE/etc. */
  audAction: AuditAction | string;
  /** PIC X(4) – SUCC/FAIL/WARN. */
  audStatus: AuditStatus | string;
  audKeyInfo: AuditKeyInfo;
  /** PIC X(100) – Record state before the event. */
  audBeforeImage: string;
  /** PIC X(100) – Record state after the event. */
  audAfterImage: string;
  /** PIC X(100) – Free-text message. */
  audMessage: string;
}
