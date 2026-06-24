/**
 * Domain types for the Portfolio Master Record.
 *
 * These mirror the COBOL copybook `src/copybook/common/PORTFLIO.cpy`
 * (01 PORT-RECORD). Field names are translated from COBOL hyphenated names to
 * camelCase, but the underlying semantics, lengths, and allowed values are
 * preserved so the structure can later be mapped 1:1 to the backend
 * PORTMSTR / PORTADD / PORTREAD / PORTUPDT / PORTDEL programs.
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

export const CLIENT_TYPES = Object.keys(CLIENT_TYPE_LABELS) as ClientType[];
export const PORTFOLIO_STATUSES = Object.keys(
  PORTFOLIO_STATUS_LABELS,
) as PortfolioStatus[];

/**
 * Maximum field lengths taken directly from the PIC clauses in PORTFLIO.cpy.
 * Used by both the form validation layer and any future API contract.
 */
export const PORTFOLIO_FIELD_LENGTHS = {
  /** PORT-ID PIC X(8) */
  portId: 8,
  /** PORT-ACCOUNT-NO PIC X(10) */
  accountNo: 10,
  /** PORT-CLIENT-NAME PIC X(30) */
  clientName: 30,
  /** PORT-LAST-USER PIC X(8) */
  lastUser: 8,
} as const;

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

/**
 * Fields editable by a user through the create/edit forms. Audit fields
 * (createDate, lastMaintDate, lastUser, lastTransId) are managed by the
 * service layer, mirroring how the COBOL programs stamp them.
 */
export interface PortfolioInput {
  portId: string;
  accountNo: string;
  clientName: string;
  clientType: ClientType;
  status: PortfolioStatus;
  totalValue: string;
  cashBalance: string;
}

/** Search/filter parameters for the list view. */
export interface PortfolioQuery {
  accountNo?: string;
  clientName?: string;
  status?: PortfolioStatus | '';
}
