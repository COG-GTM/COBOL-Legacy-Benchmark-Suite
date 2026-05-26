import Redis from "ioredis";

const getRedisUrl = (): string => {
  const url = process.env.REDIS_URL;
  if (!url) {
    throw new Error("REDIS_URL environment variable is not set");
  }
  return url;
};

let redis: Redis | null = null;

export function getRedisClient(): Redis {
  if (!redis) {
    redis = new Redis(getRedisUrl(), { maxRetriesPerRequest: 3 });
  }
  return redis;
}

export type VsamStatus = "00" | "22" | "23";

export interface PortfolioHash {
  portfolio_id: string;
  account_no: string;
  client_name: string;
  client_type: string;
  portfolio_name: string;
  currency_code: string;
  risk_level: string;
  branch_id: string;
  total_value: string;
  cash_balance: string;
  status: string;
  open_date: string;
  close_date: string;
  updated_by: string;
  created_at: string;
  updated_at: string;
}

function portfolioKey(id: string): string {
  return `portfolio:${id}`;
}

function statusSetKey(status: string): string {
  return `portfolio:status:${status}`;
}

function accountIndexKey(accountNo: string): string {
  return `portfolio:acct:${accountNo}`;
}

/**
 * Mirrors WRITE PORTFOLIO-RECORD (PORTMSTR.cbl line 126)
 * Writes a new portfolio hash to Redis with secondary indexes.
 * Returns '22' if the key already exists (VSAM duplicate key).
 */
export async function writePortfolio(
  data: PortfolioHash
): Promise<VsamStatus> {
  const client = getRedisClient();
  const key = portfolioKey(data.portfolio_id);

  const exists = await client.exists(key);
  if (exists) {
    return "22";
  }

  const acctKey = accountIndexKey(data.account_no);
  const existingAcct = await client.get(acctKey);
  if (existingAcct) {
    return "22";
  }

  const pipeline = client.pipeline();
  pipeline.hset(key, data as unknown as Record<string, string>);
  pipeline.sadd(statusSetKey(data.status), data.portfolio_id);
  pipeline.set(acctKey, data.portfolio_id);
  await pipeline.exec();

  return "00";
}

/**
 * Mirrors READ PORTFOLIO-FILE (PORTMSTR.cbl line 169)
 * Reads a portfolio hash from Redis by key.
 * Returns null if not found (VSAM status '23').
 */
export async function readPortfolio(
  portfolioId: string
): Promise<PortfolioHash | null> {
  const client = getRedisClient();
  const data = await client.hgetall(portfolioKey(portfolioId));

  if (!data || Object.keys(data).length === 0) {
    return null;
  }

  return data as unknown as PortfolioHash;
}

/**
 * Mirrors REWRITE PORT-RECORD (PORTMSTR.cbl line 194)
 * Updates an existing portfolio hash. Returns '23' if not found.
 */
export async function rewritePortfolio(
  portfolioId: string,
  data: PortfolioHash,
  oldStatus?: string
): Promise<VsamStatus> {
  const client = getRedisClient();
  const key = portfolioKey(portfolioId);

  const exists = await client.exists(key);
  if (!exists) {
    return "23";
  }

  const pipeline = client.pipeline();
  pipeline.hset(key, data as unknown as Record<string, string>);

  if (oldStatus && oldStatus !== data.status) {
    pipeline.srem(statusSetKey(oldStatus), portfolioId);
    pipeline.sadd(statusSetKey(data.status), portfolioId);
  }

  await pipeline.exec();

  return "00";
}

/**
 * Mirrors DELETE PORTFOLIO-FILE (PORTMSTR.cbl line 215)
 * Deletes a portfolio and cleans up all indexes.
 * Returns '23' if not found.
 */
export async function deletePortfolioFromCache(
  portfolioId: string
): Promise<VsamStatus> {
  const client = getRedisClient();
  const key = portfolioKey(portfolioId);

  const data = await client.hgetall(key);
  if (!data || Object.keys(data).length === 0) {
    return "23";
  }

  const pipeline = client.pipeline();
  pipeline.del(key);
  if (data.status) {
    pipeline.srem(statusSetKey(data.status), portfolioId);
  }
  if (data.account_no) {
    pipeline.del(accountIndexKey(data.account_no));
  }
  await pipeline.exec();

  return "00";
}

/**
 * Get all portfolio IDs for a given status from the status index set.
 */
export async function getPortfolioIdsByStatus(
  status: string
): Promise<string[]> {
  const client = getRedisClient();
  return client.smembers(statusSetKey(status));
}

/**
 * Look up portfolio_id by account number.
 */
export async function getPortfolioByAccount(
  accountNo: string
): Promise<string | null> {
  const client = getRedisClient();
  return client.get(accountIndexKey(accountNo));
}
