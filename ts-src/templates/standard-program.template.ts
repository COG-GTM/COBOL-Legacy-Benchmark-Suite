/**
 * Standard Program Template.
 * Migrated from: src/templates/program/standard-program.cbl
 *
 * Base class for all batch programs. Provides the standard lifecycle:
 * initialize → process → terminate.
 */

import { ReturnCode } from '../types';

/**
 * Abstract base class mirroring the standard COBOL program structure.
 *
 * Subclasses implement the three lifecycle hooks:
 * - `onInitialize()` – COBOL 1000-INITIALIZE
 * - `onProcess()`    – COBOL 2000-PROCESS
 * - `onTerminate()`  – COBOL 3000-TERMINATE
 */
export abstract class StandardProgram {
  protected programName = 'UNKNOWN';
  protected returnCode: number = ReturnCode.Success;
  protected startTime = '';
  protected endTime = '';

  /** Main entry point – mirrors COBOL 0000-MAIN. */
  async run(): Promise<number> {
    this.startTime = new Date().toISOString();
    console.log(`[${this.programName}] Starting at ${this.startTime}`);

    let rc = await this.onInitialize();
    if (rc > ReturnCode.Warning) {
      this.returnCode = rc;
      await this.onTerminate();
      return this.returnCode;
    }

    rc = await this.onProcess();
    this.returnCode = Math.max(this.returnCode, rc);

    await this.onTerminate();

    this.endTime = new Date().toISOString();
    console.log(
      `[${this.programName}] Completed at ${this.endTime} – RC=${this.returnCode}`,
    );

    return this.returnCode;
  }

  protected abstract onInitialize(): Promise<number>;
  protected abstract onProcess(): Promise<number>;
  protected abstract onTerminate(): Promise<number>;
}
