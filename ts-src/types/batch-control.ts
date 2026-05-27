/**
 * Batch Control Record types.
 * Migrated from: src/copybook/batch/BCHCTL.cpy
 *
 * Job-level control record with dependency tracking and timing.
 */

import { BatchStatus, DependencyType } from './batch-constants';

/** Composite key for a batch control record. */
export interface BatchControlKey {
  /** PIC X(8) – Job name. */
  bctJobName: string;
  /** PIC X(8) – Process date (YYYYMMDD). */
  bctProcessDate: string;
  /** PIC 9(4) – Sequence number within date/job. */
  bctSequenceNo: number;
}

/** Process control information. */
export interface BatchProcessControl {
  /** PIC X(8) – Current step name. */
  bctStepName: string;
  /** PIC X(8) – Program being executed. */
  bctProgramName: string;
  /** PIC X(26) – Start timestamp. */
  bctStartTime: string;
  /** PIC X(26) – End timestamp. */
  bctEndTime: string;
  /** PIC X(26) – Elapsed time. */
  bctElapsedTime: string;
}

/** Single dependency entry. */
export interface BatchDependency {
  /** PIC X(8) – Prerequisite job name. */
  depJobName: string;
  /** PIC X(3) – REQ/OPT/EXC. */
  depType: DependencyType | string;
  /** PIC X(5) – Status of the dependency (e.g. DONE). */
  depStatus: string;
}

/** Return / completion info. */
export interface BatchReturnInfo {
  /** PIC S9(4) COMP – Return code from step. */
  bctReturnCode: number;
  /** PIC S9(4) COMP – Highest return code across steps. */
  bctHighestRc: number;
  /** PIC X(80) – Completion message. */
  bctMessage: string;
}

/** Full batch control record. */
export interface BatchControlRecord {
  bctKey: BatchControlKey;
  /** PIC X(5) – READY/ACTVE/WAIT/DONE/ERROR. */
  bctStatus: BatchStatus | string;
  bctProcessControl: BatchProcessControl;
  /** Up to 10 dependency entries. */
  bctDependencies: BatchDependency[];
  bctReturnInfo: BatchReturnInfo;
  /** PIC X(26) – Restart timestamp. */
  bctRestartTime: string;
  /** PIC S9(4) COMP – Attempt counter. */
  bctAttemptCount: number;
  /** PIC X(26) – Completion timestamp. */
  bctCompleteTime: string;
}
