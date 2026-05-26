import { getDb } from "@/db";
import { auditLog } from "@/db/schema";

/**
 * Audit helper — replaces CALL 'AUDPROC' in PORTMSTR.cbl line 287
 * and the audit file write in PORTDEL.cbl line 177.
 *
 * Parameters match AUDITLOG.cpy structure.
 */
export interface AuditParams {
  userId: string;
  programId?: string;
  eventType?: "TRAN" | "USER" | "SYST";
  action: string;
  status: "SUCC" | "FAIL" | "WARN";
  portfolioId?: string;
  accountNo?: string;
  beforeImage?: Record<string, unknown> | null;
  afterImage?: Record<string, unknown> | null;
  message?: string;
}

export async function logAudit(params: AuditParams): Promise<void> {
  const db = getDb();

  await db.insert(auditLog).values({
    user_id: params.userId,
    program_id: params.programId ?? "NEXTJS",
    event_type: params.eventType ?? "TRAN",
    action: params.action,
    status: params.status,
    portfolio_id: params.portfolioId ?? null,
    account_no: params.accountNo ?? null,
    before_image: params.beforeImage ?? null,
    after_image: params.afterImage ?? null,
    message: params.message ?? null,
  });
}
