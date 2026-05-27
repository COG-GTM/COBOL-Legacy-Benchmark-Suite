/**
 * Cursor Manager.
 * Migrated from: src/programs/online/CURSMGR.cbl
 *
 * Replaces DB2 DECLARE/OPEN/FETCH/CLOSE cursor operations with
 * a query-result-set manager using async iteration and pagination.
 */

import { Knex } from 'knex';
import { ReturnCode } from '../../types';

/** Represents an open cursor (query result set). */
interface CursorState {
  name: string;
  rows: Record<string, unknown>[];
  index: number;
  isOpen: boolean;
}

export class CursorManager {
  private cursors: Map<string, CursorState> = new Map();

  constructor(private readonly db: Knex) {}

  /** DECLARE – register a cursor name and its SQL statement. */
  declare(name: string): number {
    if (this.cursors.has(name)) {
      return ReturnCode.Warning; // already declared
    }
    this.cursors.set(name, { name, rows: [], index: 0, isOpen: false });
    return ReturnCode.Success;
  }

  /** OPEN – execute the query and populate the result set. */
  async open(name: string, query: Knex.QueryBuilder): Promise<number> {
    const cursor = this.cursors.get(name);
    if (!cursor) {
      console.error(`Cursor ${name} not declared`);
      return ReturnCode.Error;
    }

    try {
      cursor.rows = await query;
      cursor.index = 0;
      cursor.isOpen = true;
      return ReturnCode.Success;
    } catch (err) {
      console.error(`Error opening cursor ${name}: ${err}`);
      return ReturnCode.Error;
    }
  }

  /** FETCH – return the next row, or undefined at EOF. */
  fetch(name: string): { rc: number; row?: Record<string, unknown> } {
    const cursor = this.cursors.get(name);
    if (!cursor || !cursor.isOpen) {
      return { rc: ReturnCode.Error };
    }

    if (cursor.index >= cursor.rows.length) {
      return { rc: ReturnCode.Warning }; // EOF (SQLCODE +100)
    }

    const row = cursor.rows[cursor.index++];
    return { rc: ReturnCode.Success, row };
  }

  /** CLOSE – release the result set. */
  close(name: string): number {
    const cursor = this.cursors.get(name);
    if (!cursor) {
      return ReturnCode.Warning;
    }

    cursor.rows = [];
    cursor.index = 0;
    cursor.isOpen = false;
    return ReturnCode.Success;
  }

  /** Close all open cursors. */
  closeAll(): void {
    for (const [name] of this.cursors) {
      this.close(name);
    }
  }
}
