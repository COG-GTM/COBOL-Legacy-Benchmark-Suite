/**
 * Return Code Management types.
 * Migrated from: src/copybook/common/RTNCODE.cpy
 *
 * Request/response area for the RTNCDE00 return-code handler program.
 */

/** Request type for the return-code handler. */
export enum RcRequestType {
  Init = 'INIT',
  Set = 'SET ',
  Get = 'GET ',
  Log = 'LOG ',
  Analyze = 'ANLZ',
}

/** Return-code severity status. */
export enum RcStatusCode {
  Normal = 'N',
  Warning = 'W',
  Error = 'E',
  Severe = 'S',
}

/** Return-code area passed to/from the handler. */
export interface RcCodesArea {
  /** PIC S9(4) COMP – Current return code. */
  currentCode: number;
  /** PIC S9(4) COMP – Highest code seen so far. */
  highestCode: number;
  /** PIC S9(4) COMP – New code being set. */
  newCode: number;
  /** PIC X(1) – N/W/E/S. */
  statusCode: RcStatusCode | string;
}

/** Analysis / statistics data. */
export interface RcAnalysisData {
  /** PIC X(26) – Start timestamp. */
  startTimestamp: string;
  /** PIC X(26) – End timestamp. */
  endTimestamp: string;
  /** PIC S9(4) COMP – Minimum code observed. */
  minCode: number;
  /** PIC S9(4) COMP – Maximum code observed. */
  maxCode: number;
  /** PIC S9(8) COMP – Total number of code-set operations. */
  totalCount: number;
}

/** Full request area for the return-code handler. */
export interface RcRequestArea {
  /** PIC X(4) – INIT/SET/GET/LOG/ANLZ. */
  requestType: RcRequestType | string;
  codesArea: RcCodesArea;
  analysisData: RcAnalysisData;
}
