/**
 * Recovery Process Handler.
 * Migrated from: src/programs/batch/RCVPRC00.cbl
 *
 * Handles recovery scenarios: restart, bypass, terminate, and
 * sequence-level recovery of failed batch processes.
 */

import {
  BatchControlRecord,
  BatchStatus,
  ReturnCode,
  MAX_RESTARTS,
} from '../../types';

export type RecoveryFunction = 'INIT' | 'RECV' | 'TERM';

/** Recovery mode for a process. */
export enum RecoveryMode {
  Restart = 'RESTART',
  Bypass = 'BYPASS',
  Terminate = 'TERMINATE',
}

export class RecoveryProcess {
  private recoveryMode: RecoveryMode = RecoveryMode.Restart;
  private processedCount = 0;
  private failedCount = 0;

  /** Dispatch – mirrors COBOL 0000-MAIN EVALUATE. */
  execute(func: RecoveryFunction, record?: BatchControlRecord): number {
    switch (func) {
      case 'INIT':
        return this.initialize(record);
      case 'RECV':
        return this.processRecovery(record);
      case 'TERM':
        return this.terminate();
      default:
        console.error(`Invalid function code: ${func}`);
        return ReturnCode.Error;
    }
  }

  /** 1000-INITIALIZE – validate the record and set recovery mode. */
  private initialize(record?: BatchControlRecord): number {
    if (!record) {
      console.error('No control record provided for recovery');
      return ReturnCode.Error;
    }

    // Validate that the record is in an error state
    if (record.bctStatus !== BatchStatus.Error) {
      console.log(`Process ${record.bctKey.bctJobName} is not in error state`);
      return ReturnCode.Warning;
    }

    // Determine recovery mode based on attempt count
    if (record.bctAttemptCount >= MAX_RESTARTS) {
      this.recoveryMode = RecoveryMode.Terminate;
    } else {
      this.recoveryMode = RecoveryMode.Restart;
    }

    this.processedCount = 0;
    this.failedCount = 0;
    return ReturnCode.Success;
  }

  /** 2000-PROCESS-RECOVERY – execute the appropriate recovery action. */
  private processRecovery(record?: BatchControlRecord): number {
    if (!record) {
      console.error('No control record provided for recovery');
      return ReturnCode.Error;
    }

    this.processedCount++;

    switch (this.recoveryMode) {
      case RecoveryMode.Restart:
        return this.restartProcess(record);
      case RecoveryMode.Bypass:
        return this.bypassProcess(record);
      case RecoveryMode.Terminate:
        return this.terminateProcess(record);
      default:
        return ReturnCode.Error;
    }
  }

  /** Restart a failed process from its last checkpoint. */
  private restartProcess(record: BatchControlRecord): number {
    record.bctAttemptCount++;
    record.bctStatus = BatchStatus.Ready;
    record.bctRestartTime = new Date().toISOString();
    record.bctReturnInfo.bctReturnCode = 0;

    console.log(
      `Restarting process ${record.bctKey.bctJobName} ` +
      `(attempt ${record.bctAttemptCount}/${MAX_RESTARTS})`,
    );
    return ReturnCode.Success;
  }

  /** Bypass a failed process and mark it as done with a warning. */
  private bypassProcess(record: BatchControlRecord): number {
    record.bctStatus = BatchStatus.Done;
    record.bctReturnInfo.bctReturnCode = ReturnCode.Warning;
    record.bctReturnInfo.bctMessage = 'Process bypassed during recovery';
    record.bctCompleteTime = new Date().toISOString();

    console.log(`Bypassing process ${record.bctKey.bctJobName}`);
    return ReturnCode.Warning;
  }

  /** Terminate a process that has exceeded max restarts. */
  private terminateProcess(record: BatchControlRecord): number {
    record.bctStatus = BatchStatus.Error;
    record.bctReturnInfo.bctReturnCode = ReturnCode.Severe;
    record.bctReturnInfo.bctMessage = 'Process terminated – max restarts exceeded';
    record.bctCompleteTime = new Date().toISOString();
    this.failedCount++;

    console.error(
      `Terminating process ${record.bctKey.bctJobName} – ` +
      `exceeded max restarts (${MAX_RESTARTS})`,
    );
    return ReturnCode.Severe;
  }

  /** 3000-TERMINATE – display recovery summary. */
  private terminate(): number {
    console.log(
      `Recovery complete: ${this.processedCount} processed, ${this.failedCount} terminated`,
    );
    return this.failedCount > 0 ? ReturnCode.Error : ReturnCode.Success;
  }

  /** Set the recovery mode externally. */
  setRecoveryMode(mode: RecoveryMode): void {
    this.recoveryMode = mode;
  }
}
