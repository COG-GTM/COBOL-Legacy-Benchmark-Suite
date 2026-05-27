/**
 * DB2 Procedures types.
 * Migrated from: src/copybook/db2/DBPROC.cpy
 *
 * Error handling, retry logic, and connection management for DB2 operations.
 */

/** DB2 error handling area. */
export interface Db2ErrorHandling {
  /** Formatted error message. */
  db2ErrorMessage: string;
  /** SQLCODE from the last operation. */
  db2Sqlcode: number;
  /** SQLSTATE from the last operation. */
  db2Sqlstate: string;
}

/** Retry control for DB2 operations. */
export interface Db2RetryControl {
  /** Current retry attempt. */
  db2RetryCount: number;
  /** Maximum retries allowed. */
  db2RetryMax: number;
  /** Milliseconds to wait between retries. */
  db2RetryWait: number;
}

/** SQL Communication Area status codes. */
export const SQL_STATUS = {
  SUCCESS: '00000',
  NOT_FOUND: '02000',
  DUP_KEY: '23505',
  DEADLOCK: '40001',
  TIMEOUT: '40003',
  CONNECTION_ERROR: '08001',
  DB_ERROR: '58004',
} as const;

/** Well-known SQLCODE values. */
export const SQLCODE = {
  SUCCESS: 0,
  NOT_FOUND: 100,
  DUP_KEY: -803,
  DEADLOCK: -911,
  TIMEOUT: -913,
  MAX_CONNECTIONS: -30081,
  NETWORK_ERROR: -99999,
} as const;
