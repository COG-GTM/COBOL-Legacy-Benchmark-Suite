import type { AuditEvent } from '../../types/audit';
import type { Position } from '../../types/position';
import type { Portfolio } from '../../types/portfolio';
import type {
  AuditQuery,
  BatchJobRun,
  Db2DayStat,
  PositionReport,
  PositionReportRow,
  ReportFilters,
  ReturnAnalysis,
  ReturnAnalysisRow,
  ReturnCodeCounts,
  ReturnCodeEntry,
  SystemStatistics,
  SystemStatisticsDay,
} from '../../types/report';
import {
  divideDecimals,
  percentageOf,
  shiftDecimalPoint,
  subtractDecimals,
  sumDecimals,
} from '../../utils/decimal';
import {
  cobolTimestampDate,
  inclusiveDayCount,
  shiftCobolDate,
} from '../../utils/date';

/**
 * Pure aggregation logic behind the report views — the modern equivalent of the
 * accumulate/summarize paragraphs in RPTPOS00, RPTAUD00, RPTSTA00 and RTNANA00.
 * Everything here is deterministic and free of I/O so it can be unit tested and
 * reused by a future REST-backed report service.
 */

/** True when `date` (YYYYMMDD) falls inside the inclusive filter range. */
function withinRange(date: string, { fromDate, toDate }: ReportFilters) {
  if (fromDate && date < fromDate) return false;
  if (toDate && date > toDate) return false;
  return true;
}

function matches(value: string, filter?: string) {
  return !filter || value.toUpperCase() === filter.trim().toUpperCase();
}

/**
 * Rolls positions up per portfolio, the way RPTPOS00 formats one detail line
 * per position group and then writes the report totals.
 *
 * RPTPOS00 computes WS-POS-CHANGE-PCT from POS-CURRENT-VALUE against
 * POS-PREVIOUS-VALUE, but POSREC.cpy carries no previous-value field — the
 * batch program reads it from a working copy of the prior run. The modern
 * report therefore measures the change against POS-COST-BASIS, i.e. the
 * unrealized gain/loss percentage, which is what the valuation summary needs.
 */
export function buildPositionReport(
  positions: readonly Position[],
  portfolios: readonly Portfolio[],
  filters: ReportFilters = {},
): PositionReport {
  const byPortfolio = new Map<string, Position[]>();
  for (const position of positions) {
    if (!withinRange(position.date, filters)) continue;
    if (!matches(position.portfolioId, filters.portfolioId)) continue;
    if (!matches(position.lastMaintUser, filters.userId)) continue;
    const group = byPortfolio.get(position.portfolioId);
    if (group) {
      group.push(position);
    } else {
      byPortfolio.set(position.portfolioId, [position]);
    }
  }

  const masterById = new Map(portfolios.map((p) => [p.portId, p]));
  const rows: PositionReportRow[] = [...byPortfolio.entries()]
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([portfolioId, group]) => {
      const master = masterById.get(portfolioId);
      const totalCostBasis = sumDecimals(group.map((p) => p.costBasis));
      const totalMarketValue = sumDecimals(group.map((p) => p.marketValue));
      const gainLoss = subtractDecimals(totalMarketValue, totalCostBasis);
      return {
        portfolioId,
        clientName: master?.clientName ?? '',
        accountNo: master?.accountNo ?? '',
        holdings: group.length,
        totalQuantity: sumDecimals(
          group.map((p) => p.quantity),
          4,
        ),
        totalCostBasis,
        totalMarketValue,
        gainLoss,
        gainLossPct: percentageOf(gainLoss, totalCostBasis),
      };
    });

  const totalCostBasis = sumDecimals(rows.map((r) => r.totalCostBasis));
  const totalMarketValue = sumDecimals(rows.map((r) => r.totalMarketValue));
  const gainLoss = subtractDecimals(totalMarketValue, totalCostBasis);
  return {
    rows,
    totals: {
      holdings: rows.reduce((sum, r) => sum + r.holdings, 0),
      totalCostBasis,
      totalMarketValue,
      gainLoss,
      gainLossPct: percentageOf(gainLoss, totalCostBasis),
    },
  };
}

/**
 * Filters the audit trail, newest event first — the browse RPTAUD00 performs
 * over the AUDITLOG file before summarizing it.
 */
export function filterAuditEvents(
  events: readonly AuditEvent[],
  query: AuditQuery = {},
): AuditEvent[] {
  return events
    .filter((event) => {
      if (!withinRange(cobolTimestampDate(event.timestamp), query))
        return false;
      if (!matches(event.portfolioId, query.portfolioId)) return false;
      if (!matches(event.userId, query.userId)) return false;
      if (query.type && event.type !== query.type) return false;
      if (query.action && event.action !== query.action) return false;
      if (query.status && event.status !== query.status) return false;
      return true;
    })
    .sort((a, b) => b.timestamp.localeCompare(a.timestamp));
}

/** Event counts by AUD-STATUS — the RPTAUD00 audit summary section. */
export interface AuditSummary {
  total: number;
  success: number;
  failure: number;
  warning: number;
  /** failure / total × 100; null when no events matched. */
  failureRatePct: string | null;
}

export function summarizeAuditEvents(
  events: readonly AuditEvent[],
): AuditSummary {
  const count = (status: AuditEvent['status']) =>
    events.filter((event) => event.status === status).length;
  const failure = count('FAIL');
  return {
    total: events.length,
    success: count('SUCC'),
    failure,
    warning: count('WARN'),
    failureRatePct: percentageOf(String(failure), String(events.length)),
  };
}

/**
 * Accumulates the DB2 and batch counters of RPTSTA00 over the filtered period
 * and derives the per-day trend section (2400-WRITE-REPORT).
 *
 * The user filter does not apply: BCHCTL / DB2STATS records are system-level
 * and carry no AUD-USER-ID equivalent.
 */
export function buildSystemStatistics(
  batchRuns: readonly BatchJobRun[],
  db2Stats: readonly Db2DayStat[],
  filters: ReportFilters = {},
): SystemStatistics {
  const runs = batchRuns.filter((run) => withinRange(run.processDate, filters));
  const stats = db2Stats.filter((stat) => withinRange(stat.date, filters));

  const failed = runs.filter((run) => run.status === 'E').length;
  const succeeded = runs.filter((run) => run.status === 'D').length;
  const db2Calls = stats.reduce((sum, stat) => sum + stat.calls, 0);
  const db2Elapsed = sumDecimals(stats.map((stat) => stat.elapsedSeconds));

  const dates = [
    ...new Set([
      ...runs.map((run) => run.processDate),
      ...stats.map((stat) => stat.date),
    ]),
  ].sort();
  const daily: SystemStatisticsDay[] = dates.map((date) => {
    const dayRuns = runs.filter((run) => run.processDate === date);
    const dayFailed = dayRuns.filter((run) => run.status === 'E').length;
    return {
      date,
      db2Calls: stats
        .filter((stat) => stat.date === date)
        .reduce((sum, stat) => sum + stat.calls, 0),
      batchJobs: dayRuns.length,
      batchFailed: dayFailed,
      recordsProcessed: dayRuns.reduce(
        (sum, run) => sum + run.recordsProcessed,
        0,
      ),
      errorRatePct: percentageOf(String(dayFailed), String(dayRuns.length)),
    };
  });

  return {
    db2: {
      calls: db2Calls,
      elapsedSeconds: db2Elapsed,
      cpuSeconds: sumDecimals(stats.map((stat) => stat.cpuSeconds)),
      waitSeconds: sumDecimals(stats.map((stat) => stat.waitSeconds)),
      // WS-DB2-AVG-RESP is reported in milliseconds; elapsed is in seconds.
      avgResponseMs: divideDecimals(
        shiftDecimalPoint(db2Elapsed, 3),
        String(db2Calls),
        3,
      ),
    },
    batch: {
      jobs: runs.length,
      succeeded,
      failed,
      restarts: runs.reduce((sum, run) => sum + run.restartCount, 0),
      recordsProcessed: runs.reduce(
        (sum, run) => sum + run.recordsProcessed,
        0,
      ),
      elapsedSeconds: sumDecimals(runs.map((run) => run.elapsedSeconds)),
      successRatePct: percentageOf(String(succeeded), String(runs.length)),
      errorRatePct: percentageOf(String(failed), String(runs.length)),
    },
    daily,
  };
}

const EMPTY_COUNTS: ReturnCodeCounts = {
  total: 0,
  success: 0,
  warning: 0,
  error: 0,
  severe: 0,
};

function countEntries(entries: readonly ReturnCodeEntry[]): ReturnCodeCounts {
  return entries.reduce<ReturnCodeCounts>(
    (counts, entry) => ({
      total: counts.total + 1,
      success: counts.success + (entry.status === 'S' ? 1 : 0),
      warning: counts.warning + (entry.status === 'W' ? 1 : 0),
      error: counts.error + (entry.status === 'E' ? 1 : 0),
      severe: counts.severe + (entry.status === 'F' ? 1 : 0),
    }),
    { ...EMPTY_COUNTS },
  );
}

function addCounts(a: ReturnCodeCounts, b: ReturnCodeCounts): ReturnCodeCounts {
  return {
    total: a.total + b.total,
    success: a.success + b.success,
    warning: a.warning + b.warning,
    error: a.error + b.error,
    severe: a.severe + b.severe,
  };
}

function failureRate(counts: ReturnCodeCounts): string | null {
  return percentageOf(
    String(counts.error + counts.severe),
    String(counts.total),
  );
}

function analysisRow(
  program: string,
  current: ReturnCodeCounts,
  prior: ReturnCodeCounts,
): ReturnAnalysisRow {
  const failureRatePct = failureRate(current);
  const priorFailureRatePct = failureRate(prior);
  return {
    program,
    ...current,
    prior,
    failureRatePct,
    priorFailureRatePct,
    failureRateDeltaPct:
      failureRatePct !== null && priorFailureRatePct !== null
        ? subtractDecimals(failureRatePct, priorFailureRatePct)
        : null,
    volumeChangePct: percentageOf(
      String(current.total - prior.total),
      String(prior.total),
    ),
  };
}

/**
 * Groups logged return codes by RC-PROGRAM-ID for the selected period and
 * compares each program against the immediately preceding period of equal
 * length — RTNANA00's per-program detail lines plus the TOTALS line, extended
 * with the period-over-period columns the trend analysis calls for.
 *
 * With no period selected the report covers the most recent half of the logged
 * span, so the equally long prior period is still backed by data.
 */
export function buildReturnAnalysis(
  entries: readonly ReturnCodeEntry[],
  filters: ReportFilters = {},
): ReturnAnalysis {
  const dates = entries.map((entry) => entry.date).sort();
  const firstLogged = dates[0] ?? '';
  const lastLogged = dates[dates.length - 1] ?? '';
  const toDate = filters.toDate || lastLogged;
  const fromDate =
    filters.fromDate ||
    shiftCobolDate(
      toDate,
      -(Math.ceil(inclusiveDayCount(firstLogged, lastLogged) / 2) - 1),
    );
  const days = inclusiveDayCount(fromDate, toDate);
  const priorPeriod = {
    fromDate: shiftCobolDate(fromDate, -days),
    toDate: shiftCobolDate(fromDate, -1),
  };

  const inPeriod = (entry: ReturnCodeEntry, from: string, to: string) =>
    (!from || entry.date >= from) && (!to || entry.date <= to);

  const currentEntries = entries.filter((entry) =>
    inPeriod(entry, fromDate, toDate),
  );
  const priorEntries = entries.filter((entry) =>
    inPeriod(entry, priorPeriod.fromDate, priorPeriod.toDate),
  );

  const programs = [
    ...new Set([
      ...currentEntries.map((entry) => entry.program),
      ...priorEntries.map((entry) => entry.program),
    ]),
  ].sort();

  const rows = programs.map((program) =>
    analysisRow(
      program,
      countEntries(currentEntries.filter((e) => e.program === program)),
      countEntries(priorEntries.filter((e) => e.program === program)),
    ),
  );

  return {
    period: { fromDate, toDate },
    priorPeriod,
    rows,
    totals: analysisRow(
      'TOTALS',
      rows.reduce<ReturnCodeCounts>(addCounts, { ...EMPTY_COUNTS }),
      rows.reduce<ReturnCodeCounts>((acc, row) => addCounts(acc, row.prior), {
        ...EMPTY_COUNTS,
      }),
    ),
  };
}
