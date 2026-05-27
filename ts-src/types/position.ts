/**
 * Position Record types.
 * Migrated from: src/copybook/common/POSREC.cpy
 *
 * Tracks investment positions within a portfolio at a point in time.
 * Key: POS-PORTFOLIO-ID + POS-DATE + POS-INVESTMENT-ID.
 */

/** Position status codes (level-88). */
export enum PositionStatus {
  Active = 'A',
  Closed = 'C',
  Pending = 'P',
}

/** Composite key for a position record. */
export interface PositionKey {
  /** PIC X(8) – Owning portfolio. */
  posPortfolioId: string;
  /** PIC X(8) – Position date (YYYYMMDD). */
  posDate: string;
  /** PIC X(8) – Investment / security ID. */
  posInvestmentId: string;
}

/** Full position record. */
export interface PositionRecord {
  posKey: PositionKey;
  /** PIC X(1) – A/C/P. */
  posStatus: PositionStatus | string;
  /** PIC X(10) – Account number. */
  posAccountNo: string;
  /** PIC X(30) – Investment description. */
  posDescription: string;
  /** PIC X(3) – Investment type (STK/BND/MMF/ETF). */
  posInvestmentType: string;
  /** PIC S9(11)V999 COMP-3 – Quantity held. */
  posQuantity: number;
  /** PIC S9(13)V99 COMP-3 – Original cost basis. */
  posCostBasis: number;
  /** PIC S9(13)V99 COMP-3 – Current market value. */
  posMarketValue: number;
  /** PIC S9(5)V99 COMP-3 – Percentage change. */
  posPercentChange: number;
  /** PIC X(8) – Last valuation date (YYYYMMDD). */
  posLastValDate: string;
}
