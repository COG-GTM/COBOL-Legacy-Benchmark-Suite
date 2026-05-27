/**
 * Database Schema / Migrations.
 * Migrated from: src/database/db2/db2-definitions.sql
 *
 * Creates tables equivalent to the DB2 PORTFOLIO_MASTER, INVESTMENT_POSITIONS,
 * TRANSACTION_HISTORY, RTNCODES, POSHIST, ERRLOG, and AUDITLOG tables.
 */

import { Knex } from 'knex';

/** Run all table-creation migrations. */
export async function createSchema(db: Knex): Promise<void> {
  // PORTFOLIO_MASTER
  if (!(await db.schema.hasTable('PORTFOLIO_MASTER'))) {
    await db.schema.createTable('PORTFOLIO_MASTER', (t) => {
      t.string('PORTFOLIO_ID', 8).primary();
      t.string('ACCOUNT_NO', 10).notNullable();
      t.string('ACCOUNT_TYPE', 2).defaultTo('');
      t.string('CLIENT_NAME', 30).defaultTo('');
      t.string('CLIENT_TYPE', 1).defaultTo('I');
      t.string('BRANCH_ID', 3).defaultTo('');
      t.string('STATUS', 1).notNullable().defaultTo('A');
      t.decimal('TOTAL_VALUE', 15, 2).defaultTo(0);
      t.decimal('CASH_BALANCE', 15, 2).defaultTo(0);
      t.decimal('TOTAL_COST', 15, 2).defaultTo(0);
      t.integer('TOTAL_UNITS').defaultTo(0);
      t.string('CREATE_DATE', 10).defaultTo('');
      t.string('LAST_MAINT_DATE', 10).defaultTo('');
      t.string('MAINT_USER', 8).defaultTo('');
    });
  }

  // INVESTMENT_POSITIONS
  if (!(await db.schema.hasTable('INVESTMENT_POSITIONS'))) {
    await db.schema.createTable('INVESTMENT_POSITIONS', (t) => {
      t.string('PORTFOLIO_ID', 8).notNullable();
      t.string('INVESTMENT_ID', 8).notNullable();
      t.string('POSITION_DATE', 10).notNullable();
      t.string('ACCOUNT_NO', 10).defaultTo('');
      t.string('DESCRIPTION', 30).defaultTo('');
      t.string('INVESTMENT_TYPE', 3).defaultTo('');
      t.string('STATUS', 1).defaultTo('A');
      t.decimal('QUANTITY', 15, 3).defaultTo(0);
      t.decimal('COST_BASIS', 15, 2).defaultTo(0);
      t.decimal('MARKET_VALUE', 15, 2).defaultTo(0);
      t.decimal('PERCENT_CHANGE', 7, 2).defaultTo(0);
      t.string('LAST_VAL_DATE', 10).defaultTo('');
      t.primary(['PORTFOLIO_ID', 'INVESTMENT_ID', 'POSITION_DATE']);
    });
  }

  // TRANSACTION_HISTORY
  if (!(await db.schema.hasTable('TRANSACTION_HISTORY'))) {
    await db.schema.createTable('TRANSACTION_HISTORY', (t) => {
      t.string('TRANS_DATE', 8).notNullable();
      t.string('TRANS_TIME', 6).notNullable();
      t.string('PORTFOLIO_ID', 8).notNullable();
      t.integer('SEQUENCE_NO').notNullable();
      t.string('TRANS_TYPE', 2).defaultTo('');
      t.string('STATUS', 1).defaultTo('P');
      t.string('INVESTMENT_ID', 8).defaultTo('');
      t.decimal('QUANTITY', 13, 2).defaultTo(0);
      t.decimal('PRICE', 13, 2).defaultTo(0);
      t.decimal('AMOUNT', 15, 2).defaultTo(0);
      t.decimal('FEES', 13, 2).defaultTo(0);
      t.string('ACCOUNT_NO', 10).defaultTo('');
      t.string('DESCRIPTION', 30).defaultTo('');
      t.primary(['TRANS_DATE', 'TRANS_TIME', 'PORTFOLIO_ID', 'SEQUENCE_NO']);
    });
  }

  // POSHIST (Position History – from POSHIST.sql)
  if (!(await db.schema.hasTable('POSHIST'))) {
    await db.schema.createTable('POSHIST', (t) => {
      t.string('ACCOUNT_NO', 10).notNullable();
      t.string('PORTFOLIO_ID', 8).notNullable();
      t.string('TRANS_DATE', 10).notNullable();
      t.string('TRANS_TIME', 8).notNullable();
      t.string('TRANS_TYPE', 4).defaultTo('');
      t.string('SECURITY_ID', 8).defaultTo('');
      t.decimal('QUANTITY', 15, 3).defaultTo(0);
      t.decimal('PRICE', 15, 3).defaultTo(0);
      t.decimal('AMOUNT', 15, 2).defaultTo(0);
      t.decimal('FEES', 15, 2).defaultTo(0);
      t.decimal('TOTAL_AMOUNT', 15, 2).defaultTo(0);
      t.decimal('COST_BASIS', 15, 2).defaultTo(0);
      t.decimal('GAIN_LOSS', 15, 2).defaultTo(0);
      t.string('PROCESS_DATE', 10).defaultTo('');
      t.string('PROCESS_TIME', 8).defaultTo('');
      t.string('USER_ID', 8).defaultTo('');
      t.primary(['ACCOUNT_NO', 'PORTFOLIO_ID', 'TRANS_DATE', 'TRANS_TIME']);
    });
  }

  // ERRLOG (Error Log – from ERRLOG.sql)
  if (!(await db.schema.hasTable('ERRLOG'))) {
    await db.schema.createTable('ERRLOG', (t) => {
      t.string('ERROR_TIMESTAMP', 26).notNullable();
      t.string('PROGRAM_ID', 8).notNullable();
      t.string('ERROR_TYPE', 1).defaultTo('S');
      t.integer('ERROR_SEVERITY').defaultTo(1);
      t.string('ERROR_CODE', 8).defaultTo('');
      t.string('ERROR_MESSAGE', 80).defaultTo('');
      t.string('PROCESS_DATE', 10).defaultTo('');
      t.string('PROCESS_TIME', 8).defaultTo('');
      t.string('USER_ID', 8).defaultTo('');
      t.string('ADDITIONAL_INFO', 100).defaultTo('');
      t.primary(['ERROR_TIMESTAMP', 'PROGRAM_ID']);
    });
  }

  // RTNCODES (Return Code Logging – from RTNCODES.sql)
  if (!(await db.schema.hasTable('RTNCODES'))) {
    await db.schema.createTable('RTNCODES', (t) => {
      t.string('TIMESTAMP', 26).notNullable();
      t.string('PROGRAM_ID', 8).notNullable();
      t.integer('RETURN_CODE').notNullable();
      t.integer('HIGHEST_CODE').notNullable();
      t.string('STATUS_CODE', 1).notNullable();
      t.string('MESSAGE_TEXT', 80).defaultTo('');
      t.primary(['TIMESTAMP', 'PROGRAM_ID']);
    });
  }

  // AUDITLOG
  if (!(await db.schema.hasTable('AUDITLOG'))) {
    await db.schema.createTable('AUDITLOG', (t) => {
      t.string('TIMESTAMP', 26).notNullable();
      t.string('USER_ID', 8).defaultTo('');
      t.string('TERMINAL_ID', 8).defaultTo('');
      t.string('TRANS_ID', 4).defaultTo('');
      t.string('PROGRAM', 8).defaultTo('');
      t.string('ACCESS_TYPE', 8).defaultTo('');
      t.string('TYPE', 4).defaultTo('');
      t.string('ACTION', 8).defaultTo('');
      t.string('STATUS', 4).defaultTo('');
      t.string('PORTFOLIO_ID', 8).defaultTo('');
      t.string('ACCOUNT_NO', 10).defaultTo('');
      t.string('BEFORE_IMAGE', 100).defaultTo('');
      t.string('AFTER_IMAGE', 100).defaultTo('');
      t.string('MESSAGE', 100).defaultTo('');
    });
  }

  // Create indexes equivalent to those in db2-definitions.sql
  try {
    await db.schema.table('PORTFOLIO_MASTER', (t) => {
      t.index(['ACCOUNT_NO'], 'IDX_PORT_ACCT');
      t.index(['CREATE_DATE'], 'IDX_PORT_DATE');
    });
  } catch {
    // Indexes may already exist
  }

  try {
    await db.schema.table('INVESTMENT_POSITIONS', (t) => {
      t.index(['POSITION_DATE'], 'IDX_POS_DATE');
    });
  } catch {
    // Indexes may already exist
  }

  try {
    await db.schema.table('RTNCODES', (t) => {
      t.index(['PROGRAM_ID', 'TIMESTAMP'], 'RTNCODES_PRG_IDX');
      t.index(['STATUS_CODE', 'TIMESTAMP'], 'RTNCODES_STS_IDX');
    });
  } catch {
    // Indexes may already exist
  }
}
