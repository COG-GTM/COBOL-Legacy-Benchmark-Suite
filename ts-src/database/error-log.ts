/**
 * Error Log Database Operations.
 * Migrated from: src/database/db2/ERRLOG.sql
 *
 * Provides typed insert/query helpers for the ERRLOG table.
 */

import { Knex } from 'knex';
import { ErrLogRecord } from '../types';

/** Insert an error log record. */
export async function insertErrLog(db: Knex, record: ErrLogRecord): Promise<void> {
  await db('ERRLOG').insert({
    ERROR_TIMESTAMP: record.elErrorTimestamp,
    PROGRAM_ID: record.elProgramId,
    ERROR_TYPE: record.elErrorType,
    ERROR_SEVERITY: record.elErrorSeverity,
    ERROR_CODE: record.elErrorCode,
    ERROR_MESSAGE: record.elErrorMessage,
    PROCESS_DATE: record.elProcessDate,
    PROCESS_TIME: record.elProcessTime,
    USER_ID: record.elUserId,
    ADDITIONAL_INFO: record.elAdditionalInfo,
  });
}

/** Query error logs by program ID. */
export async function queryErrLogByProgram(
  db: Knex,
  programId: string,
  limit = 100,
): Promise<ErrLogRecord[]> {
  const rows = await db('ERRLOG')
    .where('PROGRAM_ID', programId)
    .orderBy('ERROR_TIMESTAMP', 'desc')
    .limit(limit);

  return rows.map(rowToErrLog);
}

/** Query error logs by severity. */
export async function queryErrLogBySeverity(
  db: Knex,
  minSeverity: number,
  limit = 100,
): Promise<ErrLogRecord[]> {
  const rows = await db('ERRLOG')
    .where('ERROR_SEVERITY', '>=', minSeverity)
    .orderBy('ERROR_TIMESTAMP', 'desc')
    .limit(limit);

  return rows.map(rowToErrLog);
}

function rowToErrLog(row: Record<string, unknown>): ErrLogRecord {
  return {
    elErrorTimestamp: String(row['ERROR_TIMESTAMP'] ?? ''),
    elProgramId: String(row['PROGRAM_ID'] ?? ''),
    elErrorType: String(row['ERROR_TYPE'] ?? 'S'),
    elErrorSeverity: Number(row['ERROR_SEVERITY'] ?? 1),
    elErrorCode: String(row['ERROR_CODE'] ?? ''),
    elErrorMessage: String(row['ERROR_MESSAGE'] ?? ''),
    elProcessDate: String(row['PROCESS_DATE'] ?? ''),
    elProcessTime: String(row['PROCESS_TIME'] ?? ''),
    elUserId: String(row['USER_ID'] ?? ''),
    elAdditionalInfo: String(row['ADDITIONAL_INFO'] ?? ''),
  };
}
