export type ErrorSeverity = 'I' | 'W' | 'E' | 'S';

export interface ErrorRecord {
  errorCode: string;
  severity: ErrorSeverity;
  message: string;
  programId: string;
  timestamp: string;
  errorType: string;
}
