/**
 * Batch Processing Constants.
 * Migrated from: src/copybook/batch/BCHCON.cpy
 *
 * Status values, thresholds, and control constants for batch job orchestration.
 */

/** Batch job status values. */
export enum BatchStatus {
  Ready = 'READY',
  Active = 'ACTVE',
  Waiting = 'WAIT ',
  Done = 'DONE ',
  Error = 'ERROR',
}

/** Process type codes. */
export enum ProcessType {
  Initial = 'INIT',
  Update = 'UPDT',
  Report = 'RPT ',
  Cleanup = 'CLEN',
}

/** Dependency type codes. */
export enum DependencyType {
  Required = 'REQ',
  Optional = 'OPT',
  Exclusive = 'EXC',
}

/** Return-code severity thresholds. */
export const RC_THRESHOLD_SUCCESS = 0;
export const RC_THRESHOLD_WARNING = 4;
export const RC_THRESHOLD_ERROR = 8;
export const RC_THRESHOLD_SEVERE = 12;

/** Process control constants. */
export const MAX_PREREQ = 10;
export const MAX_RESTARTS = 3;
export const WAIT_INTERVAL = 300;
export const MAX_WAIT_TIME = 3600;
export const MAX_STEPS = 99;
export const MAX_DEPENDENCIES = 10;
