import type { ReturnCodeEntry } from '../types/report';

/**
 * Mock RTNCODES table contents — the input RTNANA00 groups by PROGRAM_ID.
 *
 * Written as `[program, date, status, occurrences]` tuples and expanded into
 * one {@link ReturnCodeEntry} per logged code, so the fixture stays readable
 * while the data keeps the row-per-code shape of the DB2 table. RC-CURRENT-CODE
 * follows the usual mainframe convention for each RC-STATUS: S=0, W=4, E=8,
 * F=12 (`src/copybook/common/RTNCODE.cpy`).
 *
 * Dates span 20240324–20240402 so a five-day report period always has a
 * comparable five-day prior period behind it.
 */

type ReturnCodeRow = [
  program: string,
  date: string,
  status: ReturnCodeEntry['status'],
  occurrences: number,
];

const CODE_BY_STATUS: Record<ReturnCodeEntry['status'], number> = {
  S: 0,
  W: 4,
  E: 8,
  F: 12,
};

const ROWS: ReturnCodeRow[] = [
  // --- prior period: 20240324 - 20240328 ---
  ['PORTMSTR', '20240325', 'S', 142],
  ['PORTMSTR', '20240325', 'W', 6],
  ['PORTMSTR', '20240327', 'S', 138],
  ['PORTMSTR', '20240327', 'E', 3],
  ['PORTTRAN', '20240325', 'S', 96],
  ['PORTTRAN', '20240325', 'W', 11],
  ['PORTTRAN', '20240326', 'S', 88],
  ['PORTTRAN', '20240326', 'E', 9],
  ['PORTTRAN', '20240328', 'F', 2],
  ['POSUPDT', '20240324', 'S', 54],
  ['POSUPDT', '20240326', 'S', 61],
  ['POSUPDT', '20240328', 'W', 4],
  ['POSUPDT', '20240328', 'E', 1],
  ['INQPORT', '20240325', 'S', 210],
  ['INQPORT', '20240327', 'S', 198],
  ['INQPORT', '20240327', 'W', 5],
  ['RPTPOS00', '20240326', 'S', 12],
  ['RPTPOS00', '20240328', 'S', 11],
  ['RPTPOS00', '20240328', 'W', 1],
  ['PORTMSTR', '20240324', 'S', 147],
  ['PORTMSTR', '20240326', 'S', 151],
  ['PORTMSTR', '20240328', 'S', 139],
  ['PORTMSTR', '20240328', 'E', 4],
  ['PORTTRAN', '20240324', 'S', 111],
  ['PORTTRAN', '20240327', 'S', 104],
  ['PORTTRAN', '20240327', 'W', 7],
  ['PORTTRAN', '20240328', 'S', 97],
  ['POSUPDT', '20240325', 'S', 57],
  ['POSUPDT', '20240327', 'S', 59],
  ['POSUPDT', '20240328', 'S', 52],
  ['INQPORT', '20240324', 'S', 187],
  ['INQPORT', '20240326', 'S', 205],
  ['INQPORT', '20240328', 'S', 216],
  ['RPTPOS00', '20240324', 'S', 12],
  ['RPTPOS00', '20240327', 'S', 12],
  ['RTNANA00', '20240328', 'S', 4],

  // --- current period: 20240329 - 20240402 ---
  ['PORTMSTR', '20240329', 'S', 151],
  ['PORTMSTR', '20240331', 'S', 166],
  ['PORTMSTR', '20240331', 'W', 4],
  ['PORTMSTR', '20240401', 'S', 149],
  ['PORTMSTR', '20240401', 'E', 2],
  ['PORTMSTR', '20240402', 'S', 144],
  ['PORTTRAN', '20240329', 'S', 102],
  ['PORTTRAN', '20240331', 'S', 121],
  ['PORTTRAN', '20240331', 'W', 8],
  ['PORTTRAN', '20240331', 'E', 4],
  ['PORTTRAN', '20240401', 'S', 118],
  ['PORTTRAN', '20240402', 'S', 109],
  ['PORTTRAN', '20240402', 'W', 6],
  ['POSUPDT', '20240329', 'S', 58],
  ['POSUPDT', '20240331', 'S', 63],
  ['POSUPDT', '20240331', 'W', 2],
  ['POSUPDT', '20240401', 'S', 60],
  ['POSUPDT', '20240402', 'E', 7],
  ['POSUPDT', '20240402', 'F', 3],
  ['POSUPDT', '20240402', 'S', 55],
  ['INQPORT', '20240329', 'S', 224],
  ['INQPORT', '20240331', 'S', 187],
  ['INQPORT', '20240401', 'S', 241],
  ['INQPORT', '20240401', 'W', 3],
  ['INQPORT', '20240402', 'S', 233],
  ['RPTPOS00', '20240329', 'S', 12],
  ['RPTPOS00', '20240331', 'S', 13],
  ['RPTPOS00', '20240401', 'S', 12],
  ['RPTPOS00', '20240402', 'S', 12],
  ['RTNANA00', '20240401', 'W', 1],
  ['RTNANA00', '20240402', 'F', 1],
  ['RTNANA00', '20240402', 'S', 3],
];

export const RETURN_CODE_FIXTURE: ReturnCodeEntry[] = ROWS.flatMap(
  ([program, date, status, occurrences]) =>
    Array.from({ length: occurrences }, () => ({
      program,
      date,
      status,
      code: CODE_BY_STATUS[status],
    })),
);
