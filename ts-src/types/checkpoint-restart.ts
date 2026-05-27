/**
 * Checkpoint / Restart types.
 * Migrated from: src/copybook/batch/CKPRST.cpy
 *
 * Provides durable checkpoint state so a batch program can resume after failure.
 */

/** Checkpoint status values. */
export enum CheckpointStatus {
  Active = 'A',
  Complete = 'C',
  Failed = 'F',
  Restarting = 'R',
}

/** Restart mode. */
export enum RestartMode {
  FromBeginning = 'B',
  FromCheckpoint = 'C',
  FromStep = 'S',
}

/** Checkpoint header. */
export interface CheckpointHeader {
  /** PIC X(8) – Program identifier. */
  programId: string;
  /** PIC X(8) – Run date (YYYYMMDD). */
  runDate: string;
  /** PIC X(6) – Run time (HHMMSS). */
  runTime: string;
  /** PIC X(1) – A/C/F/R. */
  status: CheckpointStatus | string;
}

/** Processing counters. */
export interface CheckpointCounters {
  /** PIC S9(9) COMP. */
  recordsRead: number;
  /** PIC S9(9) COMP. */
  recordsProcessed: number;
  /** PIC S9(9) COMP. */
  recordsInError: number;
}

/** Positional bookmark (for restart). */
export interface CheckpointPosition {
  /** PIC X(50) – Last successfully processed key. */
  lastKey: string;
  /** PIC X(8) – Current processing phase. */
  currentPhase: string;
  /** PIC S9(4) COMP – Current step number. */
  currentStep: number;
}

/** Resource / file status snapshot (up to 10 files). */
export interface CheckpointFileStatus {
  /** PIC X(8) – DD name or file reference. */
  fileName: string;
  /** PIC X(2) – File status code. */
  fileStatus: string;
  /** PIC S9(9) COMP – Record count for that file. */
  recordCount: number;
}

/** Control parameters. */
export interface CheckpointControl {
  /** PIC S9(4) COMP – Commit every N records. */
  commitFrequency: number;
  /** PIC S9(4) COMP – Max errors before abort. */
  errorLimit: number;
  /** PIC X(1) – B/C/S. */
  restartMode: RestartMode | string;
}

/** Full checkpoint control area. */
export interface CheckpointControlArea {
  header: CheckpointHeader;
  counters: CheckpointCounters;
  position: CheckpointPosition;
  resources: CheckpointFileStatus[];
  control: CheckpointControl;
}

/** Serialized checkpoint record (for VSAM storage). */
export interface CheckpointRecord {
  /** PIC X(8) – Program ID (record key). */
  checkpointId: string;
  /** PIC X(26) – When the checkpoint was taken. */
  checkpointTimestamp: string;
  /** JSON-serialized CheckpointControlArea. */
  checkpointData: string;
}
