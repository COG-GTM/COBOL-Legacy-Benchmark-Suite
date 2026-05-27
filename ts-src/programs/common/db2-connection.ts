/**
 * Database Connection Manager.
 * Migrated from: src/programs/common/DB2CONN.cbl
 *
 * Manages the database connection lifecycle with retry logic:
 * connect, disconnect, and status check.
 */

import { Knex } from 'knex';
import { connectToDatabase, disconnectFromDatabase, checkDatabaseStatus } from '../../database';
import { ReturnCode } from '../../types';

export type ConnectionFunction = 'CONN' | 'DISC' | 'STAT';

export class Db2Connection {
  private retryCount = 0;
  private readonly maxRetries = 3;
  private readonly retryWaitMs = 2000;
  private connected = false;

  /** Dispatch – mirrors COBOL 0000-MAIN EVALUATE. */
  async execute(func: ConnectionFunction): Promise<{ rc: number; db?: Knex }> {
    switch (func) {
      case 'CONN':
        return this.connect();
      case 'DISC':
        return this.disconnect();
      case 'STAT':
        return this.checkStatus();
      default:
        console.error(`Invalid function code: ${func}`);
        return { rc: ReturnCode.Error };
    }
  }

  /** 1000-CONNECT – connect with retry logic. */
  private async connect(): Promise<{ rc: number; db?: Knex }> {
    this.retryCount = 0;

    while (this.retryCount <= this.maxRetries) {
      try {
        const db = await connectToDatabase();
        this.connected = true;
        console.log('Database connection established');
        return { rc: ReturnCode.Success, db };
      } catch (err) {
        this.retryCount++;
        console.error(
          `Connection attempt ${this.retryCount}/${this.maxRetries} failed: ${err}`,
        );
        if (this.retryCount <= this.maxRetries) {
          await this.sleep(this.retryWaitMs);
        }
      }
    }

    console.error('All connection attempts exhausted');
    return { rc: ReturnCode.Severe };
  }

  /** 2000-DISCONNECT. */
  private async disconnect(): Promise<{ rc: number }> {
    try {
      await disconnectFromDatabase();
      this.connected = false;
      console.log('Database connection closed');
      return { rc: ReturnCode.Success };
    } catch (err) {
      console.error(`Error disconnecting: ${err}`);
      return { rc: ReturnCode.Error };
    }
  }

  /** 3000-CHECK-STATUS. */
  private async checkStatus(): Promise<{ rc: number }> {
    const ok = await checkDatabaseStatus();
    if (ok) {
      return { rc: ReturnCode.Success };
    }
    console.error('Database connection is not active');
    return { rc: ReturnCode.Error };
  }

  private sleep(ms: number): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, ms));
  }
}
