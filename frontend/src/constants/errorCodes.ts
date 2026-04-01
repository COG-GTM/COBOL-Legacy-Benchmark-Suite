/**
 * Legacy COBOL response codes from SECMGR.cbl (lines 29-39).
 *
 * 0  = Success
 * 8  = Business error (access denied, not found)
 * 12 = System error (system failure)
 */
export const ErrorCode = {
  SUCCESS: 0,
  BUSINESS_ERROR: 8,
  SYSTEM_ERROR: 12,
} as const;

export type ErrorCode = (typeof ErrorCode)[keyof typeof ErrorCode];

/** Human-readable labels for each legacy error code. */
export const ERROR_CODE_LABELS: Record<ErrorCode, string> = {
  [ErrorCode.SUCCESS]: 'Success',
  [ErrorCode.BUSINESS_ERROR]: 'Business Error',
  [ErrorCode.SYSTEM_ERROR]: 'System Error',
};
