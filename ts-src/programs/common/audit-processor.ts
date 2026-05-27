/**
 * Audit Trail Processor.
 * Migrated from: src/programs/common/AUDPROC.cbl
 *
 * Writes audit trail records to the AUDITLOG table.
 */

import { Knex } from 'knex';
import {
  AuditRecord,
  AuditType,
  AuditAction,
  AuditStatus,
  ReturnCode,
} from '../../types';

export class AuditProcessor {
  private recordCount = 0;

  constructor(private readonly db: Knex) {}

  /** Write an audit record. */
  async writeAudit(record: AuditRecord): Promise<number> {
    try {
      await this.db('AUDITLOG').insert({
        TIMESTAMP: record.audTimestamp || new Date().toISOString(),
        USER_ID: record.audHeader.audUserId,
        TERMINAL_ID: record.audHeader.audTerminal,
        PROGRAM: record.audHeader.audProgram,
        TYPE: record.audType,
        ACTION: record.audAction,
        STATUS: record.audStatus,
        PORTFOLIO_ID: record.audKeyInfo.audPortfolioId,
        ACCOUNT_NO: record.audKeyInfo.audAccountNo,
        BEFORE_IMAGE: record.audBeforeImage.slice(0, 100),
        AFTER_IMAGE: record.audAfterImage.slice(0, 100),
        MESSAGE: record.audMessage.slice(0, 100),
      });

      this.recordCount++;
      return ReturnCode.Success;
    } catch (err) {
      console.error(`Failed to write audit record: ${err}`);
      return ReturnCode.Error;
    }
  }

  /** Convenience: create and write a transaction audit record. */
  async auditTransaction(
    userId: string,
    program: string,
    action: AuditAction,
    portfolioId: string,
    accountNo: string,
    message: string,
    status: AuditStatus = AuditStatus.Success,
  ): Promise<number> {
    const record: AuditRecord = {
      audTimestamp: new Date().toISOString(),
      audHeader: {
        audSystemId: 'PMS',
        audUserId: userId,
        audProgram: program,
        audTerminal: '',
      },
      audType: AuditType.Transaction,
      audAction: action,
      audStatus: status,
      audKeyInfo: { audPortfolioId: portfolioId, audAccountNo: accountNo },
      audBeforeImage: '',
      audAfterImage: '',
      audMessage: message,
    };

    return this.writeAudit(record);
  }

  /** Get count of records written. */
  getRecordCount(): number {
    return this.recordCount;
  }
}
