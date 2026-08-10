/**
 * Domain types for the Transaction Record.
 *
 * These mirror the COBOL copybook `src/copybook/common/TRNREC.cpy`
 * (01 TRANSACTION-RECORD). Field names are translated from COBOL hyphenated
 * names to camelCase, but the underlying semantics, lengths, and allowed values
 * are preserved so the structure can later be mapped 1:1 to the backend
 * PORTTRAN / PORTVALD programs and the TRANFILE records they read.
 */

/** TRN-TYPE (X(2)) — 88-level values from TRNREC.cpy. */
export type TransactionType = 'BU' | 'SL' | 'TR' | 'FE';

/** TRN-STATUS (X(1)) — 88-level values from TRNREC.cpy. */
export type TransactionStatus = 'P' | 'D' | 'F' | 'R';

export const TRANSACTION_TYPE_LABELS: Record<TransactionType, string> = {
  BU: 'Buy',
  SL: 'Sell',
  TR: 'Transfer',
  FE: 'Fee',
};

export const TRANSACTION_TYPES = Object.keys(
  TRANSACTION_TYPE_LABELS,
) as TransactionType[];

/**
 * Types a user may submit from the web form. PORTTRAN also accepts 'FE' (fee),
 * but fees are raised by the batch fee-assessment job rather than entered by
 * hand, so the form offers BUY / SELL / TRANSFER only. Fee records still appear
 * in the status view.
 */
export const SUBMITTABLE_TRANSACTION_TYPES: TransactionType[] = [
  'BU',
  'SL',
  'TR',
];

export const TRANSACTION_STATUS_LABELS: Record<TransactionStatus, string> = {
  P: 'Pending',
  D: 'Done',
  F: 'Failed',
  R: 'Reversed',
};

export const TRANSACTION_STATUSES = Object.keys(
  TRANSACTION_STATUS_LABELS,
) as TransactionStatus[];

/** Maximum field lengths taken directly from the PIC clauses in TRNREC.cpy. */
export const TRANSACTION_FIELD_LENGTHS = {
  /** TRN-DATE PIC X(8) */
  date: 8,
  /** TRN-TIME PIC X(6) */
  time: 6,
  /** TRN-PORTFOLIO-ID PIC X(8) */
  portfolioId: 8,
  /** TRN-SEQUENCE-NO PIC X(6) */
  sequenceNo: 6,
  /** TRN-INVESTMENT-ID PIC X(10) */
  investmentId: 10,
  /** TRN-CURRENCY PIC X(3) */
  currency: 3,
  /** TRN-PROCESS-USER PIC X(8) */
  processUser: 8,
} as const;

/**
 * COMP-3 scales from TRNREC.cpy, used by the validation and amount-calculation
 * layers so entered values can never exceed what the packed-decimal fields
 * hold.
 */
export const TRANSACTION_DECIMALS = {
  /** TRN-QUANTITY PIC S9(11)V9(4) COMP-3 */
  quantity: { maxIntDigits: 11, maxFracDigits: 4 },
  /** TRN-PRICE PIC S9(11)V9(4) COMP-3 */
  price: { maxIntDigits: 11, maxFracDigits: 4 },
  /** TRN-AMOUNT PIC S9(13)V9(2) COMP-3 */
  amount: { maxIntDigits: 13, maxFracDigits: 2 },
} as const;

/** ISO codes offered for TRN-CURRENCY PIC X(3). */
export const TRANSACTION_CURRENCIES = [
  'USD',
  'EUR',
  'GBP',
  'JPY',
  'CHF',
  'CAD',
] as const;

/**
 * A single transaction record.
 *
 * Quantity, price and amount are carried as plain decimal strings (e.g.
 * "1234567.89") rather than JS numbers: TRN-AMOUNT is `S9(13)V9(2)` and
 * TRN-QUANTITY / TRN-PRICE are `S9(11)V9(4)`, all of which exceed the
 * safe-integer range of an IEEE-754 double once scaled, so keeping the raw
 * decimal string preserves COMP-3 packed-decimal precision end to end
 * (see utils/decimal.ts).
 */
export interface Transaction {
  /** TRN-DATE PIC X(8) — YYYYMMDD, part of TRN-KEY. */
  date: string;
  /** TRN-TIME PIC X(6) — HHMMSS, part of TRN-KEY. */
  time: string;
  /** TRN-PORTFOLIO-ID PIC X(8) — part of TRN-KEY. */
  portfolioId: string;
  /** TRN-SEQUENCE-NO PIC X(6) — part of TRN-KEY. */
  sequenceNo: string;
  /** TRN-INVESTMENT-ID PIC X(10). */
  investmentId: string;
  /** TRN-TYPE. */
  type: TransactionType;
  /** TRN-QUANTITY — signed decimal string with up to 4 fraction digits. */
  quantity: string;
  /** TRN-PRICE — signed decimal string with up to 4 fraction digits. */
  price: string;
  /** TRN-AMOUNT — signed decimal string with 2 fraction digits. */
  amount: string;
  /** TRN-CURRENCY PIC X(3) — ISO currency code. */
  currency: string;
  /** TRN-STATUS. */
  status: TransactionStatus;
  /** TRN-PROCESS-DATE PIC X(26). */
  processDate: string;
  /** TRN-PROCESS-USER PIC X(8). */
  processUser: string;
}

/**
 * Fields captured by the submission form. The TRN-KEY components (date, time,
 * sequence number), TRN-AMOUNT and the TRN-AUDIT fields are stamped by the
 * service layer, mirroring how PORTTRAN writes them.
 */
export interface TransactionInput {
  portfolioId: string;
  investmentId: string;
  type: TransactionType;
  quantity: string;
  price: string;
  currency: string;
}

/** Search/filter parameters for the transaction status view. */
export interface TransactionQuery {
  /** Filter by TRN-PORTFOLIO-ID; empty string means "all portfolios". */
  portfolioId?: string;
  /** Filter by TRN-STATUS; empty string means "all statuses". */
  status?: TransactionStatus | '';
  /** Filter by TRN-TYPE; empty string means "all types". */
  type?: TransactionType | '';
}
