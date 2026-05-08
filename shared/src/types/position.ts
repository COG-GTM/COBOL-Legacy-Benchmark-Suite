/**
 * Position types derived from COBOL copybook POSREC.cpy (lines 6-23)
 * and DB2 table INVESTMENT_POSITIONS
 */

export type PositionStatus = 'ACTIVE' | 'CLOSED' | 'PENDING';

export interface Position {
  portfolioId: string;     // POS-PORTFOLIO-ID (8 chars)
  date: string;            // POS-DATE (ISO 8601)
  investmentId: string;    // POS-INVESTMENT-ID (10 chars)
  quantity: number;        // POS-QUANTITY (COMP-3 S9(11)V9(4))
  costBasis: number;       // POS-COST-BASIS (COMP-3 S9(13)V9(2))
  marketValue: number;     // POS-MARKET-VALUE (COMP-3 S9(13)V9(2))
  currency: string;        // POS-CURRENCY (3 chars)
  status: PositionStatus;  // POS-STATUS A/C/P
  lastMaintDate: string;   // POS-LAST-MAINT-DATE (ISO 8601)
  lastMaintUser: string;   // POS-LAST-MAINT-USER (8 chars)
}
