/**
 * Online (CICS) types.
 * Migrated from:
 *   src/copybook/online/DB2REQ.cpy   – Online DB2 request area
 *   src/copybook/online/ERRHND.cpy   – Online error handling area
 *   src/copybook/online/INQCOM.cpy   – Online inquiry communication area
 *   src/copybook/db2/SQLCA.cpy       – SQL Communication Area status codes
 */

// ── DB2REQ ──────────────────────────────────────────────────────────────

/** Online DB2 request types. */
export enum Db2RequestType {
  Connect = 'C',
  Disconnect = 'D',
  Status = 'S',
}

/** Online DB2 request/response area. */
export interface Db2RequestArea {
  requestType: Db2RequestType | string;
  responseCode: number;
  connectionToken: string;
  sqlcode: number;
  errorMsg: string;
}

// ── ERRHND (online) ─────────────────────────────────────────────────────

/** Severity codes for online errors. */
export enum OnlineErrorSeverity {
  Fatal = 'F',
  Warning = 'W',
  Info = 'I',
}

/** Action to take after an error. */
export enum OnlineErrorAction {
  Return = 'RETURN  ',
  Continue = 'CONTINUE',
  Abend = 'ABEND   ',
}

/** Online error handling area (ERRHND copybook). */
export interface OnlineErrorArea {
  errProgram: string;
  errParagraph: string;
  errSqlcode: number;
  errCicsResp: number;
  errCicsResp2: number;
  errSeverity: OnlineErrorSeverity | string;
  errMessage: string;
  errAction: OnlineErrorAction | string;
  errTraceId: string;
  errTimestamp: string;
}

// ── INQCOM ──────────────────────────────────────────────────────────────

/** Inquiry function codes. */
export enum InquiryFunction {
  Menu = 'MENU',
  InquiryPortfolio = 'INQP',
  InquiryHistory = 'INQH',
  Exit = 'EXIT',
}

/** Inquiry communication area. */
export interface InquiryCommArea {
  /** PIC X(4) – MENU/INQP/INQH/EXIT. */
  function: InquiryFunction | string;
  /** PIC X(10) – Account number entered by user. */
  accountNo: string;
  /** PIC S9(4) – Response code. */
  responseCode: number;
  /** PIC X(80) – Error message to display. */
  errorMsg: string;
}

// ── Recovery request area (used by DB2RECV) ─────────────────────────────

/** Recovery request types. */
export enum RecoveryRequestType {
  Connection = 'C',
  Transaction = 'T',
  Cursor = 'R',
}

/** Recovery status. */
export enum RecoveryStatus {
  Success = 'S',
  Failed = 'F',
  Retry = 'R',
}

/** Recovery request area. */
export interface RecoveryRequestArea {
  requestType: RecoveryRequestType | string;
  responseCode: number;
  sqlcode: number;
  program: string;
  cursor: string;
  message: string;
  status: RecoveryStatus | string;
}

// ── Cursor request area (used by CURSMGR) ───────────────────────────────

/** Cursor operations. */
export enum CursorRequestType {
  Declare = 'D',
  Open = 'O',
  Fetch = 'F',
  Close = 'C',
}

/** Cursor request area. */
export interface CursorRequestArea {
  requestType: CursorRequestType | string;
  name: string;
  statement: string;
  arrayFetch: boolean;
  responseCode: number;
  dataArea: string;
  dataLength: number;
}

// ── Security request area (used by SECMGR) ──────────────────────────────

/** Security request types. */
export enum SecurityRequestType {
  Validate = 'V',
  Authorize = 'A',
  Audit = 'L',
}

/** Security request area. */
export interface SecurityRequestArea {
  requestType: SecurityRequestType | string;
  userId: string;
  resourceName: string;
  accessType: string;
  responseCode: number;
  errorInfo: string;
}
