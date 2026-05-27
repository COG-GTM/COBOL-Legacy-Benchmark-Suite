/**
 * History Loader.
 * Migrated from: src/programs/batch/HISTLD00.cbl
 *
 * Reads transaction records from the VSAM transaction store and
 * inserts them into the POSHIST DB2 table.  Supports checkpoint/restart.
 */

import { Knex } from 'knex';
import Decimal from 'decimal.js';
import {
  TransactionRecord,
  PosHistRecord,
  ReturnCode,
} from '../../types';
import { VsamStore } from '../../database/vsam-store';
import { insertPosHist } from '../../database/position-history';
import { CheckpointRestart } from './checkpoint-restart';

export class HistoryLoader {
  private checkpoint: CheckpointRestart;

  constructor(
    private readonly db: Knex,
    private readonly transactionStore: VsamStore<TransactionRecord>,
  ) {
    this.checkpoint = new CheckpointRestart();
    this.checkpoint.setProgramId('HISTLD00');
    this.checkpoint.setCommitFrequency(500);
  }

  /** Main entry point – mirrors COBOL 0000-MAIN. */
  async run(): Promise<number> {
    let rc = this.checkpoint.execute('INIT');
    if (rc > ReturnCode.Warning) return rc;

    rc = await this.processTransactions();

    this.checkpoint.markComplete();
    this.checkpoint.execute('CMIT');

    const counters = this.checkpoint.getCounters();
    console.log(
      `History loader complete: read=${counters.recordsRead}, ` +
      `processed=${counters.recordsProcessed}, errors=${counters.recordsInError}`,
    );

    return rc;
  }

  /** 2000-PROCESS-TRANSACTIONS – iterate over VSAM records. */
  private async processTransactions(): Promise<number> {
    let overallRc = ReturnCode.Success;

    this.transactionStore.startBrowse();
    let record = this.transactionStore.readNext();

    while (record) {
      this.checkpoint.incrementRead();

      try {
        const posHist = this.mapToPosHist(record);
        await insertPosHist(this.db, posHist);
        this.checkpoint.incrementProcessed();
      } catch (err) {
        this.checkpoint.incrementError();
        console.error(`Error loading transaction: ${err}`);

        if (this.checkpoint.isErrorLimitExceeded()) {
          console.error('Error limit exceeded – aborting');
          overallRc = ReturnCode.Severe;
          break;
        }
        overallRc = Math.max(overallRc, ReturnCode.Warning);
      }

      if (this.checkpoint.shouldCommit()) {
        this.checkpoint.execute('TAKE');
        this.checkpoint.execute('CMIT');
      }

      record = this.transactionStore.readNext();
    }

    this.transactionStore.endBrowse();
    return overallRc;
  }

  /** 3000-MAP-RECORD – translate TransactionRecord → PosHistRecord. */
  private mapToPosHist(trn: TransactionRecord): PosHistRecord {
    const amount = new Decimal(trn.trnAmount);
    const fees = new Decimal(trn.trnFees);
    const totalAmount = amount.plus(fees);

    const now = new Date();

    return {
      phAccountNo: trn.trnAccountNo,
      phPortfolioId: trn.trnKey.trnPortfolioId,
      phTransDate: trn.trnKey.trnDate,
      phTransTime: trn.trnKey.trnTime,
      phTransType: trn.trnType,
      phSecurityId: trn.trnInvestmentId,
      phQuantity: trn.trnQuantity,
      phPrice: trn.trnPrice,
      phAmount: amount.toNumber(),
      phFees: fees.toNumber(),
      phTotalAmount: totalAmount.toNumber(),
      phCostBasis: amount.toNumber(),
      phGainLoss: 0,
      phProcessDate: this.formatDate(now),
      phProcessTime: this.formatTime(now),
      phUserId: 'BATCH',
    };
  }

  private formatDate(d: Date): string {
    return d.toISOString().slice(0, 10).replace(/-/g, '');
  }

  private formatTime(d: Date): string {
    return d.toISOString().slice(11, 19).replace(/:/g, '');
  }
}
