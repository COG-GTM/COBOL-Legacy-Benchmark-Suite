/**
 * Domain types for the History Record.
 *
 * These mirror the COBOL copybook `src/copybook/common/HISTREC.cpy`
 * (01 HISTORY-RECORD). Field names are translated from COBOL hyphenated names
 * to camelCase, but the underlying semantics, lengths, and allowed values are
 * preserved so the structure can later be mapped 1:1 to the backend INQHIST
 * program / POSHIST table.
 */

import type { TransactionType } from './transaction';

/** HIST-RECORD-TYPE (X(2)) — 88-level values from HISTREC.cpy. */
export type HistoryRecordType = 'PT' | 'PS' | 'TR';

/** HIST-ACTION-CODE (X(1)) — 88-level values from HISTREC.cpy. */
export type HistoryActionCode = 'A' | 'C' | 'D';

export const HISTORY_RECORD_TYPE_LABELS: Record<HistoryRecordType, string> = {
  PT: 'Portfolio',
  PS: 'Position',
  TR: 'Transaction',
};

export const HISTORY_RECORD_TYPES = Object.keys(
  HISTORY_RECORD_TYPE_LABELS,
) as HistoryRecordType[];

export const HISTORY_ACTION_LABELS: Record<HistoryActionCode, string> = {
  A: 'Add',
  C: 'Change',
  D: 'Delete',
};

/**
 * A single history (audit trail) entry.
 *
 * The monetary/quantity fields are carried as plain decimal strings (see
 * utils/decimal.ts): the legacy amounts come from `S9(11)V9(4)` and
 * `S9(13)V9(2)` COMP-3 fields, which exceed the safe-integer range of an
 * IEEE-754 double once scaled.
 */
export interface HistoryRecord {
  /** HIST-PORTFOLIO-ID PIC X(8) — part of HIST-KEY. */
  portfolioId: string;
  /** HIST-DATE PIC X(8) — YYYYMMDD, part of HIST-KEY. */
  date: string;
  /** HIST-TIME PIC X(6) — HHMMSS, part of HIST-KEY. */
  time: string;
  /** HIST-SEQ-NO PIC X(4) — part of HIST-KEY. */
  seqNo: string;
  /** HIST-RECORD-TYPE. */
  recordType: HistoryRecordType;
  /** HIST-ACTION-CODE. */
  actionCode: HistoryActionCode;
  /** HIST-REASON-CODE PIC X(4). */
  reasonCode: string;
  /** HIST-BEFORE-IMAGE PIC X(400) — empty for 'A' (add) actions. */
  beforeImage: string;
  /** HIST-AFTER-IMAGE PIC X(400) — empty for 'D' (delete) actions. */
  afterImage: string;
  /** HIST-PROCESS-DATE PIC X(26). */
  processDate: string;
  /** HIST-PROCESS-USER PIC X(8). */
  processUser: string;
  /**
   * The dealt figures behind the change, as displayed on the legacy HISMAP
   * columns (Units / Price / Amount, fetched by INQHIST from POSHIST). They
   * are decoded from the record images, so they are only populated for the
   * position and transaction rows that carry them; portfolio-level changes
   * (record type PT) leave them null.
   */
  investmentId: string | null;
  /** TRN-TYPE of the underlying transaction, when this row has one. */
  transactionType: TransactionType | null;
  /** Units dealt — decimal string with up to 4 fraction digits. */
  units: string | null;
  /** Unit price — decimal string with up to 4 fraction digits. */
  price: string | null;
  /** Settlement amount — decimal string with 2 fraction digits. */
  amount: string | null;
  /** TRN-CURRENCY PIC X(3) — ISO currency code. */
  currency: string | null;
}

/** Search/filter parameters for the history inquiry. */
export interface HistoryQuery {
  /** Inclusive lower bound on HIST-DATE, as YYYYMMDD. */
  startDate?: string;
  /** Inclusive upper bound on HIST-DATE, as YYYYMMDD. */
  endDate?: string;
  /** Filter by HIST-RECORD-TYPE; empty string means "all types". */
  recordType?: HistoryRecordType | '';
}

/**
 * The 26-character HIST-KEY (portfolio id + date + time + sequence number),
 * used as the stable identity of a history row in URLs and React keys.
 */
export function historyKey(record: HistoryRecord): string {
  return `${record.portfolioId}${record.date}${record.time}${record.seqNo}`;
}
