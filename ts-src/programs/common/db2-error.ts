/**
 * Database SQL Error Handler.
 * Migrated from: src/programs/common/DB2ERR.cbl
 *
 * Processes SQL errors: logging, diagnostics, and retry decisions.
 * Maps SQLCODE / SQLSTATE to severity levels.
 */

import { Knex } from 'knex';
import { insertErrLog } from '../../database/error-log';
import { ErrLogRecord, ErrorType, ReturnCode } from '../../types';

export type Db2ErrorFunction = 'LOG' | 'DIAG' | 'RETR';

export class Db2Error {
  constructor(private readonly db: Knex) {}

  /** Dispatch – mirrors COBOL 0000-MAIN EVALUATE. */
  async execute(func: Db2ErrorFunction, err: unknown, programId: string): Promise<number> {
    switch (func) {
      case 'LOG':
        return this.logError(err, programId);
      case 'DIAG':
        return this.diagnose(err);
      case 'RETR':
        return this.shouldRetry(err);
      default:
        return ReturnCode.Error;
    }
  }

  /** 1000-LOG-ERROR – write to ERRLOG table. */
  private async logError(err: unknown, programId: string): Promise<number> {
    const severity = this.mapSeverity(err);
    const now = new Date();

    const record: ErrLogRecord = {
      elErrorTimestamp: now.toISOString(),
      elProgramId: programId,
      elErrorType: ErrorType.System,
      elErrorSeverity: severity,
      elErrorCode: this.extractCode(err),
      elErrorMessage: this.extractMessage(err).slice(0, 80),
      elProcessDate: now.toISOString().slice(0, 10).replace(/-/g, ''),
      elProcessTime: now.toISOString().slice(11, 19).replace(/:/g, ''),
      elUserId: 'SYSTEM',
      elAdditionalInfo: '',
    };

    try {
      await insertErrLog(this.db, record);
      return ReturnCode.Success;
    } catch (logErr) {
      console.error(`Failed to log error: ${logErr}`);
      return ReturnCode.Error;
    }
  }

  /** 2000-DIAGNOSE – return diagnostic information. */
  private diagnose(err: unknown): number {
    const message = this.extractMessage(err);
    const code = this.extractCode(err);
    const severity = this.mapSeverity(err);

    console.error(
      `SQL Diagnostic: code=${code}, severity=${severity}, message=${message}`,
    );
    return severity >= 3 ? ReturnCode.Error : ReturnCode.Warning;
  }

  /** 3000-SHOULD-RETRY – determine if the error is retryable. */
  private shouldRetry(err: unknown): number {
    const code = this.extractCode(err);
    const retryableCodes = ['-911', '-913', '-30081', 'SQLITE_BUSY'];
    if (retryableCodes.includes(code)) {
      return ReturnCode.Success; // 0 = yes, retry
    }
    return ReturnCode.Error; // 8 = no, do not retry
  }

  private mapSeverity(err: unknown): number {
    const code = this.extractCode(err);
    if (code === '-803') return 2; // duplicate key – warning level
    if (code === '-911' || code === '-913') return 2; // deadlock / timeout
    if (code.startsWith('-3') || code === '-99999') return 4; // network / fatal
    return 3; // default
  }

  private extractCode(err: unknown): string {
    if (err instanceof Error && 'code' in err) {
      return String((err as Record<string, unknown>).code);
    }
    return 'UNKNOWN';
  }

  private extractMessage(err: unknown): string {
    if (err instanceof Error) return err.message;
    return String(err);
  }
}
