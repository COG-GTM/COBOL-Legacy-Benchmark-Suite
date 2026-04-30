// System Statistics Report Generator - migrated from RPTSTA00.cbl
//
// RPTSTA00.cbl generates system performance and statistics reports by:
// 1. 1000-INITIALIZE: Opening DB2-STATS, BATCH-STATS, REPORT-FILE;
//    writing headers; 1300-INIT-ACCUMULATORS initializes all metric fields.
// 2. 2000-PROCESS-REPORT:
//    - 2100-PROCESS-DB2-STATS: Read DB2 statistics records sequentially,
//      2110-ACCUMULATE-DB2-STATS: Sum WS-DB2-CALLS, WS-DB2-ELAPSED, etc.
//    - 2200-PROCESS-BATCH-STATS: Read batch job statistics sequentially,
//      2210-ACCUMULATE-BATCH-STATS: Sum jobs, success, failed counts.
//    - 2300-CALCULATE-METRICS:
//      - 2310-CALC-DB2-METRICS: Average response time = elapsed / calls
//      - 2320-CALC-BATCH-METRICS: Success rate = success / total * 100
//    - 2400-WRITE-REPORT:
//      - 2410-WRITE-DB2-SECTION: DB2 calls count, avg response
//      - 2420-WRITE-BATCH-SECTION: Batch jobs count, success rate
//      - 2430-WRITE-TREND-ANALYSIS: Historical trend data for charts
// 3. 3000-CLEANUP: Close all files
//
// Modern implementation queries aggregated data from database tables
// instead of reading VSAM statistics files.

import Decimal from "decimal.js";
import { prisma } from "../../lib/db";
import type { StatisticsMetrics } from "../../types";

export interface StatisticsReportParams {
  startDate?: Date;
  endDate?: Date;
  trendDays?: number;
}

/**
 * Generate statistics report, mirroring RPTSTA00 2000-PROCESS-REPORT.
 */
export async function generateStatisticsReport(
  params: StatisticsReportParams = {}
): Promise<StatisticsMetrics> {
  const now = new Date();
  const endDate = params.endDate ?? now;
  const trendDays = params.trendDays ?? 30;
  const startDate =
    params.startDate ?? new Date(endDate.getTime() - trendDays * 86400000);

  // 2100-PROCESS-DB2-STATS equivalent: Portfolio metrics
  const portfolioMetrics = await computePortfolioMetrics();

  // 2200-PROCESS-BATCH-STATS equivalent: Transaction metrics
  const transactionMetrics = await computeTransactionMetrics(startDate, endDate);

  // 2300-CALCULATE-METRICS: Error rate and severity breakdown
  const errorMetrics = await computeErrorMetrics(startDate, endDate);

  // 2430-WRITE-TREND-ANALYSIS: Daily trend data for charts
  const trendData = await computeTrendData(startDate, endDate);

  return {
    reportDate: now.toISOString().split("T")[0],
    generatedAt: now.toISOString(),
    portfolioMetrics,
    transactionMetrics,
    errorMetrics,
    trendData,
  };
}

/**
 * Compute portfolio-level metrics.
 * Mirrors 2100-PROCESS-DB2-STATS / 2310-CALC-DB2-METRICS:
 * aggregate position values across all active portfolios.
 */
async function computePortfolioMetrics(): Promise<StatisticsMetrics["portfolioMetrics"]> {
  const [totalPortfolios, activePortfolios] = await Promise.all([
    prisma.portfolioMaster.count(),
    prisma.portfolioMaster.count({ where: { status: "A" } }),
  ]);

  const positionAggregates = await prisma.investmentPosition.aggregate({
    _sum: {
      marketValue: true,
      costBasis: true,
    },
  });

  const totalMarketValue = new Decimal(
    positionAggregates._sum.marketValue?.toString() ?? "0"
  );
  const totalCostBasis = new Decimal(
    positionAggregates._sum.costBasis?.toString() ?? "0"
  );

  // 2310-CALC-DB2-METRICS: overall return = (market - cost) / cost * 100
  const overallReturn = totalCostBasis.isZero()
    ? new Decimal(0)
    : totalMarketValue.minus(totalCostBasis).dividedBy(totalCostBasis).times(100);

  return {
    totalPortfolios,
    activePortfolios,
    totalMarketValue: totalMarketValue.toFixed(2),
    totalCostBasis: totalCostBasis.toFixed(2),
    overallReturn: overallReturn.toFixed(2),
  };
}

/**
 * Compute transaction metrics for a date range.
 * Mirrors 2200-PROCESS-BATCH-STATS / 2320-CALC-BATCH-METRICS:
 * count jobs, calculate success rate.
 */
async function computeTransactionMetrics(
  startDate: Date,
  endDate: Date
): Promise<StatisticsMetrics["transactionMetrics"]> {
  const dateFilter = {
    transactionDate: { gte: startDate, lte: endDate },
  };

  const transactions = await prisma.transactionHistory.findMany({
    where: dateFilter,
    select: {
      transactionType: true,
      status: true,
      amount: true,
    },
  });

  const byType: Record<string, number> = {};
  const byStatus: Record<string, number> = {};
  let totalVolume = new Decimal(0);

  for (const txn of transactions) {
    const type = txn.transactionType.trim();
    const status = txn.status.trim();

    byType[type] = (byType[type] ?? 0) + 1;
    byStatus[status] = (byStatus[status] ?? 0) + 1;
    totalVolume = totalVolume.plus(new Decimal(txn.amount.toString()).abs());
  }

  return {
    totalTransactions: transactions.length,
    byType,
    byStatus,
    totalVolume: totalVolume.toFixed(2),
    periodStart: startDate.toISOString().split("T")[0],
    periodEnd: endDate.toISOString().split("T")[0],
  };
}

/**
 * Compute error metrics for a date range.
 * Extends RPTAUD00's 2200-PROCESS-ERROR-LOG / 2220-SUMMARIZE-ERRORS.
 */
async function computeErrorMetrics(
  startDate: Date,
  endDate: Date
): Promise<StatisticsMetrics["errorMetrics"]> {
  const dateFilter = {
    processDate: { gte: startDate, lte: endDate },
  };

  const errors = await prisma.errorLog.findMany({
    where: dateFilter,
    select: {
      errorSeverity: true,
      programId: true,
    },
  });

  const bySeverity: Record<string, number> = {};
  const byProgram: Record<string, number> = {};

  for (const err of errors) {
    const sev = String(err.errorSeverity);
    const prog = err.programId.trim();

    bySeverity[sev] = (bySeverity[sev] ?? 0) + 1;
    byProgram[prog] = (byProgram[prog] ?? 0) + 1;
  }

  // Error rate: errors per day in the period
  const days = Math.max(
    1,
    (endDate.getTime() - startDate.getTime()) / 86400000
  );
  const errorRate = new Decimal(errors.length).dividedBy(days);

  return {
    totalErrors: errors.length,
    bySeverity,
    byProgram,
    errorRate: errorRate.toFixed(2),
  };
}

/**
 * Compute daily trend data for charts.
 * Mirrors RPTSTA00 2430-WRITE-TREND-ANALYSIS paragraph.
 */
async function computeTrendData(
  startDate: Date,
  endDate: Date
): Promise<StatisticsMetrics["trendData"]> {
  // Daily transaction volumes
  const transactions = await prisma.transactionHistory.findMany({
    where: {
      transactionDate: { gte: startDate, lte: endDate },
    },
    select: {
      transactionDate: true,
      amount: true,
    },
    orderBy: { transactionDate: "asc" },
  });

  const dailyTxnMap = new Map<string, { count: number; volume: Decimal }>();
  for (const txn of transactions) {
    const dateStr = txn.transactionDate.toISOString().split("T")[0];
    const existing = dailyTxnMap.get(dateStr) ?? {
      count: 0,
      volume: new Decimal(0),
    };
    existing.count++;
    existing.volume = existing.volume.plus(
      new Decimal(txn.amount.toString()).abs()
    );
    dailyTxnMap.set(dateStr, existing);
  }

  const dailyTransactionVolumes = Array.from(dailyTxnMap.entries()).map(
    ([date, data]) => ({
      date,
      count: data.count,
      volume: data.volume.toFixed(2),
    })
  );

  // Daily error counts
  const errors = await prisma.errorLog.findMany({
    where: {
      processDate: { gte: startDate, lte: endDate },
    },
    select: {
      processDate: true,
    },
    orderBy: { processDate: "asc" },
  });

  const dailyErrMap = new Map<string, number>();
  for (const err of errors) {
    const dateStr = err.processDate.toISOString().split("T")[0];
    dailyErrMap.set(dateStr, (dailyErrMap.get(dateStr) ?? 0) + 1);
  }

  const dailyErrorCounts = Array.from(dailyErrMap.entries()).map(
    ([date, count]) => ({ date, count })
  );

  return { dailyTransactionVolumes, dailyErrorCounts };
}
