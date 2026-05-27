/**
 * Portfolio Batch Add.
 * Migrated from: src/programs/portfolio/PORTADD.cbl
 *
 * Reads portfolio records from an input array, validates them,
 * and adds them to the VSAM store.  Counts successes and duplicates.
 */

import {
  PortfolioRecord,
  PortfolioStatus,
  ReturnCode,
} from '../../types';
import { VsamStore, VsamError } from '../../database/vsam-store';
import { PortfolioValidation } from './portfolio-validation';

export class PortfolioAdd {
  private addedCount = 0;
  private duplicateCount = 0;
  private errorCount = 0;

  constructor(
    private readonly store: VsamStore<PortfolioRecord>,
    private readonly validator: PortfolioValidation,
  ) {}

  /** Main entry point – mirrors COBOL 0000-MAIN. */
  run(inputRecords: PortfolioRecord[]): number {
    this.addedCount = 0;
    this.duplicateCount = 0;
    this.errorCount = 0;

    for (const record of inputRecords) {
      this.processRecord(record);
    }

    console.log(
      `Portfolio add complete: added=${this.addedCount}, ` +
      `duplicates=${this.duplicateCount}, errors=${this.errorCount}`,
    );

    return this.errorCount > 0 ? ReturnCode.Warning : ReturnCode.Success;
  }

  /** 2000-PROCESS-RECORD. */
  private processRecord(record: PortfolioRecord): void {
    // Validate
    const valResult = this.validator.validatePortfolioId(record.portKey.portId);
    if (valResult !== ReturnCode.Success) {
      this.errorCount++;
      return;
    }

    const acctResult = this.validator.validateAccountNo(record.portKey.portAccountNo);
    if (acctResult !== ReturnCode.Success) {
      this.errorCount++;
      return;
    }

    // Set defaults
    const now = new Date().toISOString().slice(0, 10).replace(/-/g, '');
    record.portAuditInfo.portCreateDate = now;
    record.portAuditInfo.portLastMaint = now;
    record.portStatus = PortfolioStatus.Active;

    try {
      this.store.write(record);
      this.addedCount++;
    } catch (err) {
      if (err instanceof VsamError && err.statusCode === '22') {
        this.duplicateCount++;
      } else {
        this.errorCount++;
      }
    }
  }

  getCounts(): { added: number; duplicates: number; errors: number } {
    return { added: this.addedCount, duplicates: this.duplicateCount, errors: this.errorCount };
  }
}
