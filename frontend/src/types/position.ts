/**
 * Domain types for the Position Record.
 *
 * These mirror the COBOL copybook `src/copybook/common/POSREC.cpy`
 * (01 POSITION-RECORD). Field names are translated from COBOL hyphenated names
 * to camelCase, but the underlying semantics, lengths, and allowed values are
 * preserved so the structure can later be mapped 1:1 to the backend INQPORT
 * program / POSFILE VSAM records.
 */

/** POS-STATUS (X(1)) — 88-level values from POSREC.cpy. */
export type PositionStatus = 'A' | 'C' | 'P';

export const POSITION_STATUS_LABELS: Record<PositionStatus, string> = {
  A: 'Active',
  C: 'Closed',
  P: 'Pending',
};

export const POSITION_STATUSES = Object.keys(
  POSITION_STATUS_LABELS,
) as PositionStatus[];

/**
 * A single portfolio position.
 *
 * Monetary and quantity fields are carried as plain decimal strings (e.g.
 * "1234567.89") rather than JS numbers. POS-COST-BASIS / POS-MARKET-VALUE are
 * `S9(13)V9(2) COMP-3` and POS-QUANTITY is `S9(11)V9(4) COMP-3` — both exceed
 * the safe-integer range of an IEEE-754 double once scaled, so keeping the raw
 * decimal string preserves COMP-3 packed-decimal precision end to end
 * (see utils/decimal.ts).
 */
export interface Position {
  /** POS-PORTFOLIO-ID PIC X(8) — part of POS-KEY. */
  portfolioId: string;
  /** POS-DATE PIC X(8) — YYYYMMDD, part of POS-KEY. */
  date: string;
  /** POS-INVESTMENT-ID PIC X(10) — part of POS-KEY (the "Fund ID"). */
  investmentId: string;
  /**
   * Human-readable fund name for display.
   *
   * NOTE: POSREC.cpy has no fund-name field — POS-INVESTMENT-ID is the only
   * fund identifier. The name is an enrichment that would come from an
   * investment/security master file in production; it is supplied by the
   * fixture here so the inquiry table can show the "Fund Name" column.
   */
  fundName: string;
  /** POS-QUANTITY — signed decimal string with up to 4 fraction digits. */
  quantity: string;
  /** POS-COST-BASIS — signed decimal string with 2 fraction digits. */
  costBasis: string;
  /** POS-MARKET-VALUE — signed decimal string with 2 fraction digits. */
  marketValue: string;
  /** POS-CURRENCY PIC X(3) — ISO currency code. */
  currency: string;
  /** POS-STATUS. */
  status: PositionStatus;
  /** POS-LAST-MAINT-DATE PIC X(26). */
  lastMaintDate: string;
  /** POS-LAST-MAINT-USER PIC X(8). */
  lastMaintUser: string;
}

/** Search/filter parameters for the position inquiry. */
export interface PositionQuery {
  /** Filter by POS-STATUS; empty string means "all statuses". */
  status?: PositionStatus | '';
}
