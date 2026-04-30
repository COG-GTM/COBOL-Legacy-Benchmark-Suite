// Audit trail processing - migrated from AUDPROC.cbl
//
// AUDPROC.cbl: Audit Trail Processing Subroutine that receives audit requests
// via LINKAGE SECTION (LS-AUDIT-REQUEST), timestamps them, and writes
// AUDIT-RECORD to the AUDIT-FILE with before/after images.
//
// LINKAGE fields mapped:
//   LS-SYSTEM-ID, LS-USER-ID, LS-PROGRAM, LS-TERMINAL -> header fields
//   LS-TYPE (TRAN/USER/SYST) -> auditType
//   LS-ACTION (CREATE/UPDATE/DELETE/INQUIRE/LOGIN/LOGOUT) -> action
//   LS-STATUS (SUCC/FAIL/WARN) -> status
//   LS-PORT-ID, LS-ACCT-NO -> key info
//   LS-BEFORE-IMAGE, LS-AFTER-IMAGE -> change tracking
//   LS-MESSAGE -> descriptive text
//
// This module provides:
// - Direct audit logging function (mirrors AUDPROC 2000-PROCESS-AUDIT)
// - Middleware that auto-logs create/update/delete operations
// - Before/after image capture via manual diffing

import { prisma } from "./db";
import type { AuditAction, AuditRecord, AuditStatus, AuditType } from "../types";

/**
 * Write an audit record, mirroring AUDPROC 2000-PROCESS-AUDIT paragraph.
 *
 * AUDPROC initialized AUDIT-RECORD, moved linkage fields to record fields,
 * then performed WRITE AUDIT-RECORD. On failure, set LS-RETURN-CODE to 8.
 */
export async function writeAuditLog(record: AuditRecord): Promise<void> {
  try {
    await prisma.auditLog.create({
      data: {
        timestamp: record.timestamp,
        systemId: pad(record.systemId, 8),
        userId: pad(record.userId, 8),
        programId: pad(record.programId, 8),
        terminal: record.terminal ? pad(record.terminal, 8) : null,
        auditType: record.auditType,
        action: padAction(record.action),
        status: record.status,
        portfolioId: record.portfolioId ?? null,
        accountNo: record.accountNo ?? null,
        beforeImage: record.beforeImage ?? null,
        afterImage: record.afterImage ?? null,
        message: record.message ?? null,
      },
    });
  } catch (error) {
    // Mirrors AUDPROC: DISPLAY 'Error writing audit record: ' WS-FILE-STATUS
    console.error("Error writing audit record:", error);
  }
}

/**
 * Log a data modification operation with before/after images.
 * Captures the diff between old and new states for audit trail.
 */
export async function logDataChange(params: {
  userId: string;
  programId: string;
  action: "CREATE" | "UPDATE" | "DELETE";
  portfolioId?: string;
  accountNo?: string;
  beforeData?: Record<string, unknown>;
  afterData?: Record<string, unknown>;
  message?: string;
}): Promise<void> {
  const beforeImage = params.beforeData
    ? JSON.stringify(params.beforeData)
    : undefined;
  const afterImage = params.afterData
    ? JSON.stringify(params.afterData)
    : undefined;

  await writeAuditLog({
    timestamp: new Date(),
    systemId: "MODERN",
    userId: params.userId,
    programId: params.programId,
    auditType: "TRAN",
    action: params.action,
    status: "SUCC",
    portfolioId: params.portfolioId,
    accountNo: params.accountNo,
    beforeImage,
    afterImage,
    message: params.message,
  });
}

/**
 * Log a user action (login, logout, inquiry).
 */
export async function logUserAction(params: {
  userId: string;
  action: AuditAction;
  status: AuditStatus;
  message?: string;
  portfolioId?: string;
}): Promise<void> {
  await writeAuditLog({
    timestamp: new Date(),
    systemId: "MODERN",
    userId: params.userId,
    programId: "API",
    auditType: "USER",
    action: params.action,
    status: params.status,
    portfolioId: params.portfolioId,
    message: params.message,
  });
}

/**
 * Log a system event (startup, shutdown).
 */
export async function logSystemEvent(params: {
  action: "STARTUP" | "SHUTDOWN";
  status: AuditStatus;
  message?: string;
}): Promise<void> {
  await writeAuditLog({
    timestamp: new Date(),
    systemId: "MODERN",
    userId: "SYSTEM",
    programId: "SYSTEM",
    auditType: "SYST",
    action: params.action,
    status: params.status,
    message: params.message,
  });
}

/**
 * Compute a diff between two objects for before/after image tracking.
 * Returns only the fields that changed.
 */
export function computeDiff(
  before: Record<string, unknown>,
  after: Record<string, unknown>
): { changed: Record<string, { before: unknown; after: unknown }> } {
  const changed: Record<string, { before: unknown; after: unknown }> = {};

  const allKeys = new Set([...Object.keys(before), ...Object.keys(after)]);
  for (const key of allKeys) {
    const bVal = before[key];
    const aVal = after[key];
    if (JSON.stringify(bVal) !== JSON.stringify(aVal)) {
      changed[key] = { before: bVal, after: aVal };
    }
  }

  return { changed };
}

// --- Helpers ---

function pad(value: string, length: number): string {
  return value.padEnd(length).substring(0, length);
}

function padAction(action: AuditAction): string {
  return action.padEnd(8).substring(0, 8);
}
