"use server";

import { revalidatePath } from "next/cache";
import { getDb } from "@/db";
import { portfolios } from "@/db/schema";
import {
  writePortfolio,
  readPortfolio,
  rewritePortfolio,
  deletePortfolioFromCache,
  getPortfolioIdsByStatus,
  getPortfolioByAccount,
  type PortfolioHash,
} from "@/lib/redis";
import {
  createPortfolioSchema,
  updatePortfolioSchema,
  deletePortfolioSchema,
} from "@/lib/validations/portfolio";
import { logAudit } from "@/lib/audit";
import { mapDbError, REASON_CODE_LABELS } from "@/lib/errors";
import { eq, and, like, sql } from "drizzle-orm";

export type ActionResult<T = unknown> = {
  success: boolean;
  data?: T;
  error?: string;
};

function toHash(
  input: Record<string, string>,
  now: string
): PortfolioHash {
  return {
    portfolio_id: input.portfolio_id,
    account_no: input.account_no,
    client_name: input.client_name,
    client_type: input.client_type,
    portfolio_name: input.portfolio_name ?? "",
    currency_code: input.currency_code ?? "USD",
    risk_level: input.risk_level ?? "",
    branch_id: input.branch_id ?? "",
    total_value: input.total_value ?? "0",
    cash_balance: input.cash_balance ?? "0",
    status: input.status ?? "A",
    open_date: now,
    close_date: "",
    updated_by: input.updated_by,
    created_at: now,
    updated_at: now,
  };
}

/**
 * Create a new portfolio.
 * Ports: PORTMSTR.cbl 2000-CREATE-PORTFOLIO + PORTADD.cbl 2100-VALIDATE-AND-ADD
 */
export async function createPortfolio(
  formData: FormData
): Promise<ActionResult<PortfolioHash>> {
  const raw = Object.fromEntries(formData.entries()) as Record<string, string>;
  const parsed = createPortfolioSchema.safeParse(raw);

  if (!parsed.success) {
    return {
      success: false,
      error: parsed.error.errors.map((e) => e.message).join("; "),
    };
  }

  const input = parsed.data;
  const now = new Date().toISOString();
  const hash = toHash(input as unknown as Record<string, string>, now);

  // Check account_no uniqueness via Redis index
  const existingAcct = await getPortfolioByAccount(input.account_no);
  if (existingAcct) {
    return {
      success: false,
      error: "Account number already in use",
    };
  }

  // Write to Redis (VSAM replacement)
  const vsamStatus = await writePortfolio(hash);
  if (vsamStatus === "22") {
    return {
      success: false,
      error: "Portfolio ID already exists",
    };
  }

  // Write to PostgreSQL (DB2 replacement)
  try {
    const db = getDb();
    await db.insert(portfolios).values({
      portfolio_id: input.portfolio_id,
      account_no: input.account_no,
      client_name: input.client_name,
      client_type: input.client_type,
      portfolio_name: input.portfolio_name || null,
      currency_code: input.currency_code,
      risk_level: input.risk_level || null,
      branch_id: input.branch_id || null,
      total_value: input.total_value,
      cash_balance: input.cash_balance,
      status: input.status,
      updated_by: input.updated_by,
    });

    // Audit log
    await logAudit({
      userId: input.updated_by,
      action: "CREATE",
      status: "SUCC",
      portfolioId: input.portfolio_id,
      accountNo: input.account_no,
      afterImage: hash as unknown as Record<string, unknown>,
    });
  } catch (err) {
    // Roll back Redis on PG failure
    await deletePortfolioFromCache(input.portfolio_id);
    const pgErr = mapDbError(err);
    return { success: false, error: pgErr.message };
  }

  revalidatePath("/portfolios");
  return { success: true, data: hash };
}

/**
 * Get a single portfolio by ID.
 * Ports: PORTMSTR.cbl 3000-READ-PORTFOLIO
 */
export async function getPortfolio(
  portfolioId: string
): Promise<ActionResult<PortfolioHash>> {
  if (!portfolioId || !/^PORT\d{4}$/.test(portfolioId)) {
    return { success: false, error: "Invalid Portfolio ID format" };
  }

  const data = await readPortfolio(portfolioId);
  if (!data) {
    return { success: false, error: "Portfolio not found" };
  }

  return { success: true, data };
}

/**
 * List portfolios with optional filters.
 * Ports: PORTREAD.cbl 2000-PROCESS — sequential browse of all records.
 */
export async function listPortfolios(filters?: {
  status?: string;
  clientType?: string;
  search?: string;
  page?: number;
  pageSize?: number;
}): Promise<
  ActionResult<{
    items: PortfolioHash[];
    total: number;
    page: number;
    pageSize: number;
  }>
> {
  const page = filters?.page ?? 1;
  const pageSize = filters?.pageSize ?? 20;
  const offset = (page - 1) * pageSize;

  // For simple status filter, use Redis sets for fast lookup
  if (
    filters?.status &&
    !filters.clientType &&
    !filters.search
  ) {
    const ids = await getPortfolioIdsByStatus(filters.status);
    const total = ids.length;
    const pageIds = ids.slice(offset, offset + pageSize);
    const items: PortfolioHash[] = [];

    for (const id of pageIds) {
      const data = await readPortfolio(id);
      if (data) items.push(data);
    }

    return {
      success: true,
      data: { items, total, page, pageSize },
    };
  }

  // Complex filters → query PostgreSQL (DB2 reporting path)
  const db = getDb();
  const conditions = [];

  if (filters?.status) {
    conditions.push(eq(portfolios.status, filters.status));
  }
  if (filters?.clientType) {
    conditions.push(eq(portfolios.client_type, filters.clientType));
  }
  if (filters?.search) {
    conditions.push(
      like(portfolios.client_name, `%${filters.search}%`)
    );
  }

  const where = conditions.length > 0 ? and(...conditions) : undefined;

  const [countResult, rows] = await Promise.all([
    db
      .select({ count: sql<number>`count(*)` })
      .from(portfolios)
      .where(where),
    db
      .select()
      .from(portfolios)
      .where(where)
      .limit(pageSize)
      .offset(offset),
  ]);

  const total = Number(countResult[0]?.count ?? 0);
  const items: PortfolioHash[] = rows.map((row) => ({
    portfolio_id: row.portfolio_id,
    account_no: row.account_no,
    client_name: row.client_name,
    client_type: row.client_type,
    portfolio_name: row.portfolio_name ?? "",
    currency_code: row.currency_code,
    risk_level: row.risk_level ?? "",
    branch_id: row.branch_id ?? "",
    total_value: row.total_value ?? "0",
    cash_balance: row.cash_balance ?? "0",
    status: row.status,
    open_date: row.open_date ?? "",
    close_date: row.close_date ?? "",
    updated_by: row.updated_by,
    created_at: row.created_at?.toISOString() ?? "",
    updated_at: row.updated_at?.toISOString() ?? "",
  }));

  return {
    success: true,
    data: { items, total, page, pageSize },
  };
}

/**
 * Update an existing portfolio.
 * Ports: PORTMSTR.cbl 4000-UPDATE-PORTFOLIO + PORTUPDT.cbl 2200-APPLY-UPDATE
 */
export async function updatePortfolio(
  formData: FormData
): Promise<ActionResult<PortfolioHash>> {
  const raw = Object.fromEntries(formData.entries()) as Record<string, string>;
  const parsed = updatePortfolioSchema.safeParse(raw);

  if (!parsed.success) {
    return {
      success: false,
      error: parsed.error.errors.map((e) => e.message).join("; "),
    };
  }

  const input = parsed.data;

  // Read current record from Redis (before_image)
  const before = await readPortfolio(input.portfolio_id);
  if (!before) {
    return { success: false, error: "Portfolio not found for update" };
  }

  const oldStatus = before.status;
  const now = new Date().toISOString();

  // Build updated hash
  const updated: PortfolioHash = {
    ...before,
    client_name: input.client_name ?? before.client_name,
    client_type: input.client_type ?? before.client_type,
    portfolio_name: input.portfolio_name ?? before.portfolio_name,
    currency_code: input.currency_code ?? before.currency_code,
    risk_level: input.risk_level ?? before.risk_level,
    branch_id: input.branch_id ?? before.branch_id,
    total_value: input.total_value ?? before.total_value,
    cash_balance: input.cash_balance ?? before.cash_balance,
    status: input.status ?? before.status,
    close_date: input.close_date ?? before.close_date,
    updated_by: input.updated_by,
    updated_at: now,
  };

  // Write to Redis
  const vsamStatus = await rewritePortfolio(
    input.portfolio_id,
    updated,
    oldStatus
  );
  if (vsamStatus === "23") {
    return { success: false, error: "Portfolio not found for update" };
  }

  // Write to PostgreSQL
  try {
    const db = getDb();
    const updateFields: Record<string, unknown> = { updated_at: new Date() };

    if (input.client_name !== undefined)
      updateFields.client_name = input.client_name;
    if (input.client_type !== undefined)
      updateFields.client_type = input.client_type;
    if (input.portfolio_name !== undefined)
      updateFields.portfolio_name = input.portfolio_name;
    if (input.currency_code !== undefined)
      updateFields.currency_code = input.currency_code;
    if (input.risk_level !== undefined)
      updateFields.risk_level = input.risk_level;
    if (input.branch_id !== undefined) updateFields.branch_id = input.branch_id;
    if (input.total_value !== undefined)
      updateFields.total_value = input.total_value;
    if (input.cash_balance !== undefined)
      updateFields.cash_balance = input.cash_balance;
    if (input.status !== undefined) updateFields.status = input.status;
    if (input.close_date !== undefined)
      updateFields.close_date = input.close_date;
    updateFields.updated_by = input.updated_by;

    await db
      .update(portfolios)
      .set(updateFields)
      .where(eq(portfolios.portfolio_id, input.portfolio_id));

    await logAudit({
      userId: input.updated_by,
      action: "UPDATE",
      status: "SUCC",
      portfolioId: input.portfolio_id,
      accountNo: before.account_no,
      beforeImage: before as unknown as Record<string, unknown>,
      afterImage: updated as unknown as Record<string, unknown>,
    });
  } catch (err) {
    // Rollback Redis to before state
    await rewritePortfolio(input.portfolio_id, before, updated.status);
    const pgErr = mapDbError(err);
    return { success: false, error: pgErr.message };
  }

  revalidatePath("/portfolios");
  revalidatePath(`/portfolios/${input.portfolio_id}`);
  return { success: true, data: updated };
}

/**
 * Delete a portfolio with reason code.
 * Ports: PORTDEL.cbl 2100-PROCESS-DELETE and 2200-DELETE-RECORD
 */
export async function deletePortfolio(
  portfolioId: string,
  reasonCode: string
): Promise<ActionResult> {
  const parsed = deletePortfolioSchema.safeParse({
    portfolio_id: portfolioId,
    reason_code: reasonCode,
  });

  if (!parsed.success) {
    return {
      success: false,
      error: parsed.error.errors.map((e) => e.message).join("; "),
    };
  }

  // Read before_image for audit (PORTDEL.cbl line 142)
  const before = await readPortfolio(portfolioId);
  if (!before) {
    return { success: false, error: "Portfolio not found for deletion" };
  }

  // Delete from Redis
  const vsamStatus = await deletePortfolioFromCache(portfolioId);
  if (vsamStatus === "23") {
    return { success: false, error: "Portfolio not found for deletion" };
  }

  // Delete from PostgreSQL and write audit
  try {
    const db = getDb();
    await db
      .delete(portfolios)
      .where(eq(portfolios.portfolio_id, portfolioId));

    const reasonLabel =
      REASON_CODE_LABELS[reasonCode] ?? `Code ${reasonCode}`;
    await logAudit({
      userId: before.updated_by,
      action: "DELETE",
      status: "SUCC",
      portfolioId,
      accountNo: before.account_no,
      beforeImage: before as unknown as Record<string, unknown>,
      message: `Reason: ${reasonCode} - ${reasonLabel}`,
    });
  } catch (err) {
    // Re-insert to Redis on PG failure
    await writePortfolio(before);
    const pgErr = mapDbError(err);
    return { success: false, error: pgErr.message };
  }

  revalidatePath("/portfolios");
  return { success: true };
}
