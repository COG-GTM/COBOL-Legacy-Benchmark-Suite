export type BatchStatus = 'P' | 'R' | 'C' | 'F' | 'A';

export interface BatchControl {
  processDate: string;
  processId: string;
  status: BatchStatus;
  startTime?: string;
  endTime?: string;
  recordCount: number;
  errorCount: number;
  returnCode: number;
  message: string;
}
