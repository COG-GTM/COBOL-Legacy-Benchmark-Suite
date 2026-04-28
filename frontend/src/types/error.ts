/** Maps to ERRHAND.cpy - ERR-MESSAGE */
export interface AppError {
  date: string;          // ERR-DATE PIC X(10)
  time: string;          // ERR-TIME PIC X(8)
  program: string;       // ERR-PROGRAM PIC X(8)
  category: ErrorCategory; // ERR-CATEGORY PIC X(2)
  code: string;          // ERR-CODE PIC X(4)
  severity: number;      // ERR-SEVERITY S9(4)
  text: string;          // ERR-TEXT PIC X(80)
  details: string;       // ERR-DETAILS PIC X(256)
}

export type ErrorCategory = 'VS' | 'VL' | 'PR' | 'SY';
export const ERROR_CATEGORY_LABELS: Record<ErrorCategory, string> = {
  VS: 'VSAM Error',
  VL: 'Validation Error',
  PR: 'Processing Error',
  SY: 'System Error',
};

/** VSAM status codes from ERRHAND.cpy */
export const VSAM_STATUS = {
  SUCCESS: '00',
  DUPLICATE_KEY: '22',
  NOT_FOUND: '23',
  EOF: '10',
} as const;

export const VSAM_STATUS_MESSAGES: Record<string, string> = {
  '00': 'Operation completed successfully',
  '22': 'A record with this key already exists',
  '23': 'The requested record was not found',
  '10': 'End of file reached',
};

/** Maps to INQCOM.cpy - INQCOM-AREA */
export interface InquiryCommArea {
  function: 'MENU' | 'INQP' | 'INQH' | 'EXIT';
  accountNumber: string;  // INQCOM-ACCOUNT-NO PIC X(10)
  responseCode: number;   // INQCOM-RESPONSE-CODE S9(8)
  errorMessage: string;   // INQCOM-ERROR-MSG PIC X(80)
}

/** Delete reason codes from PORTDEL.cbl */
export type DeleteReasonCode = '01' | '02' | '03';
export const DELETE_REASON_LABELS: Record<DeleteReasonCode, string> = {
  '01': 'Account Closed',
  '02': 'Transferred',
  '03': 'Client Requested',
};
