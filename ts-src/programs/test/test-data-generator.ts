/**
 * Test Data Generator.
 * Migrated from: src/programs/test/TSTGEN00.cbl
 *
 * Generates test data for portfolios, transactions, error scenarios,
 * and volume testing.
 */

import Decimal from 'decimal.js';
import {
  PortfolioRecord,
  PortfolioStatus,
  TransactionRecord,
  TransactionType,
  TransactionStatus,
  ErrLogRecord,
  ErrorType,
  ReturnCode,
} from '../../types';

export type TestDataType = 'PORT' | 'TRAN' | 'ERRR' | 'VOLM';

export class TestDataGenerator {
  /** Dispatch – mirrors COBOL 0000-MAIN EVALUATE. */
  generate(dataType: TestDataType, count: number): unknown[] {
    switch (dataType) {
      case 'PORT':
        return this.generatePortfolioData(count);
      case 'TRAN':
        return this.generateTransactionData(count);
      case 'ERRR':
        return this.generateErrorData(count);
      case 'VOLM':
        return this.generateVolumeData(count);
      default:
        console.error(`Unknown data type: ${dataType}`);
        return [];
    }
  }

  /** 1000-GENERATE-PORTFOLIO-DATA. */
  private generatePortfolioData(count: number): PortfolioRecord[] {
    const records: PortfolioRecord[] = [];
    const now = new Date().toISOString().slice(0, 10).replace(/-/g, '');

    for (let i = 1; i <= count; i++) {
      records.push({
        portKey: {
          portId: `PORT${String(i).padStart(4, '0')}`,
          portAccountNo: `ACCT${String(i).padStart(6, '0')}`,
          portAccountType: i % 2 === 0 ? 'IN' : 'TR',
        },
        portStatus: i % 10 === 0 ? PortfolioStatus.Closed : PortfolioStatus.Active,
        portClientInfo: {
          portClientName: `Client ${i}`,
          portClientType: i % 3 === 0 ? 'C' : i % 3 === 1 ? 'I' : 'T',
          portBranchId: `B${String(i % 5 + 1).padStart(2, '0')}`,
        },
        portFinancialInfo: {
          portTotalValue: new Decimal(50000).plus(i * 10000).toNumber(),
          portCashBalance: new Decimal(5000).plus(i * 1000).toNumber(),
          portTotalCost: new Decimal(40000).plus(i * 8000).toNumber(),
          portTotalUnits: 500 + i * 100,
        },
        portAuditInfo: {
          portCreateDate: now,
          portLastMaint: now,
          portMaintUser: 'TSTGEN',
        },
      });
    }

    console.log(`Generated ${records.length} portfolio records`);
    return records;
  }

  /** 2000-GENERATE-TRANSACTION-DATA. */
  private generateTransactionData(count: number): TransactionRecord[] {
    const records: TransactionRecord[] = [];
    const types = [TransactionType.Buy, TransactionType.Sell, TransactionType.Transfer, TransactionType.Fee];

    for (let i = 1; i <= count; i++) {
      const type = types[i % types.length];
      const price = new Decimal(25).plus(i * 0.25);
      const qty = new Decimal(50).plus(i * 5);

      records.push({
        trnKey: {
          trnDate: '20260115',
          trnTime: `${String(9 + Math.floor(i / 100)).padStart(2, '0')}${String(i % 60).padStart(2, '0')}00`,
          trnPortfolioId: `PORT${String((i % 100) + 1).padStart(4, '0')}`,
          trnSequenceNo: i,
        },
        trnType: type,
        trnStatus: TransactionStatus.Pending,
        trnInvestmentId: `SEC${String((i % 50) + 1).padStart(5, '0')}`,
        trnQuantity: qty.toNumber(),
        trnPrice: price.toNumber(),
        trnAmount: price.times(qty).toNumber(),
        trnFees: new Decimal(7.5).toNumber(),
        trnAccountNo: `ACCT${String((i % 100) + 1).padStart(6, '0')}`,
        trnDescription: `Test transaction ${i}`,
      });
    }

    console.log(`Generated ${records.length} transaction records`);
    return records;
  }

  /** 3000-GENERATE-ERROR-DATA. */
  private generateErrorData(count: number): ErrLogRecord[] {
    const records: ErrLogRecord[] = [];
    const types = [ErrorType.System, ErrorType.Application, ErrorType.Data];

    for (let i = 1; i <= count; i++) {
      const now = new Date();
      records.push({
        elErrorTimestamp: new Date(now.getTime() + i * 1000).toISOString(),
        elProgramId: `PRG${String(i % 10).padStart(5, '0')}`,
        elErrorType: types[i % types.length],
        elErrorSeverity: (i % 4) + 1,
        elErrorCode: `ERR${String(i).padStart(5, '0')}`,
        elErrorMessage: `Test error message ${i}`,
        elProcessDate: now.toISOString().slice(0, 10).replace(/-/g, ''),
        elProcessTime: now.toISOString().slice(11, 19).replace(/:/g, ''),
        elUserId: 'TSTGEN',
        elAdditionalInfo: `Additional info for error ${i}`,
      });
    }

    console.log(`Generated ${records.length} error records`);
    return records;
  }

  /** 4000-GENERATE-VOLUME-DATA – large dataset for performance testing. */
  private generateVolumeData(count: number): PortfolioRecord[] {
    console.log(`Generating ${count} volume test records...`);
    return this.generatePortfolioData(count);
  }
}
