/**
 * Position History Database Operations.
 * Migrated from: src/database/db2/POSHIST.sql
 *
 * Provides typed insert/query helpers for the POSHIST table.
 */

import { Knex } from 'knex';
import { PosHistRecord } from '../types';

/** Insert a position-history record. */
export async function insertPosHist(db: Knex, record: PosHistRecord): Promise<void> {
  await db('POSHIST').insert({
    ACCOUNT_NO: record.phAccountNo,
    PORTFOLIO_ID: record.phPortfolioId,
    TRANS_DATE: record.phTransDate,
    TRANS_TIME: record.phTransTime,
    TRANS_TYPE: record.phTransType,
    SECURITY_ID: record.phSecurityId,
    QUANTITY: record.phQuantity,
    PRICE: record.phPrice,
    AMOUNT: record.phAmount,
    FEES: record.phFees,
    TOTAL_AMOUNT: record.phTotalAmount,
    COST_BASIS: record.phCostBasis,
    GAIN_LOSS: record.phGainLoss,
    PROCESS_DATE: record.phProcessDate,
    PROCESS_TIME: record.phProcessTime,
    USER_ID: record.phUserId,
  });
}

/** Query position history for an account, ordered by date descending. */
export async function queryPosHistByAccount(
  db: Knex,
  accountNo: string,
  limit = 100,
): Promise<PosHistRecord[]> {
  const rows = await db('POSHIST')
    .where('ACCOUNT_NO', accountNo)
    .orderBy('TRANS_DATE', 'desc')
    .limit(limit);

  return rows.map(rowToPosHist);
}

/** Query position history for a portfolio. */
export async function queryPosHistByPortfolio(
  db: Knex,
  portfolioId: string,
  limit = 100,
): Promise<PosHistRecord[]> {
  const rows = await db('POSHIST')
    .where('PORTFOLIO_ID', portfolioId)
    .orderBy('TRANS_DATE', 'desc')
    .limit(limit);

  return rows.map(rowToPosHist);
}

function rowToPosHist(row: Record<string, unknown>): PosHistRecord {
  return {
    phAccountNo: String(row['ACCOUNT_NO'] ?? ''),
    phPortfolioId: String(row['PORTFOLIO_ID'] ?? ''),
    phTransDate: String(row['TRANS_DATE'] ?? ''),
    phTransTime: String(row['TRANS_TIME'] ?? ''),
    phTransType: String(row['TRANS_TYPE'] ?? ''),
    phSecurityId: String(row['SECURITY_ID'] ?? ''),
    phQuantity: Number(row['QUANTITY'] ?? 0),
    phPrice: Number(row['PRICE'] ?? 0),
    phAmount: Number(row['AMOUNT'] ?? 0),
    phFees: Number(row['FEES'] ?? 0),
    phTotalAmount: Number(row['TOTAL_AMOUNT'] ?? 0),
    phCostBasis: Number(row['COST_BASIS'] ?? 0),
    phGainLoss: Number(row['GAIN_LOSS'] ?? 0),
    phProcessDate: String(row['PROCESS_DATE'] ?? ''),
    phProcessTime: String(row['PROCESS_TIME'] ?? ''),
    phUserId: String(row['USER_ID'] ?? ''),
  };
}
