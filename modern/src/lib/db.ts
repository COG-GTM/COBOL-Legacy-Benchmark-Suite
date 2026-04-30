// Database connection management - migrated from DB2CONN.cbl
//
// Original COBOL: DB2CONN manages DB2 connections with retry logic (3 retries),
// connect/disconnect/status-check functions, and error handling via ERRPROC.
// Prisma handles connection pooling natively; this module preserves retry and
// health-check semantics from the mainframe implementation.

import { PrismaClient } from "@prisma/client";

const MAX_RETRIES = 3;
const RETRY_DELAY_MS = 100; // from DB2-RETRY-WAIT in DBPROC.cpy

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function createPrismaClient(): PrismaClient {
  return new PrismaClient({
    log:
      process.env.NODE_ENV === "development"
        ? ["query", "error", "warn"]
        : ["error"],
  });
}

// Singleton pattern — avoid multiple PrismaClient instances in development
const globalForPrisma = globalThis as unknown as { prisma: PrismaClient | undefined };

export const prisma = globalForPrisma.prisma ?? createPrismaClient();

if (process.env.NODE_ENV !== "production") {
  globalForPrisma.prisma = prisma;
}

/**
 * Connect with retry logic, mirroring DB2CONN 1000-CONNECT paragraph.
 * DB2CONN retried up to WS-MAX-RETRIES (3) with DB2-RETRY-WAIT (100ms) delay.
 */
export async function connectWithRetry(): Promise<void> {
  let retryCount = 0;

  while (retryCount < MAX_RETRIES) {
    try {
      await prisma.$connect();
      return;
    } catch (error) {
      retryCount++;
      if (retryCount >= MAX_RETRIES) {
        throw new DatabaseConnectionError(
          `Failed to connect after ${MAX_RETRIES} attempts`,
          error
        );
      }
      await sleep(RETRY_DELAY_MS);
    }
  }
}

/**
 * Disconnect gracefully, mirroring DB2CONN 2000-DISCONNECT paragraph.
 * DB2CONN issued COMMIT WORK then CONNECT RESET.
 */
export async function disconnect(): Promise<void> {
  await prisma.$disconnect();
}

/**
 * Health check, mirroring DB2CONN 3000-CHECK-STATUS paragraph.
 * DB2CONN ran SELECT CURRENT SERVER FROM SYSIBM.SYSDUMMY1 to verify connectivity.
 */
export async function checkDatabaseHealth(): Promise<{
  connected: boolean;
  responseTimeMs: number;
  error?: string;
}> {
  const start = Date.now();
  try {
    await prisma.$queryRaw`SELECT 1`;
    return {
      connected: true,
      responseTimeMs: Date.now() - start,
    };
  } catch (error) {
    return {
      connected: false,
      responseTimeMs: Date.now() - start,
      error: error instanceof Error ? error.message : "Unknown database error",
    };
  }
}

export class DatabaseConnectionError extends Error {
  public readonly cause: unknown;

  constructor(message: string, cause?: unknown) {
    super(message);
    this.name = "DatabaseConnectionError";
    this.cause = cause;
  }
}
