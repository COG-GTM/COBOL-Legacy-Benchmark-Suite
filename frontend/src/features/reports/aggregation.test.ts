import { describe, expect, it } from 'vitest';
import type { AuditEvent } from '../../types/audit';
import type { Portfolio } from '../../types/portfolio';
import type { Position } from '../../types/position';
import type {
  BatchJobRun,
  Db2DayStat,
  ReturnCodeEntry,
} from '../../types/report';
import {
  buildPositionReport,
  buildReturnAnalysis,
  buildSystemStatistics,
  filterAuditEvents,
  summarizeAuditEvents,
} from './aggregation';

function position(overrides: Partial<Position> = {}): Position {
  return {
    portfolioId: 'PORT0001',
    date: '20240401',
    investmentId: 'FND0000001',
    fundName: 'Vanguard 500 Index',
    quantity: '100.0000',
    costBasis: '1000.00',
    marketValue: '1200.00',
    currency: 'USD',
    status: 'A',
    lastMaintDate: '2024-04-01-18.05.00.000000',
    lastMaintUser: 'JSMITH',
    ...overrides,
  };
}

function portfolio(overrides: Partial<Portfolio> = {}): Portfolio {
  return {
    portId: 'PORT0001',
    accountNo: 'ACCT100001',
    clientName: 'Margaret Chen',
    clientType: 'I',
    createDate: '20200115',
    lastMaintDate: '20240401',
    status: 'A',
    totalValue: '1200.00',
    cashBalance: '0.00',
    lastUser: 'JSMITH',
    lastTransId: '00000001',
    ...overrides,
  };
}

function auditEvent(overrides: Partial<AuditEvent> = {}): AuditEvent {
  return {
    timestamp: '2024-04-01-09.31.22.000000',
    systemId: 'CLBSPROD',
    userId: 'JSMITH',
    program: 'PORTUPDT',
    terminal: 'TRM00014',
    type: 'TRAN',
    action: 'UPDATE',
    status: 'SUCC',
    portfolioId: 'PORT0001',
    accountNo: 'ACCT100001',
    message: 'Updated',
    ...overrides,
  };
}

function batchRun(overrides: Partial<BatchJobRun> = {}): BatchJobRun {
  return {
    jobName: 'CLBSNGHT',
    processDate: '20240401',
    sequenceNo: 10,
    programName: 'POSUPDT',
    status: 'D',
    returnCode: 0,
    restartCount: 0,
    elapsedSeconds: '100.00',
    recordsProcessed: 50,
    ...overrides,
  };
}

function db2Stat(overrides: Partial<Db2DayStat> = {}): Db2DayStat {
  return {
    date: '20240401',
    calls: 1000,
    elapsedSeconds: '20.00',
    cpuSeconds: '5.00',
    waitSeconds: '3.00',
    ...overrides,
  };
}

function returnCode(overrides: Partial<ReturnCodeEntry> = {}): ReturnCodeEntry {
  return {
    program: 'PORTMSTR',
    date: '20240401',
    status: 'S',
    code: 0,
    ...overrides,
  };
}

describe('buildPositionReport (RPTPOS00)', () => {
  const positions = [
    position(),
    position({ investmentId: 'FND0000002', marketValue: '800.00' }),
    position({
      portfolioId: 'PORT0002',
      investmentId: 'FND0000003',
      costBasis: '5000.00',
      marketValue: '4000.00',
      quantity: '10.5000',
    }),
  ];
  const portfolios = [
    portfolio(),
    portfolio({
      portId: 'PORT0002',
      accountNo: 'ACCT100002',
      clientName: 'Atlas Holdings LLC',
    }),
  ];

  it('rolls holdings up per portfolio with valuation and change percent', () => {
    const report = buildPositionReport(positions, portfolios);

    expect(report.rows.map((row) => row.portfolioId)).toEqual([
      'PORT0001',
      'PORT0002',
    ]);
    const [first, second] = report.rows;
    expect(first).toMatchObject({
      clientName: 'Margaret Chen',
      accountNo: 'ACCT100001',
      holdings: 2,
      totalQuantity: '200.0000',
      totalCostBasis: '2000.00',
      totalMarketValue: '2000.00',
      gainLoss: '0.00',
      gainLossPct: '0.00',
    });
    expect(second).toMatchObject({
      gainLoss: '-1000.00',
      gainLossPct: '-20.00',
    });
  });

  it('totals every detail line, as the report trailer does', () => {
    expect(buildPositionReport(positions, portfolios).totals).toEqual({
      holdings: 3,
      totalCostBasis: '7000.00',
      totalMarketValue: '6000.00',
      gainLoss: '-1000.00',
      gainLossPct: '-14.29',
    });
  });

  it('filters by date range, portfolio and maintenance user', () => {
    const withHistory = [
      ...positions,
      position({ date: '20240329', lastMaintUser: 'MGARCIA' }),
    ];

    expect(
      buildPositionReport(withHistory, portfolios, { toDate: '20240330' })
        .totals.holdings,
    ).toBe(1);
    expect(
      buildPositionReport(withHistory, portfolios, { portfolioId: 'PORT0002' })
        .rows.map((row) => row.portfolioId),
    ).toEqual(['PORT0002']);
    expect(
      buildPositionReport(withHistory, portfolios, { userId: 'MGARCIA' })
        .totals.holdings,
    ).toBe(1);
  });

  it('leaves the change percent undefined when there is no cost basis', () => {
    const report = buildPositionReport(
      [position({ costBasis: '0.00', marketValue: '10.00' })],
      portfolios,
    );
    expect(report.rows[0].gainLossPct).toBeNull();
  });
});

describe('filterAuditEvents (RPTAUD00)', () => {
  const events = [
    auditEvent(),
    auditEvent({
      timestamp: '2024-04-02-08.15.12.208000',
      userId: 'AHAMMETT',
      type: 'USER',
      action: 'LOGIN',
      portfolioId: '',
      accountNo: '',
    }),
    auditEvent({
      timestamp: '2024-03-29-06.00.02.114000',
      userId: 'SYSOPER',
      type: 'SYST',
      action: 'STARTUP',
      status: 'WARN',
      portfolioId: '',
      accountNo: '',
    }),
    auditEvent({
      timestamp: '2024-04-01-10.12.36.845000',
      status: 'FAIL',
      portfolioId: 'PORT0003',
    }),
  ];

  it('returns the newest event first', () => {
    expect(filterAuditEvents(events).map((e) => e.timestamp)).toEqual([
      '2024-04-02-08.15.12.208000',
      '2024-04-01-10.12.36.845000',
      '2024-04-01-09.31.22.000000',
      '2024-03-29-06.00.02.114000',
    ]);
  });

  it('filters on the event date derived from AUD-TIMESTAMP', () => {
    expect(
      filterAuditEvents(events, {
        fromDate: '20240401',
        toDate: '20240401',
      }),
    ).toHaveLength(2);
  });

  it('filters by user, portfolio, type, action and status', () => {
    expect(filterAuditEvents(events, { userId: 'AHAMMETT' })).toHaveLength(1);
    expect(filterAuditEvents(events, { portfolioId: 'PORT0003' })).toHaveLength(
      1,
    );
    expect(filterAuditEvents(events, { type: 'SYST' })).toHaveLength(1);
    expect(filterAuditEvents(events, { action: 'LOGIN' })).toHaveLength(1);
    expect(filterAuditEvents(events, { status: 'FAIL' })).toHaveLength(1);
  });

  it('summarizes events by AUD-STATUS', () => {
    expect(summarizeAuditEvents(events)).toEqual({
      total: 4,
      success: 2,
      failure: 1,
      warning: 1,
      failureRatePct: '25.00',
    });
    expect(summarizeAuditEvents([]).failureRatePct).toBeNull();
  });
});

describe('buildSystemStatistics (RPTSTA00)', () => {
  const runs = [
    batchRun(),
    batchRun({ sequenceNo: 20, status: 'E', returnCode: 12, restartCount: 1, recordsProcessed: 0 }),
    batchRun({ processDate: '20240402', sequenceNo: 10, recordsProcessed: 25 }),
  ];
  const stats = [db2Stat(), db2Stat({ date: '20240402', calls: 500 })];

  it('accumulates DB2 counters and derives the average response time', () => {
    const { db2 } = buildSystemStatistics(runs, stats);
    expect(db2).toEqual({
      calls: 1500,
      elapsedSeconds: '40.00',
      cpuSeconds: '10.00',
      waitSeconds: '6.00',
      avgResponseMs: '26.667',
    });
  });

  it('derives batch success and error rates', () => {
    const { batch } = buildSystemStatistics(runs, stats);
    expect(batch).toEqual({
      jobs: 3,
      succeeded: 2,
      failed: 1,
      restarts: 1,
      recordsProcessed: 75,
      elapsedSeconds: '300.00',
      successRatePct: '66.67',
      errorRatePct: '33.33',
    });
  });

  it('breaks volumes down per day, oldest first', () => {
    expect(buildSystemStatistics(runs, stats).daily).toEqual([
      {
        date: '20240401',
        db2Calls: 1000,
        batchJobs: 2,
        batchFailed: 1,
        recordsProcessed: 50,
        errorRatePct: '50.00',
      },
      {
        date: '20240402',
        db2Calls: 500,
        batchJobs: 1,
        batchFailed: 0,
        recordsProcessed: 25,
        errorRatePct: '0.00',
      },
    ]);
  });

  it('restricts the report to the selected period', () => {
    const filtered = buildSystemStatistics(runs, stats, {
      fromDate: '20240402',
    });
    expect(filtered.daily).toHaveLength(1);
    expect(filtered.batch.failed).toBe(0);
  });

  it('reports no rate at all when nothing ran', () => {
    const empty = buildSystemStatistics([], [], { fromDate: '20250101' });
    expect(empty.batch.errorRatePct).toBeNull();
    expect(empty.db2.avgResponseMs).toBeNull();
  });
});

describe('buildReturnAnalysis (RTNANA00)', () => {
  const entries = [
    // Prior period 20240325-20240329.
    returnCode({ date: '20240326' }),
    returnCode({ date: '20240326' }),
    returnCode({ date: '20240327', status: 'E', code: 8 }),
    returnCode({ date: '20240327', program: 'POSUPDT' }),
    // Current period 20240330-20240403.
    returnCode({ date: '20240401' }),
    returnCode({ date: '20240401', status: 'W', code: 4 }),
    returnCode({ date: '20240402', status: 'F', code: 12 }),
    returnCode({ date: '20240403', program: 'POSUPDT' }),
    returnCode({ date: '20240403', program: 'POSUPDT', status: 'E', code: 8 }),
  ];
  const period = { fromDate: '20240330', toDate: '20240403' };

  it('compares each program with the preceding period of equal length', () => {
    const analysis = buildReturnAnalysis(entries, period);

    expect(analysis.priorPeriod).toEqual({
      fromDate: '20240325',
      toDate: '20240329',
    });
    expect(analysis.rows.map((row) => row.program)).toEqual([
      'PORTMSTR',
      'POSUPDT',
    ]);
    expect(analysis.rows[0]).toMatchObject({
      total: 3,
      success: 1,
      warning: 1,
      error: 0,
      severe: 1,
      failureRatePct: '33.33',
      priorFailureRatePct: '33.33',
      failureRateDeltaPct: '0.00',
      volumeChangePct: '0.00',
    });
    expect(analysis.rows[1]).toMatchObject({
      total: 2,
      failureRatePct: '50.00',
      priorFailureRatePct: '0.00',
      failureRateDeltaPct: '50.00',
      volumeChangePct: '100.00',
    });
  });

  it('totals the detail lines including the prior period', () => {
    const { totals } = buildReturnAnalysis(entries, period);
    expect(totals).toMatchObject({
      program: 'TOTALS',
      total: 5,
      prior: { total: 4, success: 3, error: 1, warning: 0, severe: 0 },
      failureRatePct: '40.00',
      priorFailureRatePct: '25.00',
      failureRateDeltaPct: '15.00',
      volumeChangePct: '25.00',
    });
  });

  it('defaults to the recent half of the data so a prior period exists', () => {
    const analysis = buildReturnAnalysis(entries);
    expect(analysis.period).toEqual({
      fromDate: '20240330',
      toDate: '20240403',
    });
    expect(analysis.priorPeriod).toEqual({
      fromDate: '20240325',
      toDate: '20240329',
    });
    expect(analysis.totals.prior.total).toBeGreaterThan(0);
  });

  it('leaves comparisons undefined when the prior period is empty', () => {
    const analysis = buildReturnAnalysis(entries, {
      fromDate: '20240401',
      toDate: '20240403',
    });
    expect(analysis.rows[0].priorFailureRatePct).toBeNull();
    expect(analysis.rows[0].volumeChangePct).toBeNull();
    expect(analysis.rows[0].failureRateDeltaPct).toBeNull();
  });
});
