/**
 * Return Analysis Report.
 * Migrated from: src/programs/batch/RTNANA00.cbl
 *
 * Queries the RTNCODES table with a cursor and produces a summary
 * report of return-code distributions by program and severity.
 */

import { Knex } from 'knex';
import { ReturnCode } from '../../types';

interface ReturnCodeRow {
  TIMESTAMP: string;
  PROGRAM_ID: string;
  RETURN_CODE: number;
  HIGHEST_CODE: number;
  STATUS_CODE: string;
  MESSAGE_TEXT: string;
}

interface ProgramSummary {
  programId: string;
  totalCalls: number;
  successCount: number;
  warningCount: number;
  errorCount: number;
  severeCount: number;
  highestCode: number;
}

export class ReturnAnalysis {
  private summaries: Map<string, ProgramSummary> = new Map();

  constructor(private readonly db: Knex) {}

  /** Main entry point – mirrors COBOL 0000-MAIN. */
  async run(): Promise<number> {
    const rc = await this.fetchAndAnalyze();
    this.generateReport();
    return rc;
  }

  /** 1000-FETCH-RETURN-CODES – open cursor and iterate. */
  private async fetchAndAnalyze(): Promise<number> {
    let rows: ReturnCodeRow[];
    try {
      rows = await this.db('RTNCODES')
        .select('*')
        .orderBy(['PROGRAM_ID', 'TIMESTAMP']);
    } catch (err) {
      console.error(`Error querying RTNCODES: ${err}`);
      return ReturnCode.Error;
    }

    for (const row of rows) {
      this.accumulate(row);
    }

    return ReturnCode.Success;
  }

  /** 2000-ACCUMULATE-STATS. */
  private accumulate(row: ReturnCodeRow): void {
    let summary = this.summaries.get(row.PROGRAM_ID);
    if (!summary) {
      summary = {
        programId: row.PROGRAM_ID,
        totalCalls: 0,
        successCount: 0,
        warningCount: 0,
        errorCount: 0,
        severeCount: 0,
        highestCode: 0,
      };
      this.summaries.set(row.PROGRAM_ID, summary);
    }

    summary.totalCalls++;
    if (row.RETURN_CODE === 0) summary.successCount++;
    else if (row.RETURN_CODE <= 4) summary.warningCount++;
    else if (row.RETURN_CODE <= 8) summary.errorCount++;
    else summary.severeCount++;

    if (row.HIGHEST_CODE > summary.highestCode) {
      summary.highestCode = row.HIGHEST_CODE;
    }
  }

  /** 3000-GENERATE-REPORT. */
  private generateReport(): void {
    const lines: string[] = [];
    lines.push('');
    lines.push('='.repeat(80));
    lines.push('RETURN CODE ANALYSIS REPORT');
    lines.push('='.repeat(80));
    lines.push('');
    lines.push(
      'Program     Total  Success  Warning  Error  Severe  Highest',
    );
    lines.push('-'.repeat(70));

    for (const [, s] of this.summaries) {
      lines.push(
        `${s.programId.padEnd(12)}${String(s.totalCalls).padStart(5)}  ` +
        `${String(s.successCount).padStart(7)}  ${String(s.warningCount).padStart(7)}  ` +
        `${String(s.errorCount).padStart(5)}  ${String(s.severeCount).padStart(6)}  ` +
        `${String(s.highestCode).padStart(7)}`,
      );
    }

    lines.push('-'.repeat(70));
    lines.push(`Total programs analysed: ${this.summaries.size}`);
    lines.push('='.repeat(80));

    console.log(lines.join('\n'));
  }

  /** Get the summaries for programmatic use. */
  getSummaries(): ProgramSummary[] {
    return Array.from(this.summaries.values());
  }
}
