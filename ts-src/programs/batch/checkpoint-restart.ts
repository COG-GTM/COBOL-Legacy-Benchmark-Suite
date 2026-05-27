/**
 * Checkpoint / Restart Handler.
 * Migrated from: src/programs/batch/CKPRST.cbl
 *
 * Takes periodic checkpoints during batch processing so that a program
 * can be restarted from the last successful checkpoint after a failure.
 */

import {
  CheckpointControlArea,
  CheckpointHeader,
  CheckpointCounters,
  CheckpointPosition,
  CheckpointControl,
  CheckpointStatus,
  RestartMode,
  ReturnCode,
} from '../../types';

export type CheckpointFunction = 'INIT' | 'TAKE' | 'CMIT' | 'REST';

export class CheckpointRestart {
  private checkpoint: CheckpointControlArea;
  private lastCommitTimestamp = '';

  constructor() {
    this.checkpoint = this.createEmpty();
  }

  /** Dispatch – mirrors COBOL EVALUATE. */
  execute(func: CheckpointFunction): number {
    switch (func) {
      case 'INIT':
        return this.initialize();
      case 'TAKE':
        return this.takeCheckpoint();
      case 'CMIT':
        return this.commitCheckpoint();
      case 'REST':
        return this.restart();
      default:
        console.error(`Invalid function code: ${func}`);
        return ReturnCode.Error;
    }
  }

  /** 1000-INITIALIZE – set up a fresh checkpoint area. */
  private initialize(): number {
    const now = new Date();
    this.checkpoint.header.runDate = this.formatDate(now);
    this.checkpoint.header.runTime = this.formatTime(now);
    this.checkpoint.header.status = CheckpointStatus.Active;
    this.checkpoint.counters = { recordsRead: 0, recordsProcessed: 0, recordsInError: 0 };

    console.log(`Checkpoint initialised for program ${this.checkpoint.header.programId}`);
    return ReturnCode.Success;
  }

  /** 2000-TAKE-CHECKPOINT – snapshot current counters and position. */
  private takeCheckpoint(): number {
    this.lastCommitTimestamp = new Date().toISOString();
    console.log(
      `Checkpoint taken: read=${this.checkpoint.counters.recordsRead}, ` +
      `processed=${this.checkpoint.counters.recordsProcessed}, ` +
      `errors=${this.checkpoint.counters.recordsInError}`,
    );
    return ReturnCode.Success;
  }

  /** 3000-COMMIT-CHECKPOINT – persist the checkpoint (database or file). */
  private commitCheckpoint(): number {
    // In the COBOL skeleton this was a stub. Here we log it.
    console.log(`Checkpoint committed at ${this.lastCommitTimestamp}`);
    return ReturnCode.Success;
  }

  /** 4000-RESTART – restore state from the last committed checkpoint. */
  private restart(): number {
    if (!this.lastCommitTimestamp) {
      console.log('No checkpoint to restart from – starting from beginning');
      this.checkpoint.control.restartMode = RestartMode.FromBeginning;
      return ReturnCode.Warning;
    }

    this.checkpoint.header.status = CheckpointStatus.Restarting;
    console.log(`Restarting from checkpoint at ${this.lastCommitTimestamp}`);
    console.log(
      `Position: key=${this.checkpoint.position.lastKey}, ` +
      `phase=${this.checkpoint.position.currentPhase}`,
    );
    return ReturnCode.Success;
  }

  // ── Accessor helpers ──────────────────────────────────────────────────

  /** Set the owning program ID. */
  setProgramId(id: string): void {
    this.checkpoint.header.programId = id;
  }

  /** Increment read counter. */
  incrementRead(): void {
    this.checkpoint.counters.recordsRead++;
  }

  /** Increment processed counter. */
  incrementProcessed(): void {
    this.checkpoint.counters.recordsProcessed++;
  }

  /** Increment error counter. */
  incrementError(): void {
    this.checkpoint.counters.recordsInError++;
  }

  /** Update the positional bookmark. */
  setPosition(lastKey: string, phase: string): void {
    this.checkpoint.position.lastKey = lastKey;
    this.checkpoint.position.currentPhase = phase;
  }

  /** Set commit frequency. */
  setCommitFrequency(freq: number): void {
    this.checkpoint.control.commitFrequency = freq;
  }

  /** Check whether it is time to commit (every N records). */
  shouldCommit(): boolean {
    if (this.checkpoint.control.commitFrequency <= 0) return false;
    return (
      this.checkpoint.counters.recordsProcessed % this.checkpoint.control.commitFrequency === 0
    );
  }

  /** Check whether the error limit has been exceeded. */
  isErrorLimitExceeded(): boolean {
    return this.checkpoint.counters.recordsInError >= this.checkpoint.control.errorLimit;
  }

  /** Get the current counters snapshot. */
  getCounters(): CheckpointCounters {
    return { ...this.checkpoint.counters };
  }

  /** Mark the run as complete. */
  markComplete(): void {
    this.checkpoint.header.status = CheckpointStatus.Complete;
  }

  /** Mark the run as failed. */
  markFailed(): void {
    this.checkpoint.header.status = CheckpointStatus.Failed;
  }

  // ── Private helpers ───────────────────────────────────────────────────

  private createEmpty(): CheckpointControlArea {
    return {
      header: { programId: '', runDate: '', runTime: '', status: CheckpointStatus.Active },
      counters: { recordsRead: 0, recordsProcessed: 0, recordsInError: 0 },
      position: { lastKey: '', currentPhase: '', currentStep: 0 },
      resources: [],
      control: { commitFrequency: 1000, errorLimit: 100, restartMode: RestartMode.FromCheckpoint },
    };
  }

  private formatDate(d: Date): string {
    return d.toISOString().slice(0, 10).replace(/-/g, '');
  }

  private formatTime(d: Date): string {
    return d.toISOString().slice(11, 19).replace(/:/g, '');
  }
}
