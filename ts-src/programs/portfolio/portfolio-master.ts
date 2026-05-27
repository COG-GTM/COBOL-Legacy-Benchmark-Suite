/**
 * Portfolio Master File Maintenance.
 * Migrated from: src/programs/portfolio/PORTMSTR.cbl
 *
 * CRUD operations on the VSAM portfolio master file:
 * CREATE, READ, UPDATE, DELETE.
 */

import {
  PortfolioRecord,
  PortfolioStatus,
  ReturnCode,
} from '../../types';
import { VsamStore, VsamError } from '../../database/vsam-store';

export type MasterFunction = 'CREA' | 'READ' | 'UPDT' | 'DELE';

export class PortfolioMaster {
  constructor(private readonly store: VsamStore<PortfolioRecord>) {}

  /** Dispatch – mirrors COBOL 0000-MAIN EVALUATE. */
  execute(func: MasterFunction, record: PortfolioRecord): { rc: number; record?: PortfolioRecord } {
    switch (func) {
      case 'CREA':
        return this.createRecord(record);
      case 'READ':
        return this.readRecord(record);
      case 'UPDT':
        return this.updateRecord(record);
      case 'DELE':
        return this.deleteRecord(record);
      default:
        console.error(`Invalid function code: ${func}`);
        return { rc: ReturnCode.Error };
    }
  }

  /** 1000-CREATE-RECORD. */
  private createRecord(record: PortfolioRecord): { rc: number } {
    try {
      const now = new Date().toISOString().slice(0, 10).replace(/-/g, '');
      record.portAuditInfo.portCreateDate = now;
      record.portAuditInfo.portLastMaint = now;
      record.portStatus = PortfolioStatus.Active;

      this.store.write(record);
      console.log(`Portfolio ${record.portKey.portId} created`);
      return { rc: ReturnCode.Success };
    } catch (err) {
      if (err instanceof VsamError && err.statusCode === '22') {
        console.error(`Duplicate portfolio: ${record.portKey.portId}`);
        return { rc: ReturnCode.Warning };
      }
      console.error(`Create error: ${err}`);
      return { rc: ReturnCode.Error };
    }
  }

  /** 2000-READ-RECORD. */
  private readRecord(record: PortfolioRecord): { rc: number; record?: PortfolioRecord } {
    const key = `${record.portKey.portId}${record.portKey.portAccountNo}`;
    const found = this.store.read(key);
    if (!found) {
      return { rc: ReturnCode.Warning };
    }
    return { rc: ReturnCode.Success, record: found };
  }

  /** 3000-UPDATE-RECORD. */
  private updateRecord(record: PortfolioRecord): { rc: number } {
    try {
      record.portAuditInfo.portLastMaint = new Date().toISOString().slice(0, 10).replace(/-/g, '');
      this.store.rewrite(record);
      console.log(`Portfolio ${record.portKey.portId} updated`);
      return { rc: ReturnCode.Success };
    } catch (err) {
      if (err instanceof VsamError && err.statusCode === '23') {
        console.error(`Portfolio not found: ${record.portKey.portId}`);
        return { rc: ReturnCode.Warning };
      }
      console.error(`Update error: ${err}`);
      return { rc: ReturnCode.Error };
    }
  }

  /** 4000-DELETE-RECORD. */
  private deleteRecord(record: PortfolioRecord): { rc: number } {
    const key = `${record.portKey.portId}${record.portKey.portAccountNo}`;
    try {
      this.store.delete(key);
      console.log(`Portfolio ${record.portKey.portId} deleted`);
      return { rc: ReturnCode.Success };
    } catch (err) {
      if (err instanceof VsamError && err.statusCode === '23') {
        console.error(`Portfolio not found for delete: ${record.portKey.portId}`);
        return { rc: ReturnCode.Warning };
      }
      console.error(`Delete error: ${err}`);
      return { rc: ReturnCode.Error };
    }
  }
}
