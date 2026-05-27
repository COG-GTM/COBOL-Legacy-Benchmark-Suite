/**
 * Data Validation Utility.
 * Migrated from: src/programs/utility/UTLVAL00.cbl
 *
 * Performs integrity, cross-reference, format, and balance checks
 * across the data stores.
 */

import Decimal from 'decimal.js';
import { Knex } from 'knex';
import { ReturnCode } from '../../types';

export type ValidationFunction = 'INTG' | 'XREF' | 'FRMT' | 'BALC';

interface ValidationResult {
  check: string;
  passed: boolean;
  message: string;
}

export class DataValidation {
  private results: ValidationResult[] = [];

  constructor(private readonly db: Knex) {}

  /** Dispatch – mirrors COBOL 0000-MAIN EVALUATE. */
  async execute(func: ValidationFunction): Promise<number> {
    switch (func) {
      case 'INTG':
        return this.checkIntegrity();
      case 'XREF':
        return this.checkCrossReference();
      case 'FRMT':
        return this.checkFormat();
      case 'BALC':
        return this.checkBalance();
      default:
        return ReturnCode.Error;
    }
  }

  /** Run all validation checks. */
  async runAll(): Promise<number> {
    this.results = [];

    await this.checkIntegrity();
    await this.checkCrossReference();
    await this.checkFormat();
    await this.checkBalance();

    this.printResults();
    const failed = this.results.filter((r) => !r.passed).length;
    return failed > 0 ? ReturnCode.Error : ReturnCode.Success;
  }

  /** 1000-CHECK-INTEGRITY – verify primary keys and non-null constraints. */
  private async checkIntegrity(): Promise<number> {
    try {
      // Check for null portfolio IDs
      const nullIds = await this.db('PORTFOLIO_MASTER')
        .whereNull('PORTFOLIO_ID')
        .orWhere('PORTFOLIO_ID', '')
        .count('* as cnt')
        .first();

      this.results.push({
        check: 'Portfolio ID not null',
        passed: Number(nullIds?.cnt ?? 0) === 0,
        message: `Found ${nullIds?.cnt ?? 0} null/empty portfolio IDs`,
      });

      return ReturnCode.Success;
    } catch (err) {
      this.results.push({
        check: 'Integrity check',
        passed: false,
        message: `Error: ${err}`,
      });
      return ReturnCode.Error;
    }
  }

  /** 2000-CHECK-CROSS-REFERENCE – verify FK relationships. */
  private async checkCrossReference(): Promise<number> {
    try {
      // Positions must reference existing portfolios
      const orphanPositions = await this.db('INVESTMENT_POSITIONS as ip')
        .leftJoin('PORTFOLIO_MASTER as pm', 'ip.PORTFOLIO_ID', 'pm.PORTFOLIO_ID')
        .whereNull('pm.PORTFOLIO_ID')
        .count('* as cnt')
        .first();

      this.results.push({
        check: 'Position-Portfolio cross-reference',
        passed: Number(orphanPositions?.cnt ?? 0) === 0,
        message: `Found ${orphanPositions?.cnt ?? 0} orphaned positions`,
      });

      return ReturnCode.Success;
    } catch (err) {
      this.results.push({
        check: 'Cross-reference check',
        passed: false,
        message: `Error: ${err}`,
      });
      return ReturnCode.Error;
    }
  }

  /** 3000-CHECK-FORMAT – verify data format constraints. */
  private async checkFormat(): Promise<number> {
    try {
      // Check portfolio ID format (should start with PORT)
      const badFormat = await this.db('PORTFOLIO_MASTER')
        .whereNot('PORTFOLIO_ID', 'like', 'PORT%')
        .count('* as cnt')
        .first();

      this.results.push({
        check: 'Portfolio ID format (PORT prefix)',
        passed: Number(badFormat?.cnt ?? 0) === 0,
        message: `Found ${badFormat?.cnt ?? 0} non-conforming portfolio IDs`,
      });

      return ReturnCode.Success;
    } catch (err) {
      this.results.push({
        check: 'Format check',
        passed: false,
        message: `Error: ${err}`,
      });
      return ReturnCode.Error;
    }
  }

  /** 4000-CHECK-BALANCE – verify financial totals. */
  private async checkBalance(): Promise<number> {
    try {
      const portfolios = await this.db('PORTFOLIO_MASTER').select('PORTFOLIO_ID', 'TOTAL_VALUE');

      for (const port of portfolios) {
        const posSum = await this.db('INVESTMENT_POSITIONS')
          .where('PORTFOLIO_ID', port.PORTFOLIO_ID)
          .sum('MARKET_VALUE as total')
          .first();

        const posTotal = new Decimal(posSum?.total ?? 0);
        const portTotal = new Decimal(port.TOTAL_VALUE ?? 0);
        const diff = posTotal.minus(portTotal).abs();

        this.results.push({
          check: `Balance check ${port.PORTFOLIO_ID}`,
          passed: diff.lte(0.01),
          message: `Portfolio=${portTotal.toFixed(2)}, Positions=${posTotal.toFixed(2)}, Diff=${diff.toFixed(2)}`,
        });
      }

      return ReturnCode.Success;
    } catch (err) {
      this.results.push({
        check: 'Balance check',
        passed: false,
        message: `Error: ${err}`,
      });
      return ReturnCode.Error;
    }
  }

  private printResults(): void {
    console.log('');
    console.log('='.repeat(70));
    console.log('DATA VALIDATION RESULTS');
    console.log('='.repeat(70));

    for (const r of this.results) {
      const status = r.passed ? 'PASS' : 'FAIL';
      console.log(`  [${status}] ${r.check} – ${r.message}`);
    }

    const passed = this.results.filter((r) => r.passed).length;
    const failed = this.results.filter((r) => !r.passed).length;
    console.log('-'.repeat(70));
    console.log(`Total: ${this.results.length}  Passed: ${passed}  Failed: ${failed}`);
    console.log('='.repeat(70));
  }

  getResults(): ValidationResult[] {
    return [...this.results];
  }
}
