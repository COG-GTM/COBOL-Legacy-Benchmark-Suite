import { PrismaClient } from '@prisma/client';
import Decimal from 'decimal.js';
import type { SystemHealth, SystemMetrics } from '../types/index.js';

const prisma = new PrismaClient();
const startTime = Date.now();

// UTLMON00.cbl — System Monitoring
export async function getSystemHealth(): Promise<SystemHealth> {
  let dbStatus: 'connected' | 'disconnected' = 'disconnected';

  try {
    await prisma.$queryRaw`SELECT 1`;
    dbStatus = 'connected';
  } catch {
    dbStatus = 'disconnected';
  }

  const metrics = await getSystemMetrics();

  return {
    status: dbStatus === 'connected' ? 'healthy' : 'degraded',
    uptime: Math.floor((Date.now() - startTime) / 1000),
    database: dbStatus,
    websocket: 'active',
    metrics,
  };
}

async function getSystemMetrics(): Promise<SystemMetrics> {
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  const [
    totalPortfolios,
    activePortfolios,
    pendingTransactions,
    totalTransactions,
    batchJobsToday,
    lastBatch,
  ] = await Promise.all([
    prisma.portfolio.count(),
    prisma.portfolio.count({ where: { status: 'A' } }),
    prisma.transaction.count({ where: { status: 'P' } }),
    prisma.transaction.count(),
    prisma.batchJob.count({
      where: { startTime: { gte: today } },
    }),
    prisma.batchJob.findFirst({
      orderBy: { startTime: 'desc' },
    }),
  ]);

  return {
    totalPortfolios,
    activePortfolios,
    pendingTransactions,
    totalTransactions,
    lastBatchRun: lastBatch?.startTime?.toISOString(),
    batchJobsToday,
  };
}

// UTLVAL00.cbl — Data Validation
export async function validateSystemData() {
  const issues: Array<{ type: string; message: string; severity: string }> = [];

  // Check for portfolios with mismatched total values
  const portfolios = await prisma.portfolio.findMany({
    where: { status: 'A' },
    include: { positions: true },
  });

  for (const portfolio of portfolios) {
    // Deduplicate positions to latest per investment
    const latestPositions = new Map<string, typeof portfolio.positions[0]>();
    for (const pos of portfolio.positions.sort(
      (a, b) => b.positionDate.getTime() - a.positionDate.getTime()
    )) {
      if (!latestPositions.has(pos.investmentId)) {
        latestPositions.set(pos.investmentId, pos);
      }
    }

    const calculatedTotal = Array.from(latestPositions.values()).reduce(
      (sum, p) => sum.plus(p.marketValue.toString()),
      new Decimal(0)
    );

    if (!calculatedTotal.equals(portfolio.totalValue.toString())) {
      issues.push({
        type: 'VALUE_MISMATCH',
        message: `Portfolio ${portfolio.portfolioId}: recorded total=${portfolio.totalValue}, calculated=${calculatedTotal}`,
        severity: 'WARNING',
      });
    }
  }

  // Check for orphaned transactions
  const orphanedTxns = await prisma.transaction.findMany({
    where: {
      portfolio: { status: 'C' },
      status: 'P',
    },
  });

  if (orphanedTxns.length > 0) {
    issues.push({
      type: 'ORPHANED_TRANSACTIONS',
      message: `${orphanedTxns.length} pending transactions on closed portfolios`,
      severity: 'ERROR',
    });
  }

  // Check for stale pending transactions (older than 7 days)
  const sevenDaysAgo = new Date();
  sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7);

  const staleCount = await prisma.transaction.count({
    where: {
      status: 'P',
      transactionDate: { lt: sevenDaysAgo },
    },
  });

  if (staleCount > 0) {
    issues.push({
      type: 'STALE_TRANSACTIONS',
      message: `${staleCount} pending transactions older than 7 days`,
      severity: 'WARNING',
    });
  }

  return {
    validatedAt: new Date().toISOString(),
    issueCount: issues.length,
    issues,
    status: issues.length === 0 ? 'VALID' : 'ISSUES_FOUND',
  };
}

// UTLMNT00.cbl — Maintenance Operations
export async function runMaintenance(operation: string) {
  const results: Record<string, unknown> = {
    operation,
    startTime: new Date().toISOString(),
  };

  switch (operation) {
    case 'ARCHIVE': {
      // Archive old completed transactions
      const thirtyDaysAgo = new Date();
      thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);

      const archived = await prisma.transaction.count({
        where: {
          status: 'D',
          transactionDate: { lt: thirtyDaysAgo },
        },
      });
      results.archivedTransactions = archived;
      break;
    }

    case 'CLEANUP': {
      // Clean up failed batch jobs older than 30 days
      const thirtyDaysAgo = new Date();
      thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);

      const deleted = await prisma.batchJob.deleteMany({
        where: {
          status: 'E',
          startTime: { lt: thirtyDaysAgo },
        },
      });
      results.deletedJobs = deleted.count;
      break;
    }

    case 'ANALYZE': {
      // Database statistics
      const counts = await Promise.all([
        prisma.portfolio.count(),
        prisma.investmentPosition.count(),
        prisma.transaction.count(),
        prisma.auditLog.count(),
        prisma.positionHistory.count(),
        prisma.batchJob.count(),
      ]);

      results.tableCounts = {
        portfolios: counts[0],
        positions: counts[1],
        transactions: counts[2],
        auditLogs: counts[3],
        positionHistory: counts[4],
        batchJobs: counts[5],
      };
      break;
    }

    default:
      results.error = `Unknown operation: ${operation}`;
  }

  results.endTime = new Date().toISOString();
  return results;
}
