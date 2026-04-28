/** Maps to AUDITLOG.cpy - AUDIT-RECORD */
export interface AuditRecord {
  timestamp: string;       // AUD-TIMESTAMP PIC X(26)
  systemId: string;        // AUD-SYSTEM-ID PIC X(8)
  userId: string;          // AUD-USER-ID PIC X(8)
  program: string;         // AUD-PROGRAM PIC X(8)
  terminal: string;        // AUD-TERMINAL PIC X(8)
  type: AuditType;         // AUD-TYPE PIC X(4)
  action: AuditAction;     // AUD-ACTION PIC X(8)
  status: AuditStatus;     // AUD-STATUS PIC X(4)
  portfolioId: string;     // AUD-PORTFOLIO-ID PIC X(8)
  accountNumber: string;   // AUD-ACCOUNT-NO PIC X(10)
  beforeImage: string;     // AUD-BEFORE-IMAGE PIC X(100)
  afterImage: string;      // AUD-AFTER-IMAGE PIC X(100)
  message: string;         // AUD-MESSAGE PIC X(100)
}

export type AuditType = 'TRAN' | 'USER' | 'SYST';
export const AUDIT_TYPE_LABELS: Record<AuditType, string> = {
  TRAN: 'Transaction',
  USER: 'User Action',
  SYST: 'System Event',
};

export type AuditAction = 'CREATE' | 'UPDATE' | 'DELETE' | 'INQUIRE' | 'LOGIN' | 'LOGOUT' | 'STARTUP' | 'SHUTDOWN';

export type AuditStatus = 'SUCC' | 'FAIL' | 'WARN';
export const AUDIT_STATUS_LABELS: Record<AuditStatus, string> = {
  SUCC: 'Success',
  FAIL: 'Failure',
  WARN: 'Warning',
};
