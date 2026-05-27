/**
 * Portfolio Batch Update.
 * Migrated from: src/programs/portfolio/PORTUPDT.cbl
 *
 * Reads update records and applies them to existing portfolios
 * in the VSAM store.
 */

import {
  PortfolioRecord,
  ReturnCode,
} from '../../types';
import { VsamStore, VsamError } from '../../database/vsam-store';

export class PortfolioUpdate {
  private updatedCount = 0;
  private notFoundCount = 0;
  private errorCount = 0;

  constructor(private readonly store: VsamStore<PortfolioRecord>) {}

  /** Main entry point – mirrors COBOL 0000-MAIN. */
  run(updateRecords: Partial<PortfolioRecord> & { portKey: PortfolioRecord['portKey'] }[]): number {
    this.updatedCount = 0;
    this.notFoundCount = 0;
    this.errorCount = 0;

    for (const update of updateRecords) {
      this.processUpdate(update);
    }

    console.log(
      `Portfolio update complete: updated=${this.updatedCount}, ` +
      `notFound=${this.notFoundCount}, errors=${this.errorCount}`,
    );

    return this.errorCount > 0 ? ReturnCode.Error : ReturnCode.Success;
  }

  /** 2000-PROCESS-UPDATE. */
  private processUpdate(
    update: Partial<PortfolioRecord> & { portKey: PortfolioRecord['portKey'] },
  ): void {
    const key = `${update.portKey.portId}${update.portKey.portAccountNo}`;
    const existing = this.store.read(key);

    if (!existing) {
      this.notFoundCount++;
      console.log(`Portfolio ${update.portKey.portId} not found for update`);
      return;
    }

    try {
      // Apply updates
      if (update.portStatus !== undefined) existing.portStatus = update.portStatus;
      if (update.portClientInfo) {
        Object.assign(existing.portClientInfo, update.portClientInfo);
      }
      if (update.portFinancialInfo) {
        Object.assign(existing.portFinancialInfo, update.portFinancialInfo);
      }

      existing.portAuditInfo.portLastMaint = new Date().toISOString().slice(0, 10).replace(/-/g, '');
      if (update.portAuditInfo?.portMaintUser) {
        existing.portAuditInfo.portMaintUser = update.portAuditInfo.portMaintUser;
      }

      this.store.rewrite(existing);
      this.updatedCount++;
    } catch (err) {
      this.errorCount++;
      console.error(`Error updating portfolio ${update.portKey.portId}: ${err}`);
    }
  }

  getCounts(): { updated: number; notFound: number; errors: number } {
    return { updated: this.updatedCount, notFound: this.notFoundCount, errors: this.errorCount };
  }
}
