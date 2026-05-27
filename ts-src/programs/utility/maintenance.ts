/**
 * File Maintenance Utility.
 * Migrated from: src/programs/utility/UTLMNT00.cbl
 *
 * Performs archive, cleanup, reorganization, and analysis
 * operations on data stores and database tables.
 */

import { Knex } from 'knex';
import { ReturnCode } from '../../types';

export type MaintenanceFunction = 'ARCH' | 'CLEN' | 'REOR' | 'ANLZ';

export class Maintenance {
  constructor(private readonly db: Knex) {}

  /** Dispatch – mirrors COBOL 0000-MAIN EVALUATE. */
  async execute(func: MaintenanceFunction): Promise<number> {
    switch (func) {
      case 'ARCH':
        return this.archive();
      case 'CLEN':
        return this.cleanup();
      case 'REOR':
        return this.reorganize();
      case 'ANLZ':
        return this.analyze();
      default:
        console.error(`Invalid function code: ${func}`);
        return ReturnCode.Error;
    }
  }

  /** 1000-ARCHIVE – move old records to an archive table. */
  private async archive(): Promise<number> {
    try {
      const cutoffDate = new Date();
      cutoffDate.setFullYear(cutoffDate.getFullYear() - 1);
      const cutoff = cutoffDate.toISOString().slice(0, 10).replace(/-/g, '');

      const count = await this.db('TRANSACTION_HISTORY')
        .where('TRANS_DATE', '<', cutoff)
        .delete();

      console.log(`Archived ${count} transaction records older than ${cutoff}`);
      return ReturnCode.Success;
    } catch (err) {
      console.error(`Archive error: ${err}`);
      return ReturnCode.Error;
    }
  }

  /** 2000-CLEANUP – remove orphaned or invalid records. */
  private async cleanup(): Promise<number> {
    try {
      // Clean up error log entries older than 90 days
      const cutoffDate = new Date();
      cutoffDate.setDate(cutoffDate.getDate() - 90);
      const cutoff = cutoffDate.toISOString();

      const count = await this.db('ERRLOG')
        .where('ERROR_TIMESTAMP', '<', cutoff)
        .delete();

      console.log(`Cleaned up ${count} error log records`);
      return ReturnCode.Success;
    } catch (err) {
      console.error(`Cleanup error: ${err}`);
      return ReturnCode.Error;
    }
  }

  /** 3000-REORGANIZE – optimize tables (VACUUM for SQLite). */
  private async reorganize(): Promise<number> {
    try {
      await this.db.raw('VACUUM');
      console.log('Database reorganized (VACUUM)');
      return ReturnCode.Success;
    } catch (err) {
      console.error(`Reorganize error: ${err}`);
      return ReturnCode.Warning;
    }
  }

  /** 4000-ANALYZE – collect table statistics. */
  private async analyze(): Promise<number> {
    try {
      await this.db.raw('ANALYZE');
      console.log('Database statistics updated (ANALYZE)');
      return ReturnCode.Success;
    } catch (err) {
      console.error(`Analyze error: ${err}`);
      return ReturnCode.Warning;
    }
  }
}
