/**
 * Database Connection Module.
 * Migrated from: src/database/db2/ (connection concepts) + src/programs/common/DB2CONN.cbl
 *
 * Provides a Knex-based connection to SQLite (or PostgreSQL) that replaces
 * the DB2 CONNECT / DISCONNECT / STATUS operations.
 */

import Knex, { Knex as KnexType } from 'knex';
import path from 'path';

let knexInstance: KnexType | null = null;

/** Default SQLite database path (relative to project root). */
const DEFAULT_DB_PATH = path.resolve(__dirname, '..', 'data', 'portfolio.db');

/** Knex configuration for SQLite (can be swapped for PostgreSQL). */
export function getKnexConfig(dbPath?: string): KnexType.Config {
  return {
    client: 'better-sqlite3',
    connection: {
      filename: dbPath ?? DEFAULT_DB_PATH,
    },
    useNullAsDefault: true,
  };
}

/** Connect to the database (equivalent to COBOL 1000-CONNECT). */
export async function connectToDatabase(dbPath?: string): Promise<KnexType> {
  if (knexInstance) {
    return knexInstance;
  }
  knexInstance = Knex(getKnexConfig(dbPath));
  // Verify the connection works.
  await knexInstance.raw('SELECT 1');
  return knexInstance;
}

/** Disconnect from the database (equivalent to COBOL 2000-DISCONNECT). */
export async function disconnectFromDatabase(): Promise<void> {
  if (knexInstance) {
    await knexInstance.destroy();
    knexInstance = null;
  }
}

/** Check whether a connection is active (equivalent to COBOL 3000-CHECK-STATUS). */
export async function checkDatabaseStatus(): Promise<boolean> {
  if (!knexInstance) {
    return false;
  }
  try {
    await knexInstance.raw('SELECT 1');
    return true;
  } catch {
    return false;
  }
}

/** Get the current Knex instance (throws if not connected). */
export function getDatabase(): KnexType {
  if (!knexInstance) {
    throw new Error('Database not connected. Call connectToDatabase() first.');
  }
  return knexInstance;
}
