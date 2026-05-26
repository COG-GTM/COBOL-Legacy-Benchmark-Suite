import { getDb } from "@/db";
import { portfolios } from "@/db/schema";
import {
  writePortfolio,
  readPortfolio,
  getRedisClient,
  type PortfolioHash,
} from "./redis";
import { eq } from "drizzle-orm";

/**
 * Cache warming / sync utility.
 * Replaces the concept of VSAM file initialization and UTLVAL00 validation.
 */

function portfolioRowToHash(
  row: typeof portfolios.$inferSelect
): PortfolioHash {
  return {
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
  };
}

/**
 * Reads all portfolios from PostgreSQL and populates Redis.
 * Needed on cold start or after cache eviction.
 */
export async function warmCache(): Promise<{
  loaded: number;
  errors: number;
}> {
  const db = getDb();
  const rows = await db.select().from(portfolios);

  let loaded = 0;
  let errors = 0;

  for (const row of rows) {
    const hash = portfolioRowToHash(row);
    const status = await writePortfolio(hash);
    if (status === "00") {
      loaded++;
    } else {
      errors++;
    }
  }

  return { loaded, errors };
}

/**
 * Reads from Redis and upserts to PostgreSQL.
 * Safety net if write-through partially fails.
 */
export async function syncToDb(portfolioId: string): Promise<boolean> {
  const cached = await readPortfolio(portfolioId);
  if (!cached) {
    return false;
  }

  const db = getDb();
  await db
    .insert(portfolios)
    .values({
      portfolio_id: cached.portfolio_id,
      account_no: cached.account_no,
      client_name: cached.client_name,
      client_type: cached.client_type,
      portfolio_name: cached.portfolio_name || null,
      currency_code: cached.currency_code,
      risk_level: cached.risk_level || null,
      branch_id: cached.branch_id || null,
      total_value: cached.total_value,
      cash_balance: cached.cash_balance,
      status: cached.status,
      open_date: cached.open_date || undefined,
      close_date: cached.close_date || null,
      updated_by: cached.updated_by,
    })
    .onConflictDoUpdate({
      target: portfolios.portfolio_id,
      set: {
        account_no: cached.account_no,
        client_name: cached.client_name,
        client_type: cached.client_type,
        portfolio_name: cached.portfolio_name || null,
        currency_code: cached.currency_code,
        risk_level: cached.risk_level || null,
        branch_id: cached.branch_id || null,
        total_value: cached.total_value,
        cash_balance: cached.cash_balance,
        status: cached.status,
        updated_by: cached.updated_by,
      },
    });

  return true;
}

/**
 * Compares Redis and PostgreSQL record counts and spot-checks random records.
 * Replaces what UTLVAL00 does for VSAM/DB2 validation.
 */
export async function validateCacheConsistency(): Promise<{
  pgCount: number;
  redisCount: number;
  mismatches: string[];
}> {
  const db = getDb();
  const client = getRedisClient();

  const pgRows = await db.select().from(portfolios);
  const pgCount = pgRows.length;

  let redisCount = 0;
  const mismatches: string[] = [];

  for (const row of pgRows) {
    const cached = await readPortfolio(row.portfolio_id);
    if (cached) {
      redisCount++;
      if (cached.account_no !== row.account_no) {
        mismatches.push(
          `${row.portfolio_id}: account_no mismatch (pg=${row.account_no}, redis=${cached.account_no})`
        );
      }
      if (cached.status !== row.status) {
        mismatches.push(
          `${row.portfolio_id}: status mismatch (pg=${row.status}, redis=${cached.status})`
        );
      }
    } else {
      mismatches.push(`${row.portfolio_id}: missing from Redis`);
    }
  }

  // Check for orphaned Redis keys
  const statusSets = ["A", "C", "S"];
  const allRedisIds = new Set<string>();
  for (const s of statusSets) {
    const ids = await client.smembers(`portfolio:status:${s}`);
    ids.forEach((id) => allRedisIds.add(id));
  }

  for (const redisId of Array.from(allRedisIds)) {
    const inPg = pgRows.some((r) => r.portfolio_id === redisId);
    if (!inPg) {
      mismatches.push(`${redisId}: in Redis but not in PostgreSQL`);
    }
  }

  return { pgCount, redisCount, mismatches };
}
