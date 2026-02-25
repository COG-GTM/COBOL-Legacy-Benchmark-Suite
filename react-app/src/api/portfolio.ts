export interface PortfolioPosition {
  accountNo: string;
  fundId: string;
  fundName: string;
  units: number;
  costBasis: number;
  marketValue: number;
}

const mockPortfolios: PortfolioPosition[] = [
  {
    accountNo: "1001234567",
    fundId: "VTSAX",
    fundName: "Vanguard Total Stock Market Index",
    units: 1523.456,
    costBasis: 125340.78,
    marketValue: 152789.34,
  },
  {
    accountNo: "1001234567",
    fundId: "VBTLX",
    fundName: "Vanguard Total Bond Market Index",
    units: 834.221,
    costBasis: 45670.12,
    marketValue: 43210.55,
  },
  {
    accountNo: "1001234567",
    fundId: "VTIAX",
    fundName: "Vanguard Total Intl Stock Index",
    units: 2105.789,
    costBasis: 78450.33,
    marketValue: 82345.67,
  },
];

export async function fetchPortfolio(
  accountNo: string
): Promise<PortfolioPosition[]> {
  // Simulate network delay
  await new Promise((resolve) => setTimeout(resolve, 300));
  return mockPortfolios.filter((p) => p.accountNo === accountNo);
}

export async function fetchAllPortfolios(): Promise<PortfolioPosition[]> {
  await new Promise((resolve) => setTimeout(resolve, 300));
  return mockPortfolios;
}
