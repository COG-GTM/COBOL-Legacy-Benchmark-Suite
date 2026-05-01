import { PrismaClient } from '@prisma/client';
import Decimal from 'decimal.js';

const prisma = new PrismaClient();

// RPTPOS00.cbl — Daily Position Report Generator
export async function getPositionReport(params: {
  startDate?: string;
  endDate?: string;
  portfolioId?: string;
}) {
  const where: Record<string, unknown> = {};

  if (params.portfolioId) {
    where.portfolioId = params.portfolioId;
  }

  if (params.startDate || params.endDate) {
    where.positionDate = {};
    if (params.startDate) {
      (where.positionDate as Record<string, Date>).gte = new Date(params.startDate);
    }
    if (params.endDate) {
      (where.positionDate as Record<string, Date>).lte = new Date(params.endDate);
    }
  }

  const positions = await prisma.investmentPosition.findMany({
    where,
    include: {
      portfolio: {
        select: {
          portfolioName: true,
          clientId: true,
          status: true,
        },
      },
    },
    orderBy: [{ portfolioId: 'asc' }, { positionDate: 'desc' }],
  });

  // Calculate summary metrics
  const summary = {
    totalPositions: positions.length,
    totalMarketValue: positions
      .reduce((sum, p) => sum.plus(p.marketValue.toString()), new Decimal(0))
      .toNumber(),
    totalCostBasis: positions
      .reduce((sum, p) => sum.plus(p.costBasis.toString()), new Decimal(0))
      .toNumber(),
    totalGainLoss: 0,
    reportDate: new Date().toISOString(),
  };
  summary.totalGainLoss = summary.totalMarketValue - summary.totalCostBasis;

  return { positions, summary };
}

// RPTAUD00.cbl — Audit Report Generator
export async function getAuditReport(params: {
  startDate?: string;
  endDate?: string;
  portfolioId?: string;
}) {
  const where: Record<string, unknown> = {};

  if (params.portfolioId) {
    where.portfolioId = params.portfolioId;
  }

  if (params.startDate || params.endDate) {
    where.date = {};
    if (params.startDate) {
      (where.date as Record<string, Date>).gte = new Date(params.startDate);
    }
    if (params.endDate) {
      (where.date as Record<string, Date>).lte = new Date(params.endDate);
    }
  }

  const auditLogs = await prisma.auditLog.findMany({
    where,
    orderBy: { processDate: 'desc' },
    take: 1000,
  });

  // Summarize by type and action
  const summary = {
    totalEntries: auditLogs.length,
    byRecordType: {} as Record<string, number>,
    byActionCode: {} as Record<string, number>,
    reportDate: new Date().toISOString(),
  };

  for (const log of auditLogs) {
    summary.byRecordType[log.recordType] = (summary.byRecordType[log.recordType] || 0) + 1;
    summary.byActionCode[log.actionCode] = (summary.byActionCode[log.actionCode] || 0) + 1;
  }

  return { auditLogs, summary };
}

// RPTSTA00.cbl — System Statistics Report Generator
export async function getStatisticsReport() {
  const [
    totalPortfolios,
    activePortfolios,
    closedPortfolios,
    totalTransactions,
    pendingTransactions,
    doneTransactions,
    failedTransactions,
    totalPositions,
    batchJobs,
    recentBatchJobs,
  ] = await Promise.all([
    prisma.portfolio.count(),
    prisma.portfolio.count({ where: { status: 'A' } }),
    prisma.portfolio.count({ where: { status: 'C' } }),
    prisma.transaction.count(),
    prisma.transaction.count({ where: { status: 'P' } }),
    prisma.transaction.count({ where: { status: 'D' } }),
    prisma.transaction.count({ where: { status: 'F' } }),
    prisma.investmentPosition.count(),
    prisma.batchJob.count(),
    prisma.batchJob.findMany({
      orderBy: { startTime: 'desc' },
      take: 10,
    }),
  ]);

  const batchSuccessCount = recentBatchJobs.filter(j => j.status === 'D').length;
  const successRate = recentBatchJobs.length > 0
    ? (batchSuccessCount / recentBatchJobs.length) * 100
    : 100;

  return {
    reportDate: new Date().toISOString(),
    portfolios: {
      total: totalPortfolios,
      active: activePortfolios,
      closed: closedPortfolios,
      suspended: totalPortfolios - activePortfolios - closedPortfolios,
    },
    transactions: {
      total: totalTransactions,
      pending: pendingTransactions,
      done: doneTransactions,
      failed: failedTransactions,
    },
    positions: {
      total: totalPositions,
    },
    batch: {
      totalJobs: batchJobs,
      recentJobs: recentBatchJobs,
      successRate,
    },
  };
}
