/**
 * Database Service – unified façade over SQL and VSAM operations.
 * Migrated from: conceptual aggregation of DB2 + VSAM access patterns.
 *
 * Wraps Knex (SQL) and VsamStore (file-based KV) behind a single service.
 */

import { Knex } from 'knex';
import { connectToDatabase, disconnectFromDatabase, getDatabase } from './connection';
import { createSchema } from './schema';
import { VsamStore } from './vsam-store';
import { PortfolioRecord, TransactionRecord, PositionRecord } from '../types';

export class DatabaseService {
  private db: Knex | null = null;

  /** VSAM stores – keyed by their record-key extractor. */
  public readonly portfolioStore = new VsamStore<PortfolioRecord>(
    (r) => `${r.portKey.portId}${r.portKey.portAccountNo}`,
  );

  public readonly transactionStore = new VsamStore<TransactionRecord>(
    (r) => `${r.trnKey.trnDate}${r.trnKey.trnTime}${r.trnKey.trnPortfolioId}${String(r.trnKey.trnSequenceNo).padStart(4, '0')}`,
  );

  public readonly positionStore = new VsamStore<PositionRecord>(
    (r) => `${r.posKey.posPortfolioId}${r.posKey.posDate}${r.posKey.posInvestmentId}`,
  );

  /** Initialize the database and run migrations. */
  async initialize(dbPath?: string): Promise<void> {
    this.db = await connectToDatabase(dbPath);
    await createSchema(this.db);
  }

  /** Get the Knex instance. */
  getDb(): Knex {
    if (!this.db) {
      return getDatabase();
    }
    return this.db;
  }

  /** Shut down gracefully. */
  async shutdown(): Promise<void> {
    await disconnectFromDatabase();
    this.db = null;
  }
}

/** Singleton instance. */
let instance: DatabaseService | null = null;

export function getDatabaseService(): DatabaseService {
  if (!instance) {
    instance = new DatabaseService();
  }
  return instance;
}
