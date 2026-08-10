import type { AuditEvent } from '../types/audit';
import type {
  AuditQuery,
  PositionReport,
  ReportFilters,
  ReturnAnalysis,
  SystemStatistics,
} from '../types/report';

/**
 * Data-access boundary for the reporting subsystem — the modern replacement for
 * the batch report programs RPTPOS00, RPTAUD00, RPTSTA00 and RTNANA00, whose
 * output was previously only available as printed RPTFILE listings.
 *
 * The views depend only on this interface; {@link MockReportService} implements
 * it against in-memory fixtures today, and a REST implementation reading the
 * same VSAM/DB2 sources can replace it without touching the components.
 */
export interface ReportService {
  /** RPTPOS00 — position detail and portfolio valuation totals. */
  getPositionReport(filters?: ReportFilters): Promise<PositionReport>;
  /** RPTAUD00 — the filtered AUDITLOG event stream, newest first. */
  listAuditEvents(query?: AuditQuery): Promise<AuditEvent[]>;
  /** RPTSTA00 — DB2/batch processing volumes and error rates. */
  getSystemStatistics(filters?: ReportFilters): Promise<SystemStatistics>;
  /** RTNANA00 — return codes by program, against the prior period. */
  getReturnAnalysis(filters?: ReportFilters): Promise<ReturnAnalysis>;
  /** Portfolio ids and user ids available as filter values. */
  getFilterOptions(): Promise<ReportFilterOptions>;
}

/** Selectable values for the report filter bar. */
export interface ReportFilterOptions {
  /** PORT-ID values present in the portfolio master. */
  portfolioIds: string[];
  /** AUD-USER-ID / POS-LAST-MAINT-USER values present in the report sources. */
  userIds: string[];
}
