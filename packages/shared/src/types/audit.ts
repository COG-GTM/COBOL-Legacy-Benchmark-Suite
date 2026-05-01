export type AuditActionCode = 'A' | 'C' | 'D';

export interface AuditRecord {
  actionCode: AuditActionCode;
  beforeImage: string;
  afterImage: string;
  timestamp: string;
  userId: string;
  programId: string;
}
