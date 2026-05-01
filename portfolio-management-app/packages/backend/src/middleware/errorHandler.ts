import { Request, Response, NextFunction } from 'express';
import { AppError } from '../utils/errors.js';
import { ErrorCategory, ErrorSeverity } from '../types/index.js';

// Centralized error handler — maps from ERRHNDL.cbl
// P100: Init, P200: Log, P300: Format, P400: Determine action
export function errorHandler(
  err: Error,
  _req: Request,
  res: Response,
  _next: NextFunction
): void {
  // P200-LOG-ERROR equivalent
  console.error(`[ERROR] ${new Date().toISOString()} - ${err.message}`, err.stack);

  if (err instanceof AppError) {
    // P300-FORMAT-MESSAGE: structured error response
    res.status(err.statusCode).json({
      success: false,
      error: err.toApiError(),
    });
    return;
  }

  // P400-DETERMINE-ACTION: unknown errors get generic treatment
  res.status(500).json({
    success: false,
    error: {
      code: 'SY999',
      message: 'Internal server error',
      category: ErrorCategory.System,
      severity: ErrorSeverity.Severe,
    },
  });
}
