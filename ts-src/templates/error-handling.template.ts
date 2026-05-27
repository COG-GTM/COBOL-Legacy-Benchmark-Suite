/**
 * Error Handling Template.
 * Migrated from: src/templates/error/error-handling.cbl
 *
 * Mixin / utility for standardized error handling across programs.
 * Provides error capture, formatting, severity mapping, and propagation.
 */

import {
  ErrorMessage,
  ErrorCategory,
  ErrorSeverity,
  ReturnCode,
} from '../types';

/**
 * Standardized error handler that programs can compose into their classes.
 */
export class ErrorHandler {
  private errors: ErrorMessage[] = [];
  private readonly maxErrors: number;

  constructor(
    private readonly programName: string,
    maxErrors = 50,
  ) {
    this.maxErrors = maxErrors;
  }

  /** Capture an error. */
  handleError(
    category: ErrorCategory,
    code: string,
    text: string,
    severity: ErrorSeverity = ErrorSeverity.Error,
    details = '',
  ): number {
    const error: ErrorMessage = {
      errTimestamp: new Date().toISOString(),
      errProgram: this.programName,
      errCategory: category,
      errCode: code,
      errSeverity: severity,
      errText: text,
      errDetails: details,
    };

    this.errors.push(error);

    const level = severity >= ErrorSeverity.Error ? 'ERROR' : 'WARN';
    console.log(`[${level}] ${this.programName} ${category}/${code}: ${text}`);

    if (this.errors.length >= this.maxErrors) {
      console.error(`Error limit reached (${this.maxErrors})`);
      return ReturnCode.Severe;
    }

    return severity >= ErrorSeverity.Error ? ReturnCode.Error : ReturnCode.Warning;
  }

  /** Capture an exception as an error. */
  handleException(err: unknown, category: ErrorCategory = ErrorCategory.System): number {
    const message = err instanceof Error ? err.message : String(err);
    return this.handleError(category, 'EXC', message, ErrorSeverity.Error);
  }

  /** Check whether the error limit has been exceeded. */
  isLimitExceeded(): boolean {
    return this.errors.length >= this.maxErrors;
  }

  /** Get all captured errors. */
  getErrors(): ErrorMessage[] {
    return [...this.errors];
  }

  /** Get error count. */
  getErrorCount(): number {
    return this.errors.length;
  }

  /** Clear all errors. */
  clear(): void {
    this.errors = [];
  }
}
