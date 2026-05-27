/**
 * System Statistics Report.
 * Migrated from: src/programs/batch/RPTSTA00.cbl
 *
 * Aggregates and reports system statistics: record counts, error rates,
 * and processing throughput.
 */

import { Knex } from 'knex';
import { ReturnCode } from '../../types';

interface TableStats {
  tableName: string;
  rowCount: number;
}

export class ReportStatus {
  private lines: string[] = [];

  constructor(private readonly db: Knex) {}

  /** Main entry point – mirrors COBOL 0000-MAIN. */
  async run(): Promise<number> {
    const rc = await this.gatherStatistics();
    this.printReport();
    return rc;
  }

  /** 1000-GATHER-STATISTICS. */
  private async gatherStatistics(): Promise<number> {
    try {
      const tables = [
        'PORTFOLIO_MASTER',
        'INVESTMENT_POSITIONS',
        'TRANSACTION_HISTORY',
        'POSHIST',
        'ERRLOG',
        'RTNCODES',
        'AUDITLOG',
      ];

      const stats: TableStats[] = [];
      for (const tableName of tables) {
        try {
          const result = await this.db(tableName).count('* as cnt').first();
          stats.push({ tableName, rowCount: Number(result?.cnt ?? 0) });
        } catch {
          stats.push({ tableName, rowCount: -1 }); // table may not exist
        }
      }

      // Error summary
      let errorSummary: { severity: number; count: number }[] = [];
      try {
        const rawSummary = await this.db('ERRLOG')
          .select('ERROR_SEVERITY as severity')
          .count('* as count')
          .groupBy('ERROR_SEVERITY')
          .orderBy('ERROR_SEVERITY');

        errorSummary = rawSummary.map((r: Record<string, unknown>) => ({
          severity: Number(r.severity),
          count: Number(r.count),
        }));
      } catch {
        // ERRLOG may not exist
      }

      this.formatReport(stats, errorSummary);
      return ReturnCode.Success;
    } catch (err) {
      console.error(`Error gathering statistics: ${err}`);
      return ReturnCode.Error;
    }
  }

  /** 2000-FORMAT-REPORT. */
  private formatReport(
    stats: TableStats[],
    errorSummary: { severity: number; count: number }[],
  ): void {
    this.lines = [];
    this.lines.push('='.repeat(60));
    this.lines.push('SYSTEM STATISTICS REPORT');
    this.lines.push(`Generated: ${new Date().toISOString()}`);
    this.lines.push('='.repeat(60));

    // Table record counts
    this.lines.push('');
    this.lines.push('TABLE RECORD COUNTS');
    this.lines.push('-'.repeat(40));
    for (const s of stats) {
      const count = s.rowCount >= 0 ? String(s.rowCount) : 'N/A';
      this.lines.push(`  ${s.tableName.padEnd(25)} ${count.padStart(10)}`);
    }

    // Error summary
    if (errorSummary.length > 0) {
      this.lines.push('');
      this.lines.push('ERROR SUMMARY BY SEVERITY');
      this.lines.push('-'.repeat(40));
      for (const e of errorSummary) {
        this.lines.push(`  Severity ${e.severity}: ${String(e.count).padStart(10)}`);
      }
    }

    this.lines.push('');
    this.lines.push('='.repeat(60));
  }

  private printReport(): void {
    console.log(this.lines.join('\n'));
  }

  getReportText(): string {
    return this.lines.join('\n');
  }
}
