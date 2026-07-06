/**
 * TypeScript interfaces derived directly from the CLBS COBOL copybook field
 * layouts. Each field documents the originating copybook, the COBOL PIC clause,
 * and how the value is represented on the modern side.
 *
 * COBOL numeric convention note:
 *   COMP-3 (packed-decimal) fields with an implied decimal point (the `V` in a
 *   PIC clause, e.g. `S9(13)V99`) carry no physical decimal character on the
 *   mainframe. When such a field is exposed as JSON it is materialized as a
 *   real decimal number, so on the TypeScript side it is modelled as `number`
 *   with the implied scale already applied (e.g. `12345678.99`, not
 *   `1234567899`). A production z/OS Connect / CICS Web Services layer is
 *   responsible for applying that scale during marshalling.
 */

/** PORT-CLIENT-TYPE PIC X(1) — copybook PORTFLIO.cpy (88-levels). */
export type ClientType =
  | 'I' // PORT-INDIVIDUAL
  | 'C' // PORT-CORPORATE
  | 'T'; // PORT-TRUST

/** PORT-STATUS PIC X(1) — copybook PORTFLIO.cpy (88-levels). */
export type PortfolioStatus =
  | 'A' // PORT-ACTIVE
  | 'C' // PORT-CLOSED
  | 'S'; // PORT-SUSPENDED

/** POS-STATUS PIC X(1) — copybook POSREC.cpy (88-levels). */
export type PositionStatus =
  | 'A' // POS-STATUS-ACTIVE
  | 'C' // POS-STATUS-CLOSED
  | 'P'; // POS-STATUS-PEND

/**
 * TRANS_TYPE — copybook TRNREC.cpy (88-levels) / DB2 POSHIST.TRANS_TYPE CHAR(2).
 * BU=Buy, SL=Sell, TR=Transfer, FE=Fee.
 */
export type TransactionType = 'BU' | 'SL' | 'TR' | 'FE';

/**
 * Portfolio Master Record — copybook PORTFLIO.cpy (VSAM KSDS PORT-RECORD).
 */
export interface Portfolio {
  /** PORT-ID PIC X(8) */
  portfolioId: string;
  /** PORT-ACCOUNT-NO PIC X(10) */
  accountNo: string;
  /** PORT-CLIENT-NAME PIC X(30) */
  clientName: string;
  /** PORT-CLIENT-TYPE PIC X(1) */
  clientType: ClientType;
  /** PORT-CREATE-DATE PIC 9(8) — YYYYMMDD */
  createDate: string;
  /** PORT-LAST-MAINT PIC 9(8) — YYYYMMDD */
  lastMaintDate: string;
  /** PORT-STATUS PIC X(1) */
  status: PortfolioStatus;
  /** PORT-TOTAL-VALUE PIC S9(13)V99 COMP-3 — implied 2 decimals */
  totalValue: number;
  /** PORT-CASH-BALANCE PIC S9(13)V99 COMP-3 — implied 2 decimals */
  cashBalance: number;
}

/**
 * Position Record — copybook POSREC.cpy (POSITION-RECORD).
 *
 * `fundName` has no copybook field of its own: the BMS POSMAP screen shows a
 * `NAMEOUT` fund-name field alongside the position, so it is carried here as a
 * display-only attribute (a real backend would resolve it from a security /
 * fund master keyed by `investmentId`).
 */
export interface Position {
  /** POS-PORTFOLIO-ID PIC X(8) */
  portfolioId: string;
  /** POS-DATE PIC X(8) — YYYYMMDD */
  date: string;
  /** POS-INVESTMENT-ID PIC X(10) — shown as "Fund ID" on POSMAP */
  investmentId: string;
  /** Display-only fund name (BMS POSMAP NAMEOUT); not in POSREC.cpy */
  fundName: string;
  /** POS-QUANTITY PIC S9(11)V9(4) COMP-3 — implied 4 decimals; "Units" */
  quantity: number;
  /** POS-COST-BASIS PIC S9(13)V9(2) COMP-3 — implied 2 decimals */
  costBasis: number;
  /** POS-MARKET-VALUE PIC S9(13)V9(2) COMP-3 — implied 2 decimals */
  marketValue: number;
  /** POS-CURRENCY PIC X(3) */
  currency: string;
  /** POS-STATUS PIC X(1) */
  status: PositionStatus;
}

/**
 * Transaction history row — the exact projection selected by INQHIST.cbl from
 * the DB2 POSHIST table:
 *   SELECT TRANS_DATE, TRANS_TYPE, TRANS_UNITS, TRANS_PRICE, TRANS_AMOUNT
 *   FROM POSHIST WHERE ACCOUNT_NO = ? ORDER BY TRANS_DATE DESC
 *
 * Scale of the numeric columns follows the INQHIST working-storage host
 * variables (WS-TRANS-UNITS / WS-TRANS-PRICE / WS-TRANS-AMOUNT), all
 * PIC S9(9)V99 COMP-3 — implied 2 decimals.
 */
export interface Transaction {
  /** TRANS_DATE — DB2 DATE, rendered YYYY-MM-DD */
  transDate: string;
  /** TRANS_TYPE — CHAR(2) */
  transType: TransactionType;
  /** TRANS_UNITS — WS-TRANS-UNITS PIC S9(9)V99 COMP-3 */
  transUnits: number;
  /** TRANS_PRICE — WS-TRANS-PRICE PIC S9(9)V99 COMP-3 */
  transPrice: number;
  /** TRANS_AMOUNT — WS-TRANS-AMOUNT PIC S9(9)V99 COMP-3 */
  transAmount: number;
}

/**
 * Response for GET /api/portfolios/{account}.
 * Mirrors the CICS flow where INQPORT reads the portfolio/position for an
 * account and the BMS POSMAP renders it.
 */
export interface PortfolioResponse {
  portfolio: Portfolio;
  positions: Position[];
}

/**
 * Response for GET /api/portfolios/{account}/transactions?page=.
 * Page size is fixed at 10 to mirror the COBOL fetch of 10 rows
 * (INQHIST WS-HISTORY-ENTRY OCCURS 10 TIMES).
 */
export interface TransactionsResponse {
  account: string;
  page: number;
  pageSize: number;
  total: number;
  totalPages: number;
  transactions: Transaction[];
}

/** Shape of the JSON error body returned by the mock API. */
export interface ApiErrorBody {
  /** Mirrors INQCOM-ERROR-MSG (BMS ERRMAP / POSMSG). */
  message: string;
  /** Mirrors INQCOM-RESPONSE-CODE where relevant. */
  code?: string;
}

/** Human-readable labels for the COBOL 88-level condition names. */
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

export const POSITION_STATUS_LABELS: Record<PositionStatus, string> = {
  A: 'Active',
  C: 'Closed',
  P: 'Pending',
};

export const TRANSACTION_TYPE_LABELS: Record<TransactionType, string> = {
  BU: 'Buy',
  SL: 'Sell',
  TR: 'Transfer',
  FE: 'Fee',
};

/** Page size mirroring INQHIST WS-HISTORY-ENTRY OCCURS 10 TIMES. */
export const TRANSACTION_PAGE_SIZE = 10;
