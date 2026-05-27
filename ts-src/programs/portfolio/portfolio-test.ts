/**
 * Portfolio Test Data Generator.
 * Migrated from: src/programs/portfolio/PORTTEST.cbl
 *
 * Generates sample portfolio, position, and transaction records
 * for testing and demonstration purposes.
 */

import Decimal from 'decimal.js';
import {
  PortfolioRecord,
  PortfolioStatus,
  TransactionRecord,
  TransactionType,
  TransactionStatus,
  PositionRecord,
  PositionStatus,
} from '../../types';

export class PortfolioTest {
  /** Generate N sample portfolio records. */
  generatePortfolios(count: number): PortfolioRecord[] {
    const records: PortfolioRecord[] = [];
    const now = new Date().toISOString().slice(0, 10).replace(/-/g, '');

    for (let i = 1; i <= count; i++) {
      const id = `PORT${String(i).padStart(4, '0')}`;
      const acct = `ACCT${String(i).padStart(6, '0')}`;

      records.push({
        portKey: {
          portId: id,
          portAccountNo: acct,
          portAccountType: 'IN',
        },
        portStatus: PortfolioStatus.Active,
        portClientInfo: {
          portClientName: `Test Client ${i}`,
          portClientType: i % 3 === 0 ? 'C' : 'I',
          portBranchId: `BR${String((i % 10) + 1).padStart(1, '0')}`,
        },
        portFinancialInfo: {
          portTotalValue: new Decimal(100000).times(i).toNumber(),
          portCashBalance: new Decimal(10000).times(i).toNumber(),
          portTotalCost: new Decimal(80000).times(i).toNumber(),
          portTotalUnits: i * 1000,
        },
        portAuditInfo: {
          portCreateDate: now,
          portLastMaint: now,
          portMaintUser: 'TESTGEN',
        },
      });
    }

    return records;
  }

  /** Generate sample transactions for a portfolio. */
  generateTransactions(portfolioId: string, accountNo: string, count: number): TransactionRecord[] {
    const records: TransactionRecord[] = [];
    const types = [TransactionType.Buy, TransactionType.Sell, TransactionType.Fee];

    for (let i = 1; i <= count; i++) {
      const trnType = types[i % types.length];
      const price = new Decimal(50).plus(new Decimal(i).times(0.5));
      const quantity = new Decimal(100).plus(i * 10);
      const amount = price.times(quantity);

      records.push({
        trnKey: {
          trnDate: '20260101',
          trnTime: `${String(Math.floor(i / 60)).padStart(2, '0')}${String(i % 60).padStart(2, '0')}00`,
          trnPortfolioId: portfolioId,
          trnSequenceNo: i,
        },
        trnType: trnType,
        trnStatus: TransactionStatus.Pending,
        trnInvestmentId: `SEC${String(i % 20 + 1).padStart(5, '0')}`,
        trnQuantity: quantity.toNumber(),
        trnPrice: price.toNumber(),
        trnAmount: amount.toNumber(),
        trnFees: new Decimal(9.99).toNumber(),
        trnAccountNo: accountNo,
        trnDescription: `Test transaction ${i}`,
      });
    }

    return records;
  }

  /** Generate sample position records. */
  generatePositions(portfolioId: string, count: number): PositionRecord[] {
    const records: PositionRecord[] = [];
    const investTypes = ['STK', 'BND', 'MMF', 'ETF'];

    for (let i = 1; i <= count; i++) {
      const qty = new Decimal(100).plus(i * 50);
      const cost = new Decimal(5000).plus(i * 1000);
      const mv = cost.times(1 + (i % 10) * 0.01);

      records.push({
        posKey: {
          posPortfolioId: portfolioId,
          posDate: '20260101',
          posInvestmentId: `SEC${String(i).padStart(5, '0')}`,
        },
        posStatus: PositionStatus.Active,
        posAccountNo: `ACCT${String(i).padStart(6, '0')}`,
        posDescription: `Security ${i}`,
        posInvestmentType: investTypes[i % investTypes.length],
        posQuantity: qty.toNumber(),
        posCostBasis: cost.toNumber(),
        posMarketValue: mv.toNumber(),
        posPercentChange: mv.minus(cost).div(cost).times(100).toNumber(),
        posLastValDate: '20260101',
      });
    }

    return records;
  }
}
