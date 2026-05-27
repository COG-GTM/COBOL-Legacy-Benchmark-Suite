/**
 * Portfolio Inquiry Handler.
 * Migrated from: src/programs/online/INQPORT.cbl
 *
 * Reads portfolio data from the VSAM position store and returns
 * position details for a given account.
 */

import { Knex } from 'knex';
import Decimal from 'decimal.js';
import { PositionInquiryResponse, PositionLineItem } from './api-schemas';
import { ReturnCode } from '../../types';

export class InquiryPortfolio {
  constructor(private readonly db: Knex) {}

  /** Handle a portfolio inquiry – mirrors COBOL INQPORT PROCEDURE DIVISION. */
  async inquire(accountNo: string): Promise<{ rc: number; data: PositionInquiryResponse }> {
    const response: PositionInquiryResponse = {
      accountNo,
      portfolioId: '',
      clientName: '',
      status: '',
      totalValue: 0,
      cashBalance: 0,
      positions: [],
    };

    try {
      // 1000-READ-PORTFOLIO – fetch master record
      const master = await this.db('PORTFOLIO_MASTER')
        .where('ACCOUNT_NO', accountNo)
        .first();

      if (!master) {
        response.message = 'Portfolio not found';
        return { rc: ReturnCode.Warning, data: response };
      }

      response.portfolioId = String(master.PORTFOLIO_ID);
      response.clientName = String(master.CLIENT_NAME);
      response.status = String(master.STATUS);
      response.totalValue = Number(master.TOTAL_VALUE);
      response.cashBalance = Number(master.CASH_BALANCE);

      // 2000-READ-POSITIONS – fetch investment positions
      const positions = await this.db('INVESTMENT_POSITIONS')
        .where('PORTFOLIO_ID', master.PORTFOLIO_ID)
        .orderBy('INVESTMENT_ID');

      response.positions = positions.map((row): PositionLineItem => ({
        investmentId: String(row.INVESTMENT_ID),
        description: String(row.DESCRIPTION),
        investmentType: String(row.INVESTMENT_TYPE),
        quantity: Number(row.QUANTITY),
        costBasis: Number(row.COST_BASIS),
        marketValue: Number(row.MARKET_VALUE),
        percentChange: Number(row.PERCENT_CHANGE),
      }));

      // 3000-CALCULATE-TOTALS
      const totalMv = response.positions.reduce(
        (sum, p) => sum.plus(new Decimal(p.marketValue)),
        new Decimal(0),
      );
      response.totalValue = totalMv.toNumber();

      return { rc: ReturnCode.Success, data: response };
    } catch (err) {
      console.error(`Portfolio inquiry error: ${err}`);
      response.message = 'Internal error during inquiry';
      return { rc: ReturnCode.Error, data: response };
    }
  }
}
