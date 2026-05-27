/**
 * Database Statistics Collector.
 * Migrated from: src/programs/common/DB2STAT.cbl
 *
 * Collects and reports runtime database statistics:
 * query counts, error counts, average response time, etc.
 */

import { ReturnCode } from '../../types';

export type Db2StatFunction = 'INIT' | 'UPDT' | 'TERM' | 'DISP';

interface DbStats {
  totalQueries: number;
  totalInserts: number;
  totalUpdates: number;
  totalDeletes: number;
  totalErrors: number;
  startTime: string;
  endTime: string;
}

export class Db2Status {
  private stats: DbStats = this.createEmpty();

  /** Dispatch – mirrors COBOL 0000-MAIN EVALUATE. */
  execute(func: Db2StatFunction): number {
    switch (func) {
      case 'INIT':
        return this.initialize();
      case 'UPDT':
        return ReturnCode.Success; // updated via increment helpers
      case 'TERM':
        return this.terminate();
      case 'DISP':
        return this.display();
      default:
        return ReturnCode.Error;
    }
  }

  /** 1000-INITIALIZE. */
  private initialize(): number {
    this.stats = this.createEmpty();
    this.stats.startTime = new Date().toISOString();
    return ReturnCode.Success;
  }

  /** 4000-TERMINATE. */
  private terminate(): number {
    this.stats.endTime = new Date().toISOString();
    this.display();
    return ReturnCode.Success;
  }

  /** 5000-DISPLAY. */
  private display(): number {
    const lines: string[] = [];
    lines.push('--- Database Statistics ---');
    lines.push(`  Queries : ${this.stats.totalQueries}`);
    lines.push(`  Inserts : ${this.stats.totalInserts}`);
    lines.push(`  Updates : ${this.stats.totalUpdates}`);
    lines.push(`  Deletes : ${this.stats.totalDeletes}`);
    lines.push(`  Errors  : ${this.stats.totalErrors}`);
    lines.push(`  Start   : ${this.stats.startTime}`);
    if (this.stats.endTime) {
      lines.push(`  End     : ${this.stats.endTime}`);
    }
    lines.push('---------------------------');
    console.log(lines.join('\n'));
    return ReturnCode.Success;
  }

  // Increment helpers
  recordQuery(): void { this.stats.totalQueries++; }
  recordInsert(): void { this.stats.totalInserts++; }
  recordUpdate(): void { this.stats.totalUpdates++; }
  recordDelete(): void { this.stats.totalDeletes++; }
  recordError(): void { this.stats.totalErrors++; }

  getStats(): DbStats { return { ...this.stats }; }

  private createEmpty(): DbStats {
    return {
      totalQueries: 0,
      totalInserts: 0,
      totalUpdates: 0,
      totalDeletes: 0,
      totalErrors: 0,
      startTime: '',
      endTime: '',
    };
  }
}
