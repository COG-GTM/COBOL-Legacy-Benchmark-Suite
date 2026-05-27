/**
 * Return Handling types.
 * Migrated from: src/copybook/common/RETHND.cpy
 *
 * Rich return-status structure with location, diagnostic, and retry information.
 */

/** Standard error codes (E001–E010). */
export enum StandardErrorCode {
  E001 = 'E001',
  E002 = 'E002',
  E003 = 'E003',
  E004 = 'E004',
  E005 = 'E005',
  E006 = 'E006',
  E007 = 'E007',
  E008 = 'E008',
  E009 = 'E009',
  E010 = 'E010',
}

/** Top-level return status. */
export interface ReturnStatus {
  /** PIC S9(4) COMP – Primary return code. */
  returnCode: number;
  /** PIC S9(4) COMP – Secondary reason code. */
  reasonCode: number;
  /** PIC X(8) – Module that set the code. */
  moduleId: string;
  /** PIC X(8) – Function within the module. */
  functionId: string;
}

/** Where the error occurred. */
export interface ErrorLocation {
  /** PIC X(8) – Program name. */
  programName: string;
  /** PIC X(30) – Paragraph or method name. */
  paragraphName: string;
  /** PIC X(8) – Statement reference. */
  statementId: string;
}

/** Diagnostic information. */
export interface ErrorInfo {
  /** PIC X(4) – E001-E010 or custom. */
  errorCode: string;
  /** PIC X(80) – Human-readable message. */
  errorMessage: string;
  /** PIC X(256) – Extended details. */
  errorDetails: string;
}

/** System-level context. */
export interface SystemInfo {
  /** PIC S9(9) COMP – SQL code (if DB2 related). */
  sqlCode: number;
  /** PIC X(5) – SQL state (if DB2 related). */
  sqlState: string;
  /** PIC X(2) – VSAM file status. */
  fileStatus: string;
}

/** Retry control parameters. */
export interface RetryControl {
  /** PIC S9(4) COMP – Current attempt number. */
  retryCount: number;
  /** PIC S9(4) COMP – Maximum allowed retries. */
  maxRetries: number;
  /** PIC S9(4) COMP – Seconds between retries. */
  retryWait: number;
}

/** Full RETURN-DETAILS structure. */
export interface ReturnDetails {
  location: ErrorLocation;
  errorInfo: ErrorInfo;
  systemInfo: SystemInfo;
  retryControl: RetryControl;
}

/** Combined return handling area. */
export interface ReturnHandlingArea {
  status: ReturnStatus;
  details: ReturnDetails;
}
