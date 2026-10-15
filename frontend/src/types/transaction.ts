/** TRANSACTION-RECORD — src/copybook/common/TRNREC.cpy */
export type TransactionType = 'BU' | 'SL' | 'TR' | 'FE'
export type TransactionStatus = 'P' | 'D' | 'F' | 'R'

export interface Transaction {
  /** TRN-DATE PIC X(08), as ISO yyyy-mm-dd */
  date: string
  /** TRN-TIME PIC X(06), as hh:mm:ss */
  time: string
  /** TRN-PORTFOLIO-ID PIC X(08) */
  portfolioId: string
  /** TRN-SEQUENCE-NO PIC X(06) */
  sequenceNo: string
  /** TRN-INVESTMENT-ID PIC X(10) */
  investmentId: string
  type: TransactionType
  /** TRN-QUANTITY PIC S9(11)V9(4) COMP-3 */
  quantity: number
  /** TRN-PRICE PIC S9(11)V9(4) COMP-3 */
  price: number
  /** TRN-AMOUNT PIC S9(13)V9(2) COMP-3 */
  amount: number
  /** TRN-CURRENCY PIC X(03) */
  currency: string
  status: TransactionStatus
}
