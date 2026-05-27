/**
 * Error Processor.
 * Migrated from: src/programs/common/ERRPROC.cbl
 *
 * Centralized error processing: logs errors to the database,
 * formats error messages, and tracks error counts.
 */

import { Knex } from 'knex';
import {
  ErrorMessage,
  ErrorCategory,
  ErrorSeverity,
  ReturnCode,
} from '../../types';
import { insertErrLog } from '../../database/error-log';

export class ErrorProcessor {
  private errorCount = 0;
  private warningCount = 0;
  private readonly maxErrors: number;

  constructor(
    private readonly db: Knex,
    maxErrors = 100,
  ) {
    this.maxErrors = maxErrors;
  }

  /** Process an error – log it and track counts. */
  async processError(error: ErrorMessage): Promise<number> {
    // Track counts
    if (error.errSeverity >= ErrorSeverity.Error) {
      this.errorCount++;
    } else if (error.errSeverity >= ErrorSeverity.Warning) {
      this.warningCount++;
    }

    // Log to console
    const level = error.errSeverity >= ErrorSeverity.Error ? 'ERROR' : 'WARN';
    console.log(
      `[${level}] ${error.errProgram} (${error.errCategory}/${error.errCode}): ${error.errText}`,
    );

    // Persist to ERRLOG
    try {
      await insertErrLog(this.db, {
        elErrorTimestamp: error.errTimestamp || new Date().toISOString(),
        elProgramId: error.errProgram,
        elErrorType: this.mapCategoryToType(error.errCategory),
        elErrorSeverity: typeof error.errSeverity === 'number' ? error.errSeverity : 0,
        elErrorCode: error.errCode,
        elErrorMessage: error.errText.slice(0, 80),
        elProcessDate: new Date().toISOString().slice(0, 10).replace(/-/g, ''),
        elProcessTime: new Date().toISOString().slice(11, 19).replace(/:/g, ''),
        elUserId: 'SYSTEM',
        elAdditionalInfo: error.errDetails.slice(0, 100),
      });
    } catch (logErr) {
      console.error(`Failed to persist error log: ${logErr}`);
    }

    // Check thresholds
    if (this.errorCount >= this.maxErrors) {
      console.error(`Error limit reached (${this.maxErrors})`);
      return ReturnCode.Severe;
    }

    return error.errSeverity >= ErrorSeverity.Error ? ReturnCode.Error : ReturnCode.Warning;
  }

  /** Build a formatted error message. */
  static createError(
    program: string,
    category: ErrorCategory,
    code: string,
    text: string,
    details = '',
    severity: ErrorSeverity = ErrorSeverity.Error,
  ): ErrorMessage {
    return {
      errTimestamp: new Date().toISOString(),
      errProgram: program,
      errCategory: category,
      errCode: code,
      errSeverity: severity,
      errText: text,
      errDetails: details,
    };
  }

  /** Get current counts. */
  getCounts(): { errors: number; warnings: number } {
    return { errors: this.errorCount, warnings: this.warningCount };
  }

  private mapCategoryToType(cat: ErrorCategory | string): string {
    switch (cat) {
      case ErrorCategory.System:
        return 'S';
      case ErrorCategory.Vsam:
      case ErrorCategory.Processing:
        return 'A';
      case ErrorCategory.Validation:
        return 'D';
      default:
        return 'S';
    }
  }
}
