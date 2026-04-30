/**
 * Audit record interface derived from src/copybook/common/AUDITLOG.cpy
 * and PORTDEL audit write (src/programs/portfolio/PORTDEL.cbl)
 */

export type AuditAction = 'CREATE' | 'UPDATE' | 'DELETE' | 'INQUIRE' | 'LOGIN' | 'LOGOUT';
export type AuditStatus = 'SUCC' | 'FAIL' | 'WARN';

export interface AuditRecord {
  timestamp: string;
  action: AuditAction;
  key: string;
  reason: string;
  status: AuditStatus;
}
