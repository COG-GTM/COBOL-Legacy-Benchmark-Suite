/**
 * TypeScript types migrated from COBOL copybook BCHCTL.cpy
 * These types represent the BATCH-CONTROL-RECORD structure used in BCHCTL00
 */

/**
 * Status values for batch control records
 * Mapped from BCT-STATUS field (PIC X(1))
 */
export type BatchControlStatus = 'R' | 'A' | 'D' | 'W' | 'E';

export const BatchControlStatusLabels: Record<BatchControlStatus, string> = {
  R: 'READY',
  A: 'ACTIVE',
  D: 'DONE',
  W: 'WAITING',
  E: 'ERROR',
};

/**
 * Prerequisite job information
 * Mapped from BCT-PREREQ-JOBS (OCCURS 10 TIMES)
 */
export interface PrerequisiteJob {
  /** BCT-PREREQ-NAME - PIC X(8) */
  name: string;
  /** BCT-PREREQ-SEQ - PIC 9(4) */
  sequenceNo: number;
  /** BCT-PREREQ-RC - PIC S9(4) COMP */
  returnCode: number;
}

/**
 * Process control information
 * Mapped from BCT-PROCESS-CONTROL
 */
export interface ProcessControl {
  /** BCT-STEP-NAME - PIC X(8) */
  stepName: string;
  /** BCT-PROGRAM-NAME - PIC X(8) */
  programName: string;
  /** BCT-START-TIME - PIC X(8) */
  startTime: string;
  /** BCT-END-TIME - PIC X(8) */
  endTime: string;
}

/**
 * Return information
 * Mapped from BCT-RETURN-INFO
 */
export interface ReturnInfo {
  /** BCT-RETURN-CODE - PIC S9(4) COMP */
  returnCode: number;
  /** BCT-ERROR-DESC - PIC X(80) */
  errorDescription: string;
}

/**
 * Statistics information
 * Mapped from BCT-STATISTICS
 */
export interface BatchStatistics {
  /** BCT-RESTART-COUNT - PIC 9(2) COMP */
  restartCount: number;
  /** BCT-ATTEMPT-TS - PIC X(26) */
  attemptTimestamp: string;
  /** BCT-COMPLETE-TS - PIC X(26) */
  completeTimestamp: string;
}

/**
 * Primary key for batch control record
 * Mapped from BCT-KEY
 */
export interface BatchControlKey {
  /** BCT-JOB-NAME - PIC X(8) */
  jobName: string;
  /** BCT-PROCESS-DATE - PIC X(8) */
  processDate: string;
  /** BCT-SEQUENCE-NO - PIC 9(4) */
  sequenceNo: number;
}

/**
 * Complete batch control record
 * Mapped from BATCH-CONTROL-RECORD in BCHCTL.cpy
 */
export interface BatchControlRecord {
  key: BatchControlKey;
  status: BatchControlStatus;
  processControl: ProcessControl;
  dependencies: {
    prerequisiteCount: number;
    prerequisiteJobs: PrerequisiteJob[];
  };
  returnInfo: ReturnInfo;
  statistics: BatchStatistics;
}

/**
 * Initialization step status
 */
export type InitializationStepStatus = 'pending' | 'in_progress' | 'completed' | 'error';

/**
 * Initialization step information
 */
export interface InitializationStep {
  id: string;
  name: string;
  description: string;
  status: InitializationStepStatus;
  errorMessage?: string;
}

/**
 * Control request parameters
 * Mapped from LS-CONTROL-REQUEST in BCHCTL00
 */
export interface ControlRequest {
  /** LS-FUNCTION - PIC X(4) */
  function: 'INIT' | 'CHEK' | 'UPDT' | 'TERM';
  /** LS-JOB-NAME - PIC X(8) */
  jobName: string;
  /** LS-PROCESS-DATE - PIC X(8) */
  processDate: string;
  /** LS-SEQUENCE-NO - PIC 9(4) */
  sequenceNo: number;
}

/**
 * Control response
 */
export interface ControlResponse {
  /** LS-RETURN-CODE - PIC S9(4) COMP */
  returnCode: number;
  record?: BatchControlRecord;
  errorMessage?: string;
}

/**
 * Return codes
 * Mapped from BCHCON copybook constants
 */
export const ReturnCodes = {
  SUCCESS: 0,
  WARNING: 4,
  ERROR: 8,
  SEVERE: 12,
} as const;
