/**
 * DB2 Handling Template.
 * Migrated from: src/templates/database/db2-handling.cbl
 *
 * Base class for programs that interact with the database (DB2 → Knex).
 * Provides connection management, transaction boundaries, and error handling.
 */

import { Knex } from 'knex';
import { ReturnCode } from '../types';
import { StandardProgram } from './standard-program.template';
import { connectToDatabase, disconnectFromDatabase } from '../database';

/**
 * Abstract database-handling program.
 *
 * Subclasses implement:
 * - `onDbInitialize(db)` – set up queries / prepare statements
 * - `onDbProcess(db)`    – main processing logic
 * - `onDbTerminate(db)`  – cleanup
 */
export abstract class Db2HandlingProgram extends StandardProgram {
  protected db: Knex | null = null;

  protected async onInitialize(): Promise<number> {
    try {
      this.db = await connectToDatabase();
      return this.onDbInitialize(this.db);
    } catch (err) {
      console.error(`Database connection failed: ${err}`);
      return ReturnCode.Severe;
    }
  }

  protected async onProcess(): Promise<number> {
    if (!this.db) return ReturnCode.Error;

    try {
      return await this.onDbProcess(this.db);
    } catch (err) {
      console.error(`Database processing error: ${err}`);
      return ReturnCode.Error;
    }
  }

  protected async onTerminate(): Promise<number> {
    try {
      if (this.db) {
        const rc = await this.onDbTerminate(this.db);
        await disconnectFromDatabase();
        return rc;
      }
      return ReturnCode.Success;
    } catch (err) {
      console.error(`Termination error: ${err}`);
      return ReturnCode.Warning;
    }
  }

  protected abstract onDbInitialize(db: Knex): Promise<number>;
  protected abstract onDbProcess(db: Knex): Promise<number>;
  protected abstract onDbTerminate(db: Knex): Promise<number>;
}
