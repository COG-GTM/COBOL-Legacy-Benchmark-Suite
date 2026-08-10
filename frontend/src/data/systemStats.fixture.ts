import type { BatchJobRun, Db2DayStat } from '../types/report';

/**
 * Mock inputs for the system statistics report (RPTSTA00).
 *
 * `BATCH_JOB_FIXTURE` mirrors the BCHSTATS file (`01 BATCH-CONTROL-RECORD`
 * from `src/copybook/batch/BCHCTL.cpy`) and `DB2_STAT_FIXTURE` mirrors the
 * DB2STATS file accumulated into WS-DB2-METRICS. The 20240402 cycle contains a
 * failed POSUPDT step that is restarted, so the error rate and restart counters
 * are non-zero for that day.
 */

type BatchRow = [
  jobName: string,
  processDate: string,
  sequenceNo: number,
  programName: string,
  status: BatchJobRun['status'],
  returnCode: number,
  restartCount: number,
  elapsedSeconds: string,
  recordsProcessed: number,
];

const BATCH_ROWS: BatchRow[] = [
  ['CLBSNGHT', '20240329', 10, 'PORTLOAD', 'D', 0, 0, '182.40', 8],
  ['CLBSNGHT', '20240329', 20, 'POSUPDT', 'D', 0, 0, '640.15', 18],
  ['CLBSNGHT', '20240329', 30, 'RPTPOS00', 'D', 0, 0, '95.80', 18],
  ['CLBSNGHT', '20240329', 40, 'RPTAUD00', 'D', 4, 0, '61.25', 8],
  ['CLBSNGHT', '20240329', 50, 'RTNANA00', 'D', 0, 0, '44.10', 26],

  ['CLBSMEND', '20240331', 10, 'PORTLOAD', 'D', 0, 0, '190.05', 8],
  ['CLBSMEND', '20240331', 20, 'POSUPDT', 'D', 4, 0, '712.60', 18],
  ['CLBSMEND', '20240331', 30, 'RPTPOS00', 'D', 0, 0, '101.35', 18],
  ['CLBSMEND', '20240331', 40, 'RPTSTA00', 'D', 0, 0, '58.90', 12],
  ['CLBSMEND', '20240331', 50, 'RPTAUD00', 'D', 0, 0, '66.70', 12],
  ['CLBSMEND', '20240331', 60, 'RTNANA00', 'D', 0, 0, '47.55', 31],

  ['CLBSNGHT', '20240401', 10, 'PORTLOAD', 'D', 0, 0, '176.90', 8],
  ['CLBSNGHT', '20240401', 20, 'POSUPDT', 'D', 0, 0, '655.45', 18],
  ['CLBSNGHT', '20240401', 30, 'RPTPOS00', 'D', 0, 0, '97.20', 18],
  ['CLBSNGHT', '20240401', 40, 'RPTAUD00', 'D', 0, 0, '63.05', 16],
  ['CLBSNGHT', '20240401', 50, 'RTNANA00', 'D', 4, 0, '45.65', 29],

  ['CLBSNGHT', '20240402', 10, 'PORTLOAD', 'D', 0, 0, '181.10', 8],
  ['CLBSNGHT', '20240402', 20, 'POSUPDT', 'E', 12, 1, '128.35', 0],
  ['CLBSNGHT', '20240402', 21, 'POSUPDT', 'D', 0, 1, '689.90', 18],
  ['CLBSNGHT', '20240402', 30, 'RPTPOS00', 'D', 0, 0, '99.75', 18],
  ['CLBSNGHT', '20240402', 40, 'RPTAUD00', 'D', 0, 0, '64.80', 10],
  ['CLBSNGHT', '20240402', 50, 'RTNANA00', 'E', 8, 0, '12.05', 0],
];

export const BATCH_JOB_FIXTURE: BatchJobRun[] = BATCH_ROWS.map(
  ([
    jobName,
    processDate,
    sequenceNo,
    programName,
    status,
    returnCode,
    restartCount,
    elapsedSeconds,
    recordsProcessed,
  ]) => ({
    jobName,
    processDate,
    sequenceNo,
    programName,
    status,
    returnCode,
    restartCount,
    elapsedSeconds,
    recordsProcessed,
  }),
);

export const DB2_STAT_FIXTURE: Db2DayStat[] = [
  {
    date: '20240329',
    calls: 184320,
    elapsedSeconds: '3021.44',
    cpuSeconds: '812.19',
    waitSeconds: '640.72',
  },
  {
    date: '20240331',
    calls: 246880,
    elapsedSeconds: '4410.06',
    cpuSeconds: '1102.55',
    waitSeconds: '988.31',
  },
  {
    date: '20240401',
    calls: 192455,
    elapsedSeconds: '3164.90',
    cpuSeconds: '845.02',
    waitSeconds: '671.18',
  },
  {
    date: '20240402',
    calls: 205117,
    elapsedSeconds: '3980.27',
    cpuSeconds: '901.44',
    waitSeconds: '1043.66',
  },
];
