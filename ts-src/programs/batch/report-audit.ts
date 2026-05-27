/**
 * Audit Report Generator.
 * Migrated from: src/programs/batch/RPTAUD00.cbl
 *
 * Reads the AUDITLOG table and generates a formatted audit trail report.
 */

import { Knex } from 'knex';
import { ReturnCode } from '../../types';

interface AuditRow {
  TIMESTAMP: string;
  USER_ID: string;
  PROGRAM: string;
  TYPE: string;
  ACTION: string;
  STATUS: string;
  PORTFOLIO_ID: string;
  ACCOUNT_NO: string;
  MESSAGE: string;
}

export class ReportAudit {
  private lines: string[] = [];

  constructor(private readonly db: Knex) {}

  /** Main entry point – mirrors COBOL 0000-MAIN. */
  async run(startDate?: string, endDate?: string): Promise<number> {
    const rc = await this.fetchAuditRecords(startDate, endDate);
    this.printReport();
    return rc;
  }

  /** 1000-FETCH-AUDIT-RECORDS. */
  private async fetchAuditRecords(startDate?: string, endDate?: string): Promise<number> {
    try {
      let query = this.db('AUDITLOG').select('*').orderBy('TIMESTAMP', 'asc');
      if (startDate) query = query.where('TIMESTAMP', '>=', startDate);
      if (endDate) query = query.where('TIMESTAMP', '<=', endDate);

      const rows: AuditRow[] = await query;
      this.formatReport(rows);
      return ReturnCode.Success;
    } catch (err) {
      console.error(`Error querying AUDITLOG: ${err}`);
      return ReturnCode.Error;
    }
  }

  /** 2000-FORMAT-REPORT. */
  private formatReport(rows: AuditRow[]): void {
    this.lines = [];
    this.writeHeader();

    if (rows.length === 0) {
      this.lines.push('  No audit records found for the specified period.');
    }

    for (const row of rows) {
      this.writeDetail(row);
    }

    this.writeFooter(rows.length);
  }

  /** Write report header. */
  private writeHeader(): void {
    this.lines.push('='.repeat(100));
    this.lines.push('AUDIT TRAIL REPORT');
    this.lines.push(`Generated: ${new Date().toISOString()}`);
    this.lines.push('='.repeat(100));
    this.lines.push('');
    this.lines.push(
      'Timestamp                  User     Program  Type Action   Status Portfolio  Account',
    );
    this.lines.push('-'.repeat(100));
  }

  /** Write a single detail line. */
  private writeDetail(row: AuditRow): void {
    this.lines.push(
      `${(row.TIMESTAMP || '').padEnd(26)} ` +
      `${(row.USER_ID || '').padEnd(8)} ` +
      `${(row.PROGRAM || '').padEnd(8)} ` +
      `${(row.TYPE || '').padEnd(4)} ` +
      `${(row.ACTION || '').padEnd(8)} ` +
      `${(row.STATUS || '').padEnd(4)}  ` +
      `${(row.PORTFOLIO_ID || '').padEnd(10)} ` +
      `${(row.ACCOUNT_NO || '').padEnd(10)}`,
    );
    if (row.MESSAGE) {
      this.lines.push(`  Message: ${row.MESSAGE}`);
    }
  }

  /** Write report footer. */
  private writeFooter(count: number): void {
    this.lines.push('-'.repeat(100));
    this.lines.push(`Total audit records: ${count}`);
    this.lines.push('='.repeat(100));
  }

  /** 3000-PRINT-REPORT. */
  private printReport(): void {
    console.log(this.lines.join('\n'));
  }

  /** Get report as a string (for file output). */
  getReportText(): string {
    return this.lines.join('\n');
  }
}
