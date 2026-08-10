import { AUDIT_FIXTURE } from '../data/audit.fixture';
import { PORTFOLIO_FIXTURE } from '../data/portfolios.fixture';
import { POSITION_FIXTURE } from '../data/positions.fixture';
import { POSITION_HISTORY_FIXTURE } from '../data/positionHistory.fixture';
import { RETURN_CODE_FIXTURE } from '../data/returnCodes.fixture';
import {
  BATCH_JOB_FIXTURE,
  DB2_STAT_FIXTURE,
} from '../data/systemStats.fixture';
import {
  buildPositionReport,
  buildReturnAnalysis,
  buildSystemStatistics,
  filterAuditEvents,
} from '../features/reports/aggregation';
import type { AuditEvent } from '../types/audit';
import type { Portfolio } from '../types/portfolio';
import type { Position } from '../types/position';
import type {
  AuditQuery,
  BatchJobRun,
  Db2DayStat,
  PositionReport,
  ReportFilters,
  ReturnAnalysis,
  ReturnCodeEntry,
  SystemStatistics,
} from '../types/report';
import type { ReportFilterOptions, ReportService } from './reportService';

const SIMULATED_LATENCY_MS = 150;

function delay<T>(value: T): Promise<T> {
  return new Promise((resolve) =>
    setTimeout(() => resolve(value), SIMULATED_LATENCY_MS),
  );
}

/** The report sources, so tests can substitute their own fixtures. */
export interface ReportSources {
  positions: readonly Position[];
  portfolios: readonly Portfolio[];
  auditEvents: readonly AuditEvent[];
  batchRuns: readonly BatchJobRun[];
  db2Stats: readonly Db2DayStat[];
  returnCodes: readonly ReturnCodeEntry[];
}

const DEFAULT_SOURCES: ReportSources = {
  // RPTPOS00 reads the whole POSFILE, not just the latest snapshot, so the
  // report sees the historical position records as well as the current ones.
  positions: [...POSITION_FIXTURE, ...POSITION_HISTORY_FIXTURE],
  portfolios: PORTFOLIO_FIXTURE,
  auditEvents: AUDIT_FIXTURE,
  batchRuns: BATCH_JOB_FIXTURE,
  db2Stats: DB2_STAT_FIXTURE,
  returnCodes: RETURN_CODE_FIXTURE,
};

/**
 * In-memory {@link ReportService} backed by the report fixtures. Stands in for
 * the batch report programs until an API over POSFILE / AUDITLOG / BCHCTL /
 * RTNCODES is available.
 */
export class MockReportService implements ReportService {
  private readonly sources: ReportSources;

  constructor(sources: Partial<ReportSources> = {}) {
    this.sources = { ...DEFAULT_SOURCES, ...sources };
  }

  async getPositionReport(
    filters: ReportFilters = {},
  ): Promise<PositionReport> {
    return delay(
      buildPositionReport(
        this.sources.positions,
        this.sources.portfolios,
        filters,
      ),
    );
  }

  async listAuditEvents(query: AuditQuery = {}): Promise<AuditEvent[]> {
    return delay(filterAuditEvents(this.sources.auditEvents, query));
  }

  async getSystemStatistics(
    filters: ReportFilters = {},
  ): Promise<SystemStatistics> {
    return delay(
      buildSystemStatistics(
        this.sources.batchRuns,
        this.sources.db2Stats,
        filters,
      ),
    );
  }

  async getReturnAnalysis(
    filters: ReportFilters = {},
  ): Promise<ReturnAnalysis> {
    return delay(buildReturnAnalysis(this.sources.returnCodes, filters));
  }

  async getFilterOptions(): Promise<ReportFilterOptions> {
    const portfolioIds = new Set<string>(
      this.sources.portfolios.map((portfolio) => portfolio.portId),
    );
    const userIds = new Set<string>();
    for (const position of this.sources.positions) {
      portfolioIds.add(position.portfolioId);
      userIds.add(position.lastMaintUser);
    }
    for (const event of this.sources.auditEvents) {
      if (event.portfolioId) portfolioIds.add(event.portfolioId);
      userIds.add(event.userId);
    }
    return delay({
      portfolioIds: [...portfolioIds].sort(),
      userIds: [...userIds].sort(),
    });
  }
}
