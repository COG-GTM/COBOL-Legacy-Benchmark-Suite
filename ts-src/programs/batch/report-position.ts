/**
 * Daily Position Report.
 * Migrated from: src/programs/batch/RPTPOS00.cbl
 *
 * Reads the INVESTMENT_POSITIONS table and generates a formatted
 * daily position report grouped by portfolio.
 */

import { Knex } from 'knex';
import Decimal from 'decimal.js';
import { ReturnCode } from '../../types';

interface PositionRow {
  PORTFOLIO_ID: string;
  INVESTMENT_ID: string;
  POSITION_DATE: string;
  INVESTMENT_TYPE: string;
  STATUS: string;
  QUANTITY: number;
  COST_BASIS: number;
  MARKET_VALUE: number;
  PERCENT_CHANGE: number;
  DESCRIPTION: string;
}

export class ReportPosition {
  private lines: string[] = [];

  constructor(private readonly db: Knex) {}

  /** Main entry point – mirrors COBOL 0000-MAIN. */
  async run(positionDate?: string): Promise<number> {
    const rc = await this.fetchPositions(positionDate);
    this.printReport();
    return rc;
  }

  /** 1000-FETCH-POSITIONS. */
  private async fetchPositions(positionDate?: string): Promise<number> {
    try {
      let query = this.db('INVESTMENT_POSITIONS')
        .select('*')
        .orderBy(['PORTFOLIO_ID', 'INVESTMENT_ID']);

      if (positionDate) {
        query = query.where('POSITION_DATE', positionDate);
      }

      const rows: PositionRow[] = await query;
      this.formatReport(rows);
      return ReturnCode.Success;
    } catch (err) {
      console.error(`Error querying INVESTMENT_POSITIONS: ${err}`);
      return ReturnCode.Error;
    }
  }

  /** 2000-FORMAT-REPORT. */
  private formatReport(rows: PositionRow[]): void {
    this.lines = [];
    this.writeHeader();

    let currentPortfolio = '';
    let portfolioTotal = new Decimal(0);
    let grandTotal = new Decimal(0);

    for (const row of rows) {
      if (row.PORTFOLIO_ID !== currentPortfolio) {
        if (currentPortfolio) {
          this.writePortfolioTotal(currentPortfolio, portfolioTotal);
          portfolioTotal = new Decimal(0);
        }
        currentPortfolio = row.PORTFOLIO_ID;
        this.writePortfolioHeader(currentPortfolio);
      }

      this.writeDetail(row);
      const mv = new Decimal(row.MARKET_VALUE || 0);
      portfolioTotal = portfolioTotal.plus(mv);
      grandTotal = grandTotal.plus(mv);
    }

    if (currentPortfolio) {
      this.writePortfolioTotal(currentPortfolio, portfolioTotal);
    }

    this.writeFooter(rows.length, grandTotal);
  }

  private writeHeader(): void {
    this.lines.push('='.repeat(110));
    this.lines.push('DAILY POSITION REPORT');
    this.lines.push(`Generated: ${new Date().toISOString()}`);
    this.lines.push('='.repeat(110));
  }

  private writePortfolioHeader(portfolioId: string): void {
    this.lines.push('');
    this.lines.push(`Portfolio: ${portfolioId}`);
    this.lines.push(
      'Invest ID  Type  Status  Description                    Quantity       Cost Basis     Market Value  % Change',
    );
    this.lines.push('-'.repeat(110));
  }

  private writeDetail(row: PositionRow): void {
    const qty = new Decimal(row.QUANTITY || 0);
    const cost = new Decimal(row.COST_BASIS || 0);
    const mv = new Decimal(row.MARKET_VALUE || 0);
    const pct = new Decimal(row.PERCENT_CHANGE || 0);

    this.lines.push(
      `${(row.INVESTMENT_ID || '').padEnd(10)} ` +
      `${(row.INVESTMENT_TYPE || '').padEnd(5)} ` +
      `${(row.STATUS || '').padEnd(7)} ` +
      `${(row.DESCRIPTION || '').padEnd(30)} ` +
      `${qty.toFixed(3).padStart(12)} ` +
      `${cost.toFixed(2).padStart(14)} ` +
      `${mv.toFixed(2).padStart(14)} ` +
      `${pct.toFixed(2).padStart(8)}`,
    );
  }

  private writePortfolioTotal(portfolioId: string, total: Decimal): void {
    this.lines.push('-'.repeat(110));
    this.lines.push(`  Total for ${portfolioId}: ${total.toFixed(2).padStart(14)}`);
  }

  private writeFooter(count: number, grandTotal: Decimal): void {
    this.lines.push('');
    this.lines.push('='.repeat(110));
    this.lines.push(`Total positions: ${count}    Grand total market value: ${grandTotal.toFixed(2)}`);
    this.lines.push('='.repeat(110));
  }

  private printReport(): void {
    console.log(this.lines.join('\n'));
  }

  getReportText(): string {
    return this.lines.join('\n');
  }
}
