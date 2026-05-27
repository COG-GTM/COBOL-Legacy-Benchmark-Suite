/**
 * Batch Control Program.
 * Migrated from: src/programs/batch/BCHCTL00.cbl
 *
 * The batch supervisor: initialises a batch run, checks prerequisites,
 * updates step status, and terminates the run.  Replaces JCL step
 * orchestration with an async TypeScript batch runner.
 */

import { Knex } from 'knex';
import {
  BatchControlRecord,
  BatchControlKey,
  BatchStatus,
  ReturnCode,
} from '../../types';

export type BatchControlFunction = 'INIT' | 'CHEK' | 'UPDT' | 'TERM';

export class BatchControl {
  private controlRecords: Map<string, BatchControlRecord> = new Map();

  constructor(private readonly db: Knex) {}

  /** Dispatch to the correct handler – mirrors COBOL 0000-MAIN EVALUATE. */
  async execute(func: BatchControlFunction, key: BatchControlKey): Promise<number> {
    switch (func) {
      case 'INIT':
        return this.initialize(key);
      case 'CHEK':
        return this.checkPrerequisites(key);
      case 'UPDT':
        return this.updateStatus(key);
      case 'TERM':
        return this.terminate(key);
      default:
        console.error(`Invalid function code: ${func}`);
        return ReturnCode.Error;
    }
  }

  /** 1000-INITIALIZE – create a batch control record. */
  private initialize(key: BatchControlKey): number {
    const recordKey = this.buildKey(key);
    const now = new Date().toISOString();

    const record: BatchControlRecord = {
      bctKey: { ...key },
      bctStatus: BatchStatus.Ready,
      bctProcessControl: {
        bctStepName: '',
        bctProgramName: '',
        bctStartTime: now,
        bctEndTime: '',
        bctElapsedTime: '',
      },
      bctDependencies: [],
      bctReturnInfo: {
        bctReturnCode: 0,
        bctHighestRc: 0,
        bctMessage: '',
      },
      bctRestartTime: '',
      bctAttemptCount: 0,
      bctCompleteTime: '',
    };

    this.controlRecords.set(recordKey, record);
    console.log(`Batch control initialized for job ${key.bctJobName}`);
    return ReturnCode.Success;
  }

  /** 2000-CHECK-PREREQUISITES – verify all dependencies are DONE. */
  private checkPrerequisites(key: BatchControlKey): number {
    const record = this.controlRecords.get(this.buildKey(key));
    if (!record) {
      console.error(`Control record not found for ${key.bctJobName}`);
      return ReturnCode.Error;
    }

    for (const dep of record.bctDependencies) {
      if (dep.depType === 'REQ' && dep.depStatus !== 'DONE') {
        console.log(`Prerequisite ${dep.depJobName} not met (status: ${dep.depStatus})`);
        record.bctStatus = BatchStatus.Waiting;
        return ReturnCode.Warning;
      }
    }

    record.bctStatus = BatchStatus.Active;
    return ReturnCode.Success;
  }

  /** 3000-UPDATE-STATUS – update the control record after a step completes. */
  private updateStatus(key: BatchControlKey): number {
    const record = this.controlRecords.get(this.buildKey(key));
    if (!record) {
      console.error(`Control record not found for ${key.bctJobName}`);
      return ReturnCode.Error;
    }

    if (record.bctReturnInfo.bctReturnCode > record.bctReturnInfo.bctHighestRc) {
      record.bctReturnInfo.bctHighestRc = record.bctReturnInfo.bctReturnCode;
    }

    if (record.bctReturnInfo.bctHighestRc >= ReturnCode.Error) {
      record.bctStatus = BatchStatus.Error;
    }

    return ReturnCode.Success;
  }

  /** 4000-TERMINATE – finalise the batch run. */
  private terminate(key: BatchControlKey): number {
    const record = this.controlRecords.get(this.buildKey(key));
    if (!record) {
      console.error(`Control record not found for ${key.bctJobName}`);
      return ReturnCode.Error;
    }

    const now = new Date().toISOString();
    record.bctProcessControl.bctEndTime = now;
    record.bctCompleteTime = now;

    if (record.bctStatus !== BatchStatus.Error) {
      record.bctStatus = BatchStatus.Done;
    }

    console.log(
      `Batch ${key.bctJobName} completed – status=${record.bctStatus}, ` +
      `highest RC=${record.bctReturnInfo.bctHighestRc}`,
    );
    return ReturnCode.Success;
  }

  /** Retrieve a control record. */
  getRecord(key: BatchControlKey): BatchControlRecord | undefined {
    return this.controlRecords.get(this.buildKey(key));
  }

  /** Set the return code for the current step. */
  setStepResult(key: BatchControlKey, stepName: string, programName: string, rc: number): void {
    const record = this.controlRecords.get(this.buildKey(key));
    if (record) {
      record.bctProcessControl.bctStepName = stepName;
      record.bctProcessControl.bctProgramName = programName;
      record.bctReturnInfo.bctReturnCode = rc;
    }
  }

  /** Add a dependency to a control record. */
  addDependency(key: BatchControlKey, depJobName: string, depType: string): void {
    const record = this.controlRecords.get(this.buildKey(key));
    if (record) {
      record.bctDependencies.push({ depJobName, depType, depStatus: '' });
    }
  }

  private buildKey(key: BatchControlKey): string {
    return `${key.bctJobName}|${key.bctProcessDate}|${key.bctSequenceNo}`;
  }
}
