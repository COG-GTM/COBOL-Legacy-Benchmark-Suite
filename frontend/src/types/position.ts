/** Maps to POSREC.cpy - POSITION-RECORD */
export interface Position {
  portfolioId: string;       // POS-PORTFOLIO-ID PIC X(08)
  date: string;              // POS-DATE PIC X(08) YYYYMMDD
  investmentId: string;      // POS-INVESTMENT-ID PIC X(10)
  quantity: number;          // POS-QUANTITY PIC S9(11)V9(4)
  costBasis: number;         // POS-COST-BASIS PIC S9(13)V9(2)
  marketValue: number;       // POS-MARKET-VALUE PIC S9(13)V9(2)
  currency: string;          // POS-CURRENCY PIC X(03)
  status: PositionStatus;    // POS-STATUS PIC X(01)
  lastMaintDate: string;     // POS-LAST-MAINT-DATE PIC X(26)
  lastMaintUser: string;     // POS-LAST-MAINT-USER PIC X(08)
  fundName: string;          // derived for display (POSMAP NAMEOUT)
}

export type PositionStatus = 'A' | 'C' | 'P';
export const POSITION_STATUS_LABELS: Record<PositionStatus, string> = {
  A: 'Active',
  C: 'Closed',
  P: 'Pending',
};
