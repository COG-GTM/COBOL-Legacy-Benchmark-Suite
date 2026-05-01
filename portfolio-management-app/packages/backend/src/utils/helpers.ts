import { v4 as uuidv4 } from 'uuid';

// Generate transaction ID in format: YYYYMMDDHHMMSS + 6-digit sequence
// (from DB2 notes: TRANSACTION_ID format)
export function generateTransactionId(): string {
  const now = new Date();
  const datePart = now.toISOString().replace(/[-T:Z.]/g, '').substring(0, 14);
  const seq = uuidv4().replace(/-/g, '').substring(0, 6);
  return (datePart + seq).substring(0, 20);
}

// Format date as YYYYMMDD (COBOL date format)
export function formatDateCOBOL(date: Date): string {
  const y = date.getFullYear().toString();
  const m = (date.getMonth() + 1).toString().padStart(2, '0');
  const d = date.getDate().toString().padStart(2, '0');
  return `${y}${m}${d}`;
}

// Format time as HHMMSS (COBOL time format)
export function formatTimeCOBOL(date: Date): string {
  const h = date.getHours().toString().padStart(2, '0');
  const m = date.getMinutes().toString().padStart(2, '0');
  const s = date.getSeconds().toString().padStart(2, '0');
  return `${h}${m}${s}`;
}

// Format time as HH:MM:SS for storage
export function formatTimeSQL(date: Date): string {
  return date.toTimeString().substring(0, 8);
}

// Generate sequence number for audit logs
let auditSeqCounter = 0;
export function generateAuditSeqNo(): string {
  auditSeqCounter = (auditSeqCounter + 1) % 10000;
  return auditSeqCounter.toString().padStart(4, '0');
}

// Pagination helper
export function paginate(page: number, pageSize: number) {
  const skip = (page - 1) * pageSize;
  return { skip, take: pageSize };
}

export function paginationInfo(total: number, page: number, pageSize: number) {
  return {
    page,
    pageSize,
    totalCount: total,
    totalPages: Math.ceil(total / pageSize),
  };
}
