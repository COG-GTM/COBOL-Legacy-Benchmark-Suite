/**
 * Online DB2 Service.
 * Migrated from: src/programs/online/DB2ONLN.cbl
 *
 * Provides online database operations with connection pooling
 * and transaction management for the Express API layer.
 */

import { Knex } from 'knex';
import { ReturnCode } from '../../types';

export class Db2Online {
  private activeConnections = 0;
  private readonly maxConnections: number;

  constructor(
    private readonly db: Knex,
    maxConnections = 10,
  ) {
    this.maxConnections = maxConnections;
  }

  /** Acquire a connection from the pool. */
  async acquire(): Promise<{ rc: number; db?: Knex }> {
    if (this.activeConnections >= this.maxConnections) {
      console.error('Connection pool exhausted');
      return { rc: ReturnCode.Error };
    }

    try {
      await this.db.raw('SELECT 1');
      this.activeConnections++;
      return { rc: ReturnCode.Success, db: this.db };
    } catch (err) {
      console.error(`Failed to acquire connection: ${err}`);
      return { rc: ReturnCode.Severe };
    }
  }

  /** Release a connection back to the pool. */
  release(): number {
    if (this.activeConnections > 0) {
      this.activeConnections--;
    }
    return ReturnCode.Success;
  }

  /** Execute a query within a transaction. */
  async executeInTransaction<T>(
    fn: (trx: Knex.Transaction) => Promise<T>,
  ): Promise<{ rc: number; result?: T }> {
    try {
      const result = await this.db.transaction(async (trx) => {
        return fn(trx);
      });
      return { rc: ReturnCode.Success, result };
    } catch (err) {
      console.error(`Transaction failed: ${err}`);
      return { rc: ReturnCode.Error };
    }
  }

  /** Get current pool statistics. */
  getStats(): { active: number; max: number } {
    return { active: this.activeConnections, max: this.maxConnections };
  }
}
