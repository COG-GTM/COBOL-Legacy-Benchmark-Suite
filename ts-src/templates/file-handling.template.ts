/**
 * File Handling Template.
 * Migrated from: src/templates/program/file-handling.cbl
 *
 * Base class for programs that read from an input source and write
 * to an output target.  Provides open/read/write/close lifecycle.
 */

import { ReturnCode } from '../types';
import { StandardProgram } from './standard-program.template';

/**
 * Abstract file-handling program.
 *
 * Subclasses implement:
 * - `openFiles()`    – COBOL OPEN INPUT / OUTPUT
 * - `readRecord()`   – COBOL READ
 * - `processRecord(record)` – business logic per record
 * - `closeFiles()`   – COBOL CLOSE
 */
export abstract class FileHandlingProgram<T> extends StandardProgram {
  protected recordsRead = 0;
  protected recordsWritten = 0;
  protected recordsInError = 0;

  protected async onInitialize(): Promise<number> {
    return this.openFiles();
  }

  protected async onProcess(): Promise<number> {
    let record = await this.readRecord();
    while (record !== undefined) {
      this.recordsRead++;
      try {
        await this.processRecord(record);
        this.recordsWritten++;
      } catch (err) {
        this.recordsInError++;
        console.error(`Error processing record: ${err}`);
      }
      record = await this.readRecord();
    }

    return this.recordsInError > 0 ? ReturnCode.Warning : ReturnCode.Success;
  }

  protected async onTerminate(): Promise<number> {
    await this.closeFiles();
    console.log(
      `[${this.programName}] Records: read=${this.recordsRead}, ` +
      `written=${this.recordsWritten}, errors=${this.recordsInError}`,
    );
    return ReturnCode.Success;
  }

  protected abstract openFiles(): Promise<number>;
  protected abstract readRecord(): Promise<T | undefined>;
  protected abstract processRecord(record: T): Promise<void>;
  protected abstract closeFiles(): Promise<number>;
}
