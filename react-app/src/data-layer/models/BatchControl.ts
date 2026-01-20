/**
 * Batch Control Model
 * Migrated from: BCHCTL.cpy, CKPRST.cpy
 * 
 * Represents batch job control and checkpoint/restart records.
 */

import { BatchStatus, ReturnCode } from '../types';

/**
 * Batch control key structure
 * From: BCHCTL.cpy (BCT-KEY)
 */
export interface BatchControlKey {
  /** Job name (8 characters) */
  jobName: string;
  /** Process date (YYYYMMDD format) */
  processDate: string;
  /** Sequence number (4 digits) */
  sequenceNumber: number;
}

/**
 * Batch process control information
 * From: BCHCTL.cpy (BCT-PROCESS-CONTROL)
 */
export interface BatchProcessControl {
  /** Step name (8 characters) */
  stepName: string;
  /** Program name (8 characters) */
  programName: string;
  /** Start time (HHMMSS format) */
  startTime: string;
  /** End time (HHMMSS format) */
  endTime: string;
}

/**
 * Batch prerequisite job information
 * From: BCHCTL.cpy (BCT-PREREQ-JOBS)
 */
export interface BatchPrerequisite {
  /** Prerequisite job name (8 characters) */
  jobName: string;
  /** Prerequisite sequence number (4 digits) */
  sequenceNumber: number;
  /** Required return code */
  requiredReturnCode: ReturnCode;
}

/**
 * Batch dependencies information
 * From: BCHCTL.cpy (BCT-DEPENDENCIES)
 */
export interface BatchDependencies {
  /** Number of prerequisites */
  prerequisiteCount: number;
  /** List of prerequisite jobs (up to 10) */
  prerequisites: BatchPrerequisite[];
}

/**
 * Batch return information
 * From: BCHCTL.cpy (BCT-RETURN-INFO)
 */
export interface BatchReturnInfo {
  /** Return code */
  returnCode: ReturnCode;
  /** Error description (up to 80 characters) */
  errorDescription: string;
}

/**
 * Batch statistics
 * From: BCHCTL.cpy (BCT-STATISTICS)
 */
export interface BatchStatistics {
  /** Number of restart attempts */
  restartCount: number;
  /** Last attempt timestamp */
  attemptTimestamp: Date;
  /** Completion timestamp */
  completeTimestamp: Date | null;
  /** Records read */
  recordsRead: number;
  /** Records written */
  recordsWritten: number;
  /** Records in error */
  recordsInError: number;
}

/**
 * Complete Batch Control record
 * From: BCHCTL.cpy
 */
export interface BatchControl {
  /** Batch control key */
  key: BatchControlKey;
  /** Batch status */
  status: BatchStatus;
  /** Process control information */
  processControl: BatchProcessControl;
  /** Dependencies information */
  dependencies: BatchDependencies;
  /** Return information */
  returnInfo: BatchReturnInfo;
  /** Statistics */
  statistics: BatchStatistics;
}

/**
 * Checkpoint record
 * From: CKPRST.cpy
 */
export interface CheckpointRecord {
  /** Checkpoint key */
  key: CheckpointKey;
  /** Checkpoint data */
  data: CheckpointData;
  /** Checkpoint statistics */
  statistics: CheckpointStatistics;
}

/**
 * Checkpoint key structure
 * From: CKPRST.cpy
 */
export interface CheckpointKey {
  /** Program ID (8 characters) */
  programId: string;
  /** Process date (YYYYMMDD format) */
  processDate: string;
  /** Checkpoint sequence number */
  sequenceNumber: number;
}

/**
 * Checkpoint data
 * From: CKPRST.cpy
 */
export interface CheckpointData {
  /** Last processed key */
  lastProcessedKey: string;
  /** Last processed record number */
  lastRecordNumber: number;
  /** Checkpoint timestamp */
  checkpointTimestamp: Date;
  /** Checkpoint status */
  status: BatchStatus;
}

/**
 * Checkpoint statistics
 * From: CKPRST.cpy
 */
export interface CheckpointStatistics {
  /** Records processed since last checkpoint */
  recordsProcessed: number;
  /** Errors since last checkpoint */
  errorCount: number;
  /** Elapsed time since last checkpoint (milliseconds) */
  elapsedTime: number;
}

/**
 * Process control record
 * From: PRCSEQ.cpy
 */
export interface ProcessControlRecord {
  /** Process date (YYYYMMDD format) */
  processDate: string;
  /** Process sequence number */
  sequenceNumber: number;
  /** Program ID (8 characters) */
  programId: string;
  /** Program description (up to 30 characters) */
  programDescription: string;
  /** Required return code for success */
  requiredReturnCode: ReturnCode;
  /** Dependency program ID (8 characters) */
  dependencyProgramId: string;
  /** Is restartable */
  isRestartable: boolean;
}

/**
 * Batch job schedule
 * From: data-dictionary.md Job Scheduling Dependencies
 */
export interface BatchJobSchedule {
  /** Job step name */
  jobStep: string;
  /** Prerequisite job step */
  prerequisite: string | null;
  /** Time window start (HHMM format) */
  timeWindowStart: string;
  /** Time window end (HHMM format) */
  timeWindowEnd: string;
  /** Condition for execution */
  condition: string;
}

/**
 * Standard batch job schedules
 * From: data-dictionary.md
 */
export const StandardBatchSchedule: BatchJobSchedule[] = [
  { jobStep: 'TRNVAL00', prerequisite: null, timeWindowStart: '1800', timeWindowEnd: '1815', condition: 'Day must be open' },
  { jobStep: 'POSUPD00', prerequisite: 'TRNVAL00', timeWindowStart: '1815', timeWindowEnd: '1900', condition: 'RC <= 0004' },
  { jobStep: 'HISTLD00', prerequisite: 'POSUPD00', timeWindowStart: '1900', timeWindowEnd: '1930', condition: 'RC <= 0004' },
  { jobStep: 'RPTGEN00', prerequisite: 'HISTLD00', timeWindowStart: '1930', timeWindowEnd: '2000', condition: 'None' },
];

/**
 * Batch control creation request
 */
export interface CreateBatchControlRequest {
  jobName: string;
  processDate: string;
  stepName: string;
  programName: string;
  prerequisites?: BatchPrerequisite[];
}

/**
 * Batch control update request
 */
export interface UpdateBatchControlRequest {
  jobName: string;
  processDate: string;
  sequenceNumber: number;
  status?: BatchStatus;
  returnCode?: ReturnCode;
  errorDescription?: string;
  endTime?: string;
}

/**
 * Batch control search criteria
 */
export interface BatchControlSearchCriteria {
  jobName?: string;
  processDateFrom?: string;
  processDateTo?: string;
  programName?: string;
  status?: BatchStatus;
  returnCode?: ReturnCode;
}

/**
 * Batch control summary for list views
 */
export interface BatchControlSummary {
  jobName: string;
  processDate: string;
  sequenceNumber: number;
  programName: string;
  status: BatchStatus;
  returnCode: ReturnCode;
  startTime: string;
  endTime: string;
}

/**
 * Factory function to create a default BatchControl object
 */
export function createDefaultBatchControl(): BatchControl {
  const now = new Date();
  const dateStr = now.toISOString().slice(0, 10).replace(/-/g, '');

  return {
    key: {
      jobName: '',
      processDate: dateStr,
      sequenceNumber: 1,
    },
    status: BatchStatus.READY,
    processControl: {
      stepName: '',
      programName: '',
      startTime: '',
      endTime: '',
    },
    dependencies: {
      prerequisiteCount: 0,
      prerequisites: [],
    },
    returnInfo: {
      returnCode: ReturnCode.SUCCESS,
      errorDescription: '',
    },
    statistics: {
      restartCount: 0,
      attemptTimestamp: now,
      completeTimestamp: null,
      recordsRead: 0,
      recordsWritten: 0,
      recordsInError: 0,
    },
  };
}

/**
 * Factory function to create a default CheckpointRecord object
 */
export function createDefaultCheckpointRecord(): CheckpointRecord {
  const now = new Date();
  const dateStr = now.toISOString().slice(0, 10).replace(/-/g, '');

  return {
    key: {
      programId: '',
      processDate: dateStr,
      sequenceNumber: 1,
    },
    data: {
      lastProcessedKey: '',
      lastRecordNumber: 0,
      checkpointTimestamp: now,
      status: BatchStatus.ACTIVE,
    },
    statistics: {
      recordsProcessed: 0,
      errorCount: 0,
      elapsedTime: 0,
    },
  };
}

/**
 * Get batch status display name
 */
export function getBatchStatusDisplayName(status: BatchStatus): string {
  switch (status) {
    case BatchStatus.READY:
      return 'Ready';
    case BatchStatus.ACTIVE:
      return 'Active';
    case BatchStatus.WAITING:
      return 'Waiting';
    case BatchStatus.DONE:
      return 'Done';
    case BatchStatus.ERROR:
      return 'Error';
    default:
      return 'Unknown';
  }
}

/**
 * Check if all prerequisites are met for a batch job
 */
export function arePrerequisitesMet(
  batchControl: BatchControl,
  completedJobs: Map<string, ReturnCode>
): boolean {
  for (const prereq of batchControl.dependencies.prerequisites) {
    const jobKey = `${prereq.jobName}-${prereq.sequenceNumber}`;
    const completedReturnCode = completedJobs.get(jobKey);
    
    if (completedReturnCode === undefined) {
      return false; // Job not completed
    }
    
    if (completedReturnCode > prereq.requiredReturnCode) {
      return false; // Return code too high
    }
  }
  
  return true;
}

/**
 * Calculate batch job duration in milliseconds
 */
export function calculateBatchDuration(batchControl: BatchControl): number | null {
  if (!batchControl.processControl.startTime || !batchControl.processControl.endTime) {
    return null;
  }
  
  const startParts = batchControl.processControl.startTime.match(/(\d{2})(\d{2})(\d{2})/);
  const endParts = batchControl.processControl.endTime.match(/(\d{2})(\d{2})(\d{2})/);
  
  if (!startParts || !endParts) {
    return null;
  }
  
  const startMs = (parseInt(startParts[1]) * 3600 + parseInt(startParts[2]) * 60 + parseInt(startParts[3])) * 1000;
  const endMs = (parseInt(endParts[1]) * 3600 + parseInt(endParts[2]) * 60 + parseInt(endParts[3])) * 1000;
  
  return endMs - startMs;
}
