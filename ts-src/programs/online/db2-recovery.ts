/**
 * Online DB2 Recovery Handler.
 * Migrated from: src/programs/online/DB2RECV.cbl
 *
 * Handles connection recovery, transaction recovery, and cursor recovery
 * for the online layer.
 */

import { Knex } from 'knex';
import { connectToDatabase, checkDatabaseStatus } from '../../database';
import { ReturnCode } from '../../types';

export type RecoveryType = 'CONNECTION' | 'TRANSACTION' | 'CURSOR';

export class Db2Recovery {
  private readonly maxRetries = 3;
  private readonly retryDelayMs = 1000;

  constructor(private readonly db: Knex) {}

  /** Dispatch recovery action. */
  async recover(type: RecoveryType): Promise<number> {
    switch (type) {
      case 'CONNECTION':
        return this.recoverConnection();
      case 'TRANSACTION':
        return this.recoverTransaction();
      case 'CURSOR':
        return this.recoverCursor();
      default:
        return ReturnCode.Error;
    }
  }

  /** 1000-RECOVER-CONNECTION – re-establish database connection. */
  private async recoverConnection(): Promise<number> {
    for (let attempt = 1; attempt <= this.maxRetries; attempt++) {
      try {
        const isOk = await checkDatabaseStatus();
        if (isOk) {
          console.log('Connection recovery: connection is active');
          return ReturnCode.Success;
        }

        console.log(`Connection recovery attempt ${attempt}/${this.maxRetries}`);
        await connectToDatabase();
        return ReturnCode.Success;
      } catch (err) {
        console.error(`Connection recovery attempt ${attempt} failed: ${err}`);
        if (attempt < this.maxRetries) {
          await this.sleep(this.retryDelayMs * attempt);
        }
      }
    }

    console.error('Connection recovery failed after all retries');
    return ReturnCode.Severe;
  }

  /** 2000-RECOVER-TRANSACTION – rollback any pending transaction. */
  private async recoverTransaction(): Promise<number> {
    try {
      // Attempt to rollback any pending work
      await this.db.raw('ROLLBACK');
      console.log('Transaction recovery: rolled back pending work');
      return ReturnCode.Success;
    } catch {
      // No pending transaction to rollback – that's OK
      return ReturnCode.Success;
    }
  }

  /** 3000-RECOVER-CURSOR – cursors are in-memory, just log. */
  private recoverCursor(): number {
    console.log('Cursor recovery: in-memory cursors cleared on reconnect');
    return ReturnCode.Success;
  }

  private sleep(ms: number): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, ms));
  }
}
