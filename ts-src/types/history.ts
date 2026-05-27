/**
 * History Record types.
 * Migrated from: src/copybook/common/HISTREC.cpy
 *
 * Provides an immutable change-log for portfolios, positions, and transactions.
 * Key: HIST-DATE + HIST-TIME + HIST-PORTFOLIO-ID + HIST-SEQ-NO.
 */

/** History entry type (level-88). */
export enum HistoryType {
  Portfolio = 'PT',
  Position = 'PS',
  Transaction = 'TR',
}

/** History action (level-88). */
export enum HistoryAction {
  Add = 'A',
  Change = 'C',
  Delete = 'D',
}

/** Composite key for a history record. */
export interface HistoryKey {
  /** PIC X(8) – Date (YYYYMMDD). */
  histDate: string;
  /** PIC X(6) – Time (HHMMSS). */
  histTime: string;
  /** PIC X(8) – Portfolio identifier. */
  histPortfolioId: string;
  /** PIC 9(6) – Sequence number. */
  histSeqNo: number;
}

/** Full history record. */
export interface HistoryRecord {
  histKey: HistoryKey;
  /** PIC X(2) – PT/PS/TR. */
  histType: HistoryType | string;
  /** PIC X(1) – A/C/D. */
  histAction: HistoryAction | string;
  /** PIC X(8) – User who made the change. */
  histUserId: string;
  /** PIC X(8) – Program that made the change. */
  histProgramId: string;
  /** PIC X(400) – Snapshot before the change. */
  histBeforeImage: string;
  /** PIC X(400) – Snapshot after the change. */
  histAfterImage: string;
  /** PIC X(80) – Free-text description. */
  histDescription: string;
}
