/**
 * Domain types for the Portfolio Master Record.
 *
 * These mirror the COBOL copybook `src/copybook/common/PORTFLIO.cpy`
 * (01 PORT-RECORD). Field names are translated from COBOL hyphenated names to
 * camelCase, but the underlying semantics, lengths, and allowed values are
 * preserved so the structure can later be mapped 1:1 to the backend
 * PORTMSTR / PORTREAD programs.
 */

/** PORT-CLIENT-TYPE (X(1)) — 88-level values from PORTFLIO.cpy. */
export type ClientType = 'I' | 'C' | 'T';

/** PORT-STATUS (X(1)) — 88-level values from PORTFLIO.cpy. */
export type PortfolioStatus = 'A' | 'C' | 'S';

export const CLIENT_TYPE_LABELS: Record<ClientType, string> = {
  I: 'Individual',
  C: 'Corporate',
  T: 'Trust',
};

export const PORTFOLIO_STATUS_LABELS: Record<PortfolioStatus, string> = {
  A: 'Active',
  C: 'Closed',
  S: 'Suspended',
};

/**
 * Monetary fields are stored as plain decimal strings (e.g. "1234567.89")
 * rather than JS numbers. PORT-TOTAL-VALUE / PORT-CASH-BALANCE are
 * `S9(13)V99 COMP-3` — up to 13 integer digits plus 2 decimals, which exceeds
 * the safe-integer range of an IEEE-754 double once scaled to cents. Keeping
 * the raw decimal string preserves COMP-3 packed-decimal precision end to end.
 */
export interface Portfolio {
  /** PORT-ID — primary key (PORT-KEY). */
  portId: string;
  /** PORT-ACCOUNT-NO. */
  accountNo: string;
  /** PORT-CLIENT-NAME. */
  clientName: string;
  /** PORT-CLIENT-TYPE. */
  clientType: ClientType;
  /** PORT-CREATE-DATE PIC 9(8) — YYYYMMDD. */
  createDate: string;
  /** PORT-LAST-MAINT PIC 9(8) — YYYYMMDD. */
  lastMaintDate: string;
  /** PORT-STATUS. */
  status: PortfolioStatus;
  /** PORT-TOTAL-VALUE — signed decimal string with 2 fraction digits. */
  totalValue: string;
  /** PORT-CASH-BALANCE — signed decimal string with 2 fraction digits. */
  cashBalance: string;
  /** PORT-LAST-USER. */
  lastUser: string;
  /** PORT-LAST-TRANS PIC 9(8). */
  lastTransId: string;
}
