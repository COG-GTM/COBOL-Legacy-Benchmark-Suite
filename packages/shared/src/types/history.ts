export interface HistoryRecord {
  accountNo: string;
  portfolioId: string;
  transDate: string;
  transTime: string;
  transType: string;
  securityId: string;
  quantity: number;
  price: number;
  amount: number;
  fees: number;
  totalAmount: number;
  costBasis: number;
  gainLoss: number;
}
