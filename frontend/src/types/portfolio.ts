/** PORT-RECORD — src/copybook/common/PORTFLIO.cpy */
export type ClientType = 'I' | 'C' | 'T'
export type PortfolioStatus = 'A' | 'C' | 'S'

export interface Portfolio {
  /** PORT-ID PIC X(8) */
  portfolioId: string
  /** PORT-ACCOUNT-NO PIC X(10) */
  accountNo: string
  /** PORT-CLIENT-NAME PIC X(30) */
  clientName: string
  clientType: ClientType
  /** PORT-CREATE-DATE PIC 9(8), as ISO yyyy-mm-dd */
  createDate: string
  /** PORT-LAST-MAINT PIC 9(8), as ISO yyyy-mm-dd */
  lastMaint: string
  status: PortfolioStatus
  /** PORT-TOTAL-VALUE PIC S9(13)V99 COMP-3 */
  totalValue: number
  /** PORT-CASH-BALANCE PIC S9(13)V99 COMP-3 */
  cashBalance: number
  /** PORT-LAST-USER PIC X(8) */
  lastUser: string
  /** PORT-LAST-TRANS PIC 9(8), date of the last posted transaction as ISO yyyy-mm-dd */
  lastTrans: string
}
