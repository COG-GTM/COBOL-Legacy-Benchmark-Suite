/**
 * Online Error Handler Middleware.
 * Migrated from: src/programs/online/ERRHNDL.cbl
 *
 * Catches unhandled errors in Express routes, logs them to the ERRLOG
 * table, and returns a structured JSON error response.
 */

import { Request, Response, NextFunction } from 'express';
import { Knex } from 'knex';
import { insertErrLog } from '../../database/error-log';
import { ErrorType } from '../../types';

/**
 * Express error-handling middleware.
 * Must be registered with `app.use(errorHandler(db))` AFTER all routes.
 */
export function errorHandler(db: Knex) {
  return async (err: Error, req: Request, res: Response, _next: NextFunction): Promise<void> => {
    const now = new Date();
    const program = 'ERRHNDL';

    // Log to console
    console.error(`[ERROR] ${req.method} ${req.path}: ${err.message}`);

    // Persist to ERRLOG
    try {
      await insertErrLog(db, {
        elErrorTimestamp: now.toISOString(),
        elProgramId: program,
        elErrorType: ErrorType.Application,
        elErrorSeverity: 3,
        elErrorCode: 'HTTP500',
        elErrorMessage: err.message.slice(0, 80),
        elProcessDate: now.toISOString().slice(0, 10).replace(/-/g, ''),
        elProcessTime: now.toISOString().slice(11, 19).replace(/:/g, ''),
        elUserId: String((req as unknown as Record<string, unknown>)['userId'] || 'UNKNOWN'),
        elAdditionalInfo: `${req.method} ${req.path}`,
      });
    } catch (logErr) {
      console.error(`Failed to persist error log: ${logErr}`);
    }

    res.status(500).json({
      error: 'Internal server error',
      code: 'HTTP500',
      details: process.env.NODE_ENV === 'development' ? err.message : undefined,
    });
  };
}
