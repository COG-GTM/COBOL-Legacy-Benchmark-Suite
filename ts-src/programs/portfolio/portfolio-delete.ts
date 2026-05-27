/**
 * Portfolio Batch Delete.
 * Migrated from: src/programs/portfolio/PORTDEL.cbl
 *
 * Reads a list of portfolio keys and deletes them from the VSAM store,
 * recording audit entries for each deletion.
 */

import {
  PortfolioRecord,
  AuditAction,
  ReturnCode,
} from '../../types';
import { VsamStore, VsamError } from '../../database/vsam-store';

export class PortfolioDelete {
  private deletedCount = 0;
  private notFoundCount = 0;
  private errorCount = 0;

  constructor(private readonly store: VsamStore<PortfolioRecord>) {}

  /** Main entry point – mirrors COBOL 0000-MAIN. */
  run(deleteKeys: { portId: string; portAccountNo: string }[]): number {
    this.deletedCount = 0;
    this.notFoundCount = 0;
    this.errorCount = 0;

    for (const key of deleteKeys) {
      this.processDelete(key);
    }

    console.log(
      `Portfolio delete complete: deleted=${this.deletedCount}, ` +
      `notFound=${this.notFoundCount}, errors=${this.errorCount}`,
    );

    return this.errorCount > 0 ? ReturnCode.Error : ReturnCode.Success;
  }

  /** 2000-PROCESS-DELETE. */
  private processDelete(key: { portId: string; portAccountNo: string }): void {
    const compositeKey = `${key.portId}${key.portAccountNo}`;

    try {
      this.store.delete(compositeKey);
      this.deletedCount++;
      console.log(`Deleted portfolio ${key.portId} (action=${AuditAction.Delete})`);
    } catch (err) {
      if (err instanceof VsamError && err.statusCode === '23') {
        this.notFoundCount++;
        console.log(`Portfolio ${key.portId} not found for deletion`);
      } else {
        this.errorCount++;
        console.error(`Error deleting portfolio ${key.portId}: ${err}`);
      }
    }
  }

  getCounts(): { deleted: number; notFound: number; errors: number } {
    return { deleted: this.deletedCount, notFound: this.notFoundCount, errors: this.errorCount };
  }
}
