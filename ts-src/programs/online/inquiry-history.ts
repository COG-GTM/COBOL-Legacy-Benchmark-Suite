/**
 * History Inquiry Handler.
 * Migrated from: src/programs/online/INQHIST.cbl
 *
 * Queries the POSHIST table (DB2 cursor replacement) and returns
 * transaction history for a given account.
 */

import { Knex } from 'knex';
import { HistoryInquiryResponse, HistoryLineItem } from './api-schemas';
import { ReturnCode } from '../../types';

export class InquiryHistory {
  constructor(private readonly db: Knex) {}

  /** Handle a history inquiry – mirrors COBOL INQHIST PROCEDURE DIVISION. */
  async inquire(
    accountNo: string,
    startDate?: string,
    endDate?: string,
    limit = 50,
  ): Promise<{ rc: number; data: HistoryInquiryResponse }> {
    const response: HistoryInquiryResponse = {
      accountNo,
      transactions: [],
      totalCount: 0,
    };

    try {
      let query = this.db('POSHIST')
        .where('ACCOUNT_NO', accountNo)
        .orderBy('TRANS_DATE', 'desc')
        .orderBy('TRANS_TIME', 'desc');

      if (startDate) query = query.where('TRANS_DATE', '>=', startDate);
      if (endDate) query = query.where('TRANS_DATE', '<=', endDate);

      // Get total count before limit
      const countResult = await query.clone().count('* as cnt').first();
      response.totalCount = Number(countResult?.cnt ?? 0);

      // Fetch limited rows
      const rows = await query.limit(limit);

      response.transactions = rows.map((row): HistoryLineItem => ({
        date: String(row.TRANS_DATE),
        time: String(row.TRANS_TIME),
        type: String(row.TRANS_TYPE),
        securityId: String(row.SECURITY_ID),
        quantity: Number(row.QUANTITY),
        price: Number(row.PRICE),
        amount: Number(row.AMOUNT),
        fees: Number(row.FEES),
      }));

      if (response.transactions.length === 0) {
        response.message = 'No transaction history found';
        return { rc: ReturnCode.Warning, data: response };
      }

      return { rc: ReturnCode.Success, data: response };
    } catch (err) {
      console.error(`History inquiry error: ${err}`);
      response.message = 'Internal error during history inquiry';
      return { rc: ReturnCode.Error, data: response };
    }
  }
}
