/**
 * Error Handling types.
 * Migrated from: src/copybook/common/ERRHAND.cpy
 *
 * Centralized error classification, severity levels, VSAM status mapping,
 * and the standard error message structure.
 */

/** Error category codes (level-88). */
export enum ErrorCategory {
  Vsam = 'VS',
  Validation = 'VL',
  Processing = 'PR',
  System = 'SY',
}

/** Error severity levels. */
export enum ErrorSeverity {
  Info = 0,
  Warning = 4,
  Error = 8,
  Severe = 12,
  Critical = 16,
}

/** VSAM file-status to description mapping. */
export const VSAM_STATUS_CODES: Record<string, string> = {
  '00': 'Successful completion',
  '10': 'End of file reached',
  '22': 'Duplicate key detected',
  '23': 'Record not found',
  '35': 'File not found',
};

/** Standard error message structure (ERR-MESSAGE in COBOL). */
export interface ErrorMessage {
  /** PIC X(26) – When the error occurred. */
  errTimestamp: string;
  /** PIC X(8) – Program that raised the error. */
  errProgram: string;
  /** PIC X(2) – VS/VL/PR/SY. */
  errCategory: ErrorCategory | string;
  /** PIC X(4) – Application-specific error code. */
  errCode: string;
  /** PIC S9(4) – Numeric severity (0/4/8/12/16). */
  errSeverity: ErrorSeverity | number;
  /** PIC X(80) – Human-readable error text. */
  errText: string;
  /** PIC X(256) – Extended diagnostic details. */
  errDetails: string;
}

/** Constants from the ERRHAND copybook. */
export const ERR_CAT_VSAM = ErrorCategory.Vsam;
export const ERR_CAT_VALID = ErrorCategory.Validation;
export const ERR_CAT_PROC = ErrorCategory.Processing;
export const ERR_CAT_SYS = ErrorCategory.System;

export const ERR_VSAM_SUCCESS = 'Successful completion';
export const ERR_VSAM_22 = 'Duplicate key detected';
export const ERR_VSAM_23 = 'Record not found';
export const ERR_VSAM_10 = 'End of file reached';
export const ERR_OTHER = 'Unexpected error occurred';
