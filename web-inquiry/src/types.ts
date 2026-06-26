/**
 * Domain + session types for the CLBS online inquiry subsystem (PINQ / INQONLN).
 *
 * These mirror the legacy COBOL artifacts:
 *  - INQCOM.cpy  -> InqCom (the CICS COMMAREA passed between programs)
 *  - POSREC.cpy  -> Position (portfolio position record)
 *  - INQHIST     -> Transaction (a single transaction-history row)
 */

/** INQCOM-FUNCTION 88-levels: MENU / INQP / INQH / EXIT. */
export type InqFunction = 'MENU' | 'INQP' | 'INQH' | 'EXIT';

/**
 * Client-side analog of the INQCOM-AREA COMMAREA.
 * INQCOM.cpy:
 *   05 INQCOM-FUNCTION       PIC X(4)
 *   05 INQCOM-ACCOUNT-NO     PIC X(10)
 *   05 INQCOM-RESPONSE-CODE  PIC S9(8) COMP
 *   05 INQCOM-ERROR-MSG      PIC X(80)
 */
export interface InqCom {
  function: InqFunction;
  accountNo: string;
  responseCode: number;
  errorMsg: string;
}

/** Portfolio position (POSREC.cpy + POSMAP display fields). */
export interface Position {
  /** INQCOM-ACCOUNT-NO key (PIC X(10)). */
  accountNo: string;
  /** FUNDOUT, PIC X(6). */
  fundId: string;
  /** NAMEOUT, PIC X(30). */
  fundName: string;
  /** POS-QUANTITY, S9(11)V9(4). */
  units: number;
  /** POS-COST-BASIS, S9(13)V9(2). */
  costBasis: number;
  /** POS-MARKET-VALUE, S9(13)V9(2). */
  marketValue: number;
  /** POS-CURRENCY, PIC X(3). */
  currency: string;
  /** POS-STATUS: A=Active, C=Closed, P=Pending. */
  status: 'A' | 'C' | 'P';
}

/** A transaction-history row (INQHIST WS-HISTORY-ENTRY / HISMAP columns). */
export interface Transaction {
  /** WS-TRANS-DATE, PIC X(10) — ISO yyyy-mm-dd. */
  date: string;
  /** WS-TRANS-TYPE, PIC X(4). */
  type: string;
  /** WS-TRANS-UNITS, S9(9)V99 COMP-3. */
  units: number;
  /** WS-TRANS-PRICE, S9(9)V99 COMP-3. */
  price: number;
  /** WS-TRANS-AMOUNT, S9(9)V99 COMP-3. */
  amount: number;
}

/** Outcome of a position lookup, mirroring INQPORT NORMAL / NOTFND / ERROR. */
export type PositionResult =
  | { status: 'FOUND'; position: Position }
  | { status: 'NOT_FOUND'; errorMsg: string }
  | { status: 'ERROR'; responseCode: number; errorMsg: string };

/** A single page of transaction history (INQHIST 10-rows-per-page paging). */
export interface HistoryPage {
  rows: Transaction[];
  page: number;
  pageSize: number;
  totalRows: number;
  totalPages: number;
  hasPrevious: boolean;
  hasNext: boolean;
}

/** Outcome of a history lookup. */
export type HistoryResult =
  | { status: 'OK'; page: HistoryPage }
  | { status: 'ERROR'; responseCode: number; errorMsg: string };

/** Result of the stubbed SECMGR USERID check. */
export type AuthResult =
  | { status: 'OK'; userId: string }
  | { status: 'DENIED'; errorMsg: string };
