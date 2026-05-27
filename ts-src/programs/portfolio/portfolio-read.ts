/**
 * Portfolio Sequential Read / Display.
 * Migrated from: src/programs/portfolio/PORTREAD.cbl
 *
 * Sequentially reads all portfolio records from the VSAM store
 * and displays them.
 */

import Decimal from 'decimal.js';
import {
  PortfolioRecord,
  ReturnCode,
} from '../../types';
import { VsamStore } from '../../database/vsam-store';

export class PortfolioRead {
  private recordCount = 0;

  constructor(private readonly store: VsamStore<PortfolioRecord>) {}

  /** Main entry point – mirrors COBOL 0000-MAIN. */
  run(): number {
    this.recordCount = 0;
    this.printHeader();

    this.store.startBrowse();
    let record = this.store.readNext();

    while (record) {
      this.displayRecord(record);
      this.recordCount++;
      record = this.store.readNext();
    }

    this.store.endBrowse();
    this.printFooter();

    return ReturnCode.Success;
  }

  /** 2000-DISPLAY-RECORD. */
  private displayRecord(rec: PortfolioRecord): void {
    const totalValue = new Decimal(rec.portFinancialInfo.portTotalValue);
    const cashBalance = new Decimal(rec.portFinancialInfo.portCashBalance);

    console.log(
      `${rec.portKey.portId.padEnd(10)} ` +
      `${rec.portKey.portAccountNo.padEnd(12)} ` +
      `${rec.portClientInfo.portClientName.padEnd(30)} ` +
      `${String(rec.portStatus).padEnd(3)} ` +
      `${totalValue.toFixed(2).padStart(16)} ` +
      `${cashBalance.toFixed(2).padStart(16)}`,
    );
  }

  private printHeader(): void {
    console.log('='.repeat(90));
    console.log('PORTFOLIO MASTER FILE LISTING');
    console.log('='.repeat(90));
    console.log(
      'Port ID    Account No   Client Name                    Sts    Total Value     Cash Balance',
    );
    console.log('-'.repeat(90));
  }

  private printFooter(): void {
    console.log('-'.repeat(90));
    console.log(`Total records: ${this.recordCount}`);
    console.log('='.repeat(90));
  }

  getRecordCount(): number {
    return this.recordCount;
  }
}
