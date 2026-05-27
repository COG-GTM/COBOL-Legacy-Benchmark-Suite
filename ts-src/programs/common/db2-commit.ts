/**
 * Database Commit Controller.
 * Migrated from: src/programs/common/DB2CMT.cbl
 *
 * Manages transaction boundaries: begin, commit, rollback, savepoint, and
 * restore.
 */

import { Knex } from 'knex';
import { ReturnCode } from '../../types';

export type CommitFunction = 'INIT' | 'CMIT' | 'RBAK' | 'SAVE' | 'REST' | 'STAT';

export class Db2Commit {
  private commitCount = 0;
  private rollbackCount = 0;
  private currentTransaction: Knex.Transaction | null = null;

  constructor(private readonly db: Knex) {}

  /** Dispatch – mirrors COBOL 0000-MAIN EVALUATE. */
  async execute(func: CommitFunction): Promise<number> {
    switch (func) {
      case 'INIT':
        return this.initialize();
      case 'CMIT':
        return this.commit();
      case 'RBAK':
        return this.rollback();
      case 'SAVE':
        return this.savepoint();
      case 'REST':
        return this.restore();
      case 'STAT':
        return this.displayStats();
      default:
        console.error(`Invalid function code: ${func}`);
        return ReturnCode.Error;
    }
  }

  /** 1000-INITIALIZE – begin a new transaction. */
  private async initialize(): Promise<number> {
    try {
      this.currentTransaction = await this.db.transaction();
      return ReturnCode.Success;
    } catch (err) {
      console.error(`Error starting transaction: ${err}`);
      return ReturnCode.Error;
    }
  }

  /** 2000-COMMIT. */
  private async commit(): Promise<number> {
    try {
      if (this.currentTransaction) {
        await this.currentTransaction.commit();
        this.currentTransaction = null;
      }
      this.commitCount++;
      return ReturnCode.Success;
    } catch (err) {
      console.error(`Commit error: ${err}`);
      return ReturnCode.Error;
    }
  }

  /** 3000-ROLLBACK. */
  private async rollback(): Promise<number> {
    try {
      if (this.currentTransaction) {
        await this.currentTransaction.rollback();
        this.currentTransaction = null;
      }
      this.rollbackCount++;
      return ReturnCode.Success;
    } catch (err) {
      console.error(`Rollback error: ${err}`);
      return ReturnCode.Error;
    }
  }

  /** 4000-SAVEPOINT. */
  private async savepoint(): Promise<number> {
    try {
      if (this.currentTransaction) {
        await this.currentTransaction.raw('SAVEPOINT SP1');
      }
      return ReturnCode.Success;
    } catch (err) {
      console.error(`Savepoint error: ${err}`);
      return ReturnCode.Warning;
    }
  }

  /** 5000-RESTORE – rollback to savepoint. */
  private async restore(): Promise<number> {
    try {
      if (this.currentTransaction) {
        await this.currentTransaction.raw('ROLLBACK TO SAVEPOINT SP1');
      }
      return ReturnCode.Success;
    } catch (err) {
      console.error(`Restore error: ${err}`);
      return ReturnCode.Warning;
    }
  }

  /** 6000-DISPLAY-STATS. */
  private displayStats(): number {
    console.log(`Commit stats: commits=${this.commitCount}, rollbacks=${this.rollbackCount}`);
    return ReturnCode.Success;
  }

  /** Get the active transaction. */
  getTransaction(): Knex.Transaction | null {
    return this.currentTransaction;
  }
}
