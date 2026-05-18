import type { PositionRecord, HistoryRecord, BatchControlRecord, Portfolio } from '@/types';
import positionsData from '@/mock-data/positions.json';
import historyData from '@/mock-data/history.json';
import portfoliosData from '@/mock-data/portfolios.json';
import batchStatusData from '@/mock-data/batch-status.json';
import reportsData from '@/mock-data/reports.json';

function delay(ms = 300): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

export interface PaginatedResult<T> {
  data: T[];
  total: number;
  page: number;
  pageSize: number;
}

export const positionService = {
  async getByAccount(accountNo: string): Promise<PositionRecord[]> {
    await delay();
    return (positionsData as PositionRecord[]).filter((p) => p.accountNo === accountNo);
  },

  async getAll(): Promise<PositionRecord[]> {
    await delay();
    return positionsData as PositionRecord[];
  },
};

export const historyService = {
  async getByAccount(
    accountNo: string,
    options?: { startDate?: string; endDate?: string; page?: number; pageSize?: number }
  ): Promise<PaginatedResult<HistoryRecord>> {
    await delay();
    let records = (historyData as HistoryRecord[]).filter((h) => h.accountNo === accountNo);

    if (options?.startDate) {
      records = records.filter((h) => h.timestamp >= options.startDate!);
    }
    if (options?.endDate) {
      records = records.filter((h) => h.timestamp <= options.endDate!);
    }

    records.sort((a, b) => b.timestamp.localeCompare(a.timestamp));

    const page = options?.page ?? 1;
    const pageSize = options?.pageSize ?? 10;
    const start = (page - 1) * pageSize;

    return {
      data: records.slice(start, start + pageSize),
      total: records.length,
      page,
      pageSize,
    };
  },
};

export const portfolioService = {
  async getAll(): Promise<Portfolio[]> {
    await delay();
    return portfoliosData as Portfolio[];
  },

  async getById(portfolioId: string): Promise<Portfolio | undefined> {
    await delay();
    return (portfoliosData as Portfolio[]).find((p) => p.portfolioId === portfolioId);
  },

  async create(portfolio: Omit<Portfolio, 'createDate' | 'totalValue'>): Promise<Portfolio> {
    await delay();
    const newPortfolio: Portfolio = {
      ...portfolio,
      createDate: new Date().toISOString().replace(/-/g, '').substring(0, 8),
      totalValue: 0,
    };
    return newPortfolio;
  },

  async update(portfolio: Portfolio): Promise<Portfolio> {
    await delay();
    return portfolio;
  },

  async remove(portfolioId: string): Promise<void> {
    await delay();
    const exists = (portfoliosData as Portfolio[]).find((p) => p.portfolioId === portfolioId);
    if (!exists) {
      throw new Error('Portfolio not found');
    }
  },
};

export const batchService = {
  async getAll(): Promise<BatchControlRecord[]> {
    await delay();
    return batchStatusData as BatchControlRecord[];
  },

  async getByDate(processDate: string): Promise<BatchControlRecord[]> {
    await delay();
    return (batchStatusData as BatchControlRecord[]).filter((b) => b.processDate === processDate);
  },
};

export interface PositionReportEntry {
  accountNo: string;
  fundId: string;
  fundName: string;
  shares: number;
  costBasis: number;
  marketValue: number;
  gainLoss: number;
  gainLossPercent: number;
}

export interface AuditReportEntry {
  timestamp: string;
  userId: string;
  program: string;
  action: string;
  status: string;
  portfolioId: string;
  details: string;
}

export interface StatisticsReport {
  systemMetrics: {
    totalTransactionsToday: number;
    avgResponseTimeMs: number;
    peakResponseTimeMs: number;
    errorRate: number;
    uptime: number;
  };
  batchMetrics: {
    avgBatchDurationMin: number;
    totalRecordsProcessed: number;
    totalErrorsThisWeek: number;
    successRate: number;
  };
  dailyVolumes: { date: string; transactions: number }[];
}

export const reportService = {
  async getPositionReport(): Promise<PositionReportEntry[]> {
    await delay();
    return reportsData.positionReport as PositionReportEntry[];
  },

  async getAuditReport(): Promise<AuditReportEntry[]> {
    await delay();
    return reportsData.auditReport as AuditReportEntry[];
  },

  async getStatisticsReport(): Promise<StatisticsReport> {
    await delay();
    return reportsData.statisticsReport as StatisticsReport;
  },
};
