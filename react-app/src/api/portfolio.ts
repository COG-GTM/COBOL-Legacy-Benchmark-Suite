/**
 * Portfolio API — mock data layer
 * Replaces DB2/VSAM backend calls from INQPORT COBOL program.
 * Returns Promise-wrapped mock data so it can be swapped for real API calls later.
 */

export interface PortfolioPosition {
  accountNumber: string;
  fundId: string;
  fundName: string;
  units: number;
  costBasis: number;
  marketValue: number;
}

const MOCK_POSITIONS: PortfolioPosition[] = [
  {
    accountNumber: "1001234567",
    fundId: "VFIAX",
    fundName: "Vanguard 500 Index Fund",
    units: 152.347,
    costBasis: 45250.0,
    marketValue: 52813.45,
  },
  {
    accountNumber: "1001234567",
    fundId: "VBTLX",
    fundName: "Vanguard Total Bond Market",
    units: 310.892,
    costBasis: 32100.0,
    marketValue: 31450.12,
  },
  {
    accountNumber: "1001234567",
    fundId: "VTIAX",
    fundName: "Vanguard Total Intl Stock",
    units: 245.51,
    costBasis: 28750.0,
    marketValue: 30125.78,
  },
];

export function fetchPortfolioPositions(
  accountNumber: string
): Promise<PortfolioPosition[]> {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve(
        MOCK_POSITIONS.filter((p) => p.accountNumber === accountNumber)
      );
    }, 300);
  });
}

export function fetchDefaultPortfolio(): Promise<PortfolioPosition[]> {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve(MOCK_POSITIONS);
    }, 300);
  });
}
