/**
 * Process Sequence Record types.
 * Migrated from: src/copybook/batch/PRCSEQ.cpy
 *
 * Defines the order, timing, dependencies, and recovery behaviour of
 * batch processes within a processing run.
 */

/** Process type codes. */
export enum PsrProcessType {
  Init = 'INIT',
  Processing = 'PROC',
  Report = 'RPT ',
  Termination = 'TERM',
}

/** Frequency of execution. */
export enum PsrFrequency {
  Daily = 'D',
  Weekly = 'W',
  Monthly = 'M',
  OnDemand = 'O',
}

/** Holiday handling strategy. */
export enum PsrHolidayHandling {
  Skip = 'S',
  Previous = 'P',
  Next = 'N',
}

/** Composite key for a process-sequence record. */
export interface ProcessSequenceKey {
  /** PIC X(8) – Process identifier. */
  psrProcessId: string;
  /** PIC 9(2) – Version number. */
  psrVersion: number;
}

/** Single dependency entry. */
export interface ProcessDependency {
  /** PIC X(8) – Dependent process ID. */
  depProcessId: string;
  /** PIC X(3) – REQ/OPT/EXC. */
  depType: string;
}

/** Timing / frequency information. */
export interface ProcessTiming {
  /** PIC X(1) – D/W/M/O. */
  frequency: PsrFrequency | string;
  /** PIC X(6) – Earliest start time (HHMMSS). */
  startTime: string;
  /** PIC S9(4) COMP – Max run time in minutes. */
  maxTime: number;
}

/** Control parameters for execution. */
export interface ProcessControl {
  /** PIC X(8) – Program to execute. */
  programName: string;
  /** PIC X(80) – Parameters to pass. */
  parm: string;
  /** PIC S9(4) COMP – Maximum acceptable return code. */
  maxRc: number;
  /** PIC X(1) – Y/N. */
  restartable: boolean;
}

/** Schedule constraints. */
export interface ProcessSchedule {
  /** PIC X(7) – Bitmask for active days (MTWTFSS). */
  activeDays: string;
  /** PIC X(1) – Y/N – run on month-end? */
  monthEnd: boolean;
  /** PIC X(1) – S/P/N. */
  holidayHandling: PsrHolidayHandling | string;
}

/** Recovery information. */
export interface ProcessRecovery {
  /** PIC S9(4) COMP – Retry count. */
  retryCount: number;
  /** PIC S9(4) COMP – Seconds between retries. */
  retryInterval: number;
  /** PIC X(8) – Recovery program. */
  recoveryProgram: string;
}

/** Audit fields. */
export interface ProcessAudit {
  /** PIC X(8) – Created by user. */
  createdBy: string;
  /** PIC X(26) – Creation timestamp. */
  createdAt: string;
  /** PIC X(8) – Last modified by user. */
  modifiedBy: string;
  /** PIC X(26) – Modification timestamp. */
  modifiedAt: string;
}

/** Full process-sequence record. */
export interface ProcessSequenceRecord {
  psrKey: ProcessSequenceKey;
  /** PIC X(4) – INIT/PROC/RPT/TERM. */
  psrType: PsrProcessType | string;
  psrTiming: ProcessTiming;
  /** Up to 10 dependencies. */
  psrDependencies: ProcessDependency[];
  psrControl: ProcessControl;
  psrSchedule: ProcessSchedule;
  psrRecovery: ProcessRecovery;
  psrAudit: ProcessAudit;
}
