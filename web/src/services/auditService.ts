import type {
  AuditAction,
  AuditRecord,
  AuditStatus,
  AuditType,
} from '../types/audit';
import { SYSTEM_ID } from '../config/session';

/**
 * Audit logging service.
 *
 * Front-end stand-in for the AUDITLOG table writes performed by SECMGR
 * (P300-LOG-ACCESS in src/programs/online/SECMGR.cbl). Records are held in
 * memory and broadcast to subscribers so a future audit-log viewer (FR-11 /
 * US-7) can render them. A real implementation would POST these to the backend.
 */

const TERMINAL = 'WEB00001';

type Listener = (records: readonly AuditRecord[]) => void;

const records: AuditRecord[] = [];
const listeners = new Set<Listener>();

function notify(): void {
  const snapshot = getAuditRecords();
  for (const listener of listeners) listener(snapshot);
}

export interface AuditEventInput {
  userId: string;
  action: AuditAction;
  status: AuditStatus;
  message: string;
  program: string;
  type?: AuditType;
}

/**
 * Append an audit record. Mirrors building and inserting an AUDIT-RECORD.
 * Returns the created record.
 */
export function recordAuditEvent(input: AuditEventInput): AuditRecord {
  const record: AuditRecord = {
    timestamp: new Date().toISOString(),
    systemId: SYSTEM_ID,
    userId: input.userId || 'ANON',
    program: input.program,
    terminal: TERMINAL,
    type: input.type ?? 'USER',
    action: input.action,
    status: input.status,
    message: input.message,
  };
  records.push(record);
  notify();
  return record;
}

/** Returns an immutable snapshot of all audit records (most recent last). */
export function getAuditRecords(): readonly AuditRecord[] {
  return records.slice();
}

/** Subscribe to audit log changes. Returns an unsubscribe function. */
export function subscribeToAudit(listener: Listener): () => void {
  listeners.add(listener);
  listener(getAuditRecords());
  return () => {
    listeners.delete(listener);
  };
}

/** Clears the in-memory audit log. Intended for tests. */
export function __resetAuditLog(): void {
  records.length = 0;
  notify();
}
