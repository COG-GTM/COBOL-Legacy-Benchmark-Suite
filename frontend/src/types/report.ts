/**
 * Domain types for the reporting subsystem — the modern replacement for the
 * batch report programs RPTPOS00 (daily position report), RPTAUD00 (audit
 * report), RPTSTA00 (system statistics) and RTNANA00 (return code analysis).
 *
 * All monetary values are decimal strings (see utils/decimal.ts); percentages
 * are decimal strings with 2 fraction digits, matching the edited PIC clauses
 * used by the batch reports (e.g. `+ZZ9.99`, `ZZ9.99%`).
 */

import type { AuditAction, AuditStatus, AuditType } from './audit';

/** Filters shared by the report views; dates are `PIC 9(8)` YYYYMMDD strings. */
export interface ReportFilters {
  /** Inclusive lower bound on the report date. */
  fromDate?: string;
  /** Inclusive upper bound on the report date. */
  toDate?: string;
  /** PORT-ID / POS-PORTFOLIO-ID / AUD-PORTFOLIO-ID; empty means all. */
  portfolioId?: string;
  /** POS-LAST-MAINT-USER / AUD-USER-ID; empty means all. */
  userId?: string;
}

/** Audit report filters: the shared filters plus the AUDITLOG classifiers. */
export interface AuditQuery extends ReportFilters {
  /** AUD-TYPE; empty means all. */
  type?: AuditType | '';
  /** AUD-ACTION; empty means all. */
  action?: AuditAction | '';
  /** AUD-STATUS; empty means all. */
  status?: AuditStatus | '';
}

/** One line of the RPTPOS00 position detail, aggregated per portfolio. */
export interface PositionReportRow {
  /** POS-PORTFOLIO-ID. */
  portfolioId: string;
  /** PORT-CLIENT-NAME from the portfolio master (WS-POS-DESCRIPTION). */
  clientName: string;
  /** PORT-ACCOUNT-NO from the portfolio master. */
  accountNo: string;
  /** Number of holdings rolled up into this line. */
  holdings: number;
  /** Sum of POS-QUANTITY (4 fraction digits). */
  totalQuantity: string;
  /** Sum of POS-COST-BASIS. */
  totalCostBasis: string;
  /** Sum of POS-MARKET-VALUE (WS-POS-VALUE). */
  totalMarketValue: string;
  /** totalMarketValue − totalCostBasis. */
  gainLoss: string;
  /** gainLoss / totalCostBasis × 100 (WS-POS-CHANGE-PCT); null when there is no cost basis. */
  gainLossPct: string | null;
}

/** Result of the position report: detail lines plus the RPTPOS00 totals. */
export interface PositionReport {
  rows: PositionReportRow[];
  totals: {
    holdings: number;
    totalCostBasis: string;
    totalMarketValue: string;
    gainLoss: string;
    gainLossPct: string | null;
  };
}

/**
 * A batch job execution, mirroring `01 BATCH-CONTROL-RECORD` from
 * `src/copybook/batch/BCHCTL.cpy` (the BCHSTATS input of RPTSTA00).
 */
export interface BatchJobRun {
  /** BCT-JOB-NAME PIC X(8). */
  jobName: string;
  /** BCT-PROCESS-DATE PIC X(8) — YYYYMMDD. */
  processDate: string;
  /** BCT-SEQUENCE-NO PIC 9(4). */
  sequenceNo: number;
  /** BCT-PROGRAM-NAME PIC X(8). */
  programName: string;
  /** BCT-STATUS: R=Ready, A=Active, W=Waiting, D=Done, E=Error. */
  status: 'R' | 'A' | 'W' | 'D' | 'E';
  /** BCT-RETURN-CODE PIC S9(4) COMP. */
  returnCode: number;
  /** BCT-RESTART-COUNT PIC 9(2) COMP. */
  restartCount: number;
  /** Elapsed seconds derived from BCT-START-TIME / BCT-END-TIME. */
  elapsedSeconds: string;
  /** Records processed by the step (WS-BATCH volume accumulator). */
  recordsProcessed: number;
}

/** A day of DB2 activity — the DB2STATS input of RPTSTA00. */
export interface Db2DayStat {
  /** YYYYMMDD. */
  date: string;
  /** WS-DB2-CALLS PIC 9(9). */
  calls: number;
  /** WS-DB2-ELAPSED PIC 9(9)V99 — seconds. */
  elapsedSeconds: string;
  /** WS-DB2-CPU PIC 9(9)V99 — seconds. */
  cpuSeconds: string;
  /** WS-DB2-WAIT PIC 9(9)V99 — seconds. */
  waitSeconds: string;
}

/** Aggregated RPTSTA00 output for the selected period. */
export interface SystemStatistics {
  db2: {
    calls: number;
    elapsedSeconds: string;
    cpuSeconds: string;
    waitSeconds: string;
    /** WS-DB2-AVG-RESP — elapsed / calls, in milliseconds; null when there were no calls. */
    avgResponseMs: string | null;
  };
  batch: {
    jobs: number;
    succeeded: number;
    failed: number;
    restarts: number;
    recordsProcessed: number;
    elapsedSeconds: string;
    /** WS-SUCCESS-RATE — succeeded / jobs × 100; null when no jobs ran. */
    successRatePct: string | null;
    /** failed / jobs × 100; null when no jobs ran. */
    errorRatePct: string | null;
  };
  /** Per-day processing volumes, oldest first, for the trend section. */
  daily: SystemStatisticsDay[];
}

/** One day of the RPTSTA00 trend analysis section. */
export interface SystemStatisticsDay {
  date: string;
  db2Calls: number;
  batchJobs: number;
  batchFailed: number;
  recordsProcessed: number;
  errorRatePct: string | null;
}

/**
 * A logged return code, mirroring the RTNCODES DB2 table read by RTNANA00
 * (`SELECT PROGRAM_ID, STATUS_CODE ... GROUP BY PROGRAM_ID`). Status codes are
 * the RC-STATUS 88-levels from `src/copybook/common/RTNCODE.cpy`.
 */
export interface ReturnCodeEntry {
  /** RC-PROGRAM-ID PIC X(8). */
  program: string;
  /** Log date, YYYYMMDD (derived from RC-ANALYSIS-DATA timestamps). */
  date: string;
  /** RC-STATUS: S=Success, W=Warning, E=Error, F=Severe. */
  status: 'S' | 'W' | 'E' | 'F';
  /** RC-CURRENT-CODE PIC S9(4) COMP. */
  code: number;
}

/** Counts of return codes by RC-STATUS (the RTNANA00 detail columns). */
export interface ReturnCodeCounts {
  total: number;
  success: number;
  warning: number;
  error: number;
  severe: number;
}

/** One RTNANA00 detail line, with its prior-period comparison. */
export interface ReturnAnalysisRow extends ReturnCodeCounts {
  /** RC-PROGRAM-ID. */
  program: string;
  /** Same counts over the immediately preceding period of equal length. */
  prior: ReturnCodeCounts;
  /** (error + severe) / total × 100 for the current period; null when no codes were logged. */
  failureRatePct: string | null;
  /** Failure rate over the prior period; null when the prior period is empty. */
  priorFailureRatePct: string | null;
  /** failureRatePct − priorFailureRatePct, in percentage points; null when either side is null. */
  failureRateDeltaPct: string | null;
  /** (total − prior.total) / prior.total × 100; null when the prior total is 0. */
  volumeChangePct: string | null;
}

/** RTNANA00 output: per-program detail plus the TOTALS line. */
export interface ReturnAnalysis {
  /** The period actually analysed (YYYYMMDD, inclusive). */
  period: { fromDate: string; toDate: string };
  /** The equal-length period immediately before {@link period}. */
  priorPeriod: { fromDate: string; toDate: string };
  rows: ReturnAnalysisRow[];
  totals: ReturnAnalysisRow;
}
