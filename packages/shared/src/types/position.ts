export interface Position {
  portfolioId: string;
  investmentId: string;
  positionDate: string;
  quantity: number;
  costBasis: number;
  marketValue: number;
  currencyCode: string;
}
