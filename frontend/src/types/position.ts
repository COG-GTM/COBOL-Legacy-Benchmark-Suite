/** POSITION-RECORD — src/copybook/common/POSREC.cpy */
export type PositionStatus = 'A' | 'C' | 'P'

export interface Position {
  /** POS-PORTFOLIO-ID PIC X(08) */
  portfolioId: string
  /** POS-DATE PIC X(08), as ISO yyyy-mm-dd */
  date: string
  /** POS-INVESTMENT-ID PIC X(10) */
  investmentId: string
  /** POS-QUANTITY PIC S9(11)V9(4) COMP-3 */
  quantity: number
  /** POS-COST-BASIS PIC S9(13)V9(2) COMP-3 */
  costBasis: number
  /** POS-MARKET-VALUE PIC S9(13)V9(2) COMP-3 */
  marketValue: number
  /** POS-CURRENCY PIC X(03) */
  currency: string
  status: PositionStatus
  /** POS-LAST-MAINT-DATE PIC X(26), DB2 timestamp yyyy-mm-dd-hh.mm.ss.nnnnnn */
  lastMaintDate: string
  /** POS-LAST-MAINT-USER PIC X(08) */
  lastMaintUser: string
}
