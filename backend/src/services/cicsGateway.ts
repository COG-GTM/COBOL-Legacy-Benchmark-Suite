export interface PortfolioPosition {
  accountNo: string;
  fundId: string;
  fundName: string;
  units: number;
  costBasis: number;
  marketValue: number;
}

export interface HistoryEntry {
  date: string;
  type: string;
  units: number;
  price: number;
  amount: number;
}

export interface HistoryResponse {
  entries: HistoryEntry[];
  currentPage: number;
  totalPages: number;
  hasMore: boolean;
}

export interface AuthResult {
  success: boolean;
  userId: string;
  token: string;
  errorMessage?: string;
}

const MOCK_PORTFOLIOS: Record<string, PortfolioPosition> = {
  "1000000001": {
    accountNo: "1000000001",
    fundId: "FND001",
    fundName: "Growth Equity Fund",
    units: 1500.50,
    costBasis: 45250.75,
    marketValue: 52875.30,
  },
  "1000000002": {
    accountNo: "1000000002",
    fundId: "FND002",
    fundName: "Fixed Income Fund",
    units: 3200.00,
    costBasis: 96000.00,
    marketValue: 98400.00,
  },
  "1000000003": {
    accountNo: "1000000003",
    fundId: "FND003",
    fundName: "Balanced Portfolio Fund",
    units: 750.25,
    costBasis: 22507.50,
    marketValue: 24382.13,
  },
};

const MOCK_HISTORY: Record<string, HistoryEntry[]> = {
  "1000000001": [
    { date: "2026-02-15", type: "BUY", units: 100.00, price: 35.25, amount: 3525.00 },
    { date: "2026-02-10", type: "BUY", units: 200.00, price: 34.80, amount: 6960.00 },
    { date: "2026-01-28", type: "SELL", units: 50.00, price: 35.50, amount: 1775.00 },
    { date: "2026-01-20", type: "BUY", units: 300.00, price: 33.90, amount: 10170.00 },
    { date: "2026-01-15", type: "DIV", units: 0.00, price: 0.00, amount: 450.25 },
    { date: "2026-01-10", type: "BUY", units: 150.00, price: 34.10, amount: 5115.00 },
    { date: "2025-12-28", type: "SELL", units: 75.00, price: 34.75, amount: 2606.25 },
    { date: "2025-12-20", type: "BUY", units: 250.00, price: 33.50, amount: 8375.00 },
    { date: "2025-12-15", type: "DIV", units: 0.00, price: 0.00, amount: 380.50 },
    { date: "2025-12-10", type: "BUY", units: 100.00, price: 33.20, amount: 3320.00 },
    { date: "2025-12-01", type: "SELL", units: 80.00, price: 33.80, amount: 2704.00 },
    { date: "2025-11-25", type: "BUY", units: 175.00, price: 32.90, amount: 5757.50 },
    { date: "2025-11-15", type: "BUY", units: 200.00, price: 32.50, amount: 6500.00 },
    { date: "2025-11-10", type: "DIV", units: 0.00, price: 0.00, amount: 320.75 },
    { date: "2025-11-01", type: "BUY", units: 125.50, price: 32.00, amount: 4016.00 },
  ],
  "1000000002": [
    { date: "2026-02-12", type: "BUY", units: 500.00, price: 30.75, amount: 15375.00 },
    { date: "2026-02-01", type: "DIV", units: 0.00, price: 0.00, amount: 960.00 },
    { date: "2026-01-15", type: "BUY", units: 700.00, price: 30.00, amount: 21000.00 },
    { date: "2025-12-20", type: "BUY", units: 1000.00, price: 29.80, amount: 29800.00 },
    { date: "2025-12-01", type: "DIV", units: 0.00, price: 0.00, amount: 890.00 },
    { date: "2025-11-15", type: "BUY", units: 1000.00, price: 29.50, amount: 29500.00 },
  ],
  "1000000003": [
    { date: "2026-02-14", type: "BUY", units: 50.25, price: 32.50, amount: 1633.13 },
    { date: "2026-01-30", type: "BUY", units: 100.00, price: 31.80, amount: 3180.00 },
    { date: "2026-01-15", type: "SELL", units: 25.00, price: 32.20, amount: 805.00 },
    { date: "2025-12-28", type: "BUY", units: 200.00, price: 31.00, amount: 6200.00 },
    { date: "2025-12-15", type: "DIV", units: 0.00, price: 0.00, amount: 225.38 },
    { date: "2025-12-01", type: "BUY", units: 150.00, price: 30.50, amount: 4575.00 },
    { date: "2025-11-20", type: "BUY", units: 275.00, price: 29.90, amount: 8222.50 },
  ],
};

const MOCK_USERS: Record<string, string> = {
  admin: "admin123",
  operator: "oper456",
  viewer: "view789",
};

const ROWS_PER_PAGE = 10;

export class CICSGateway {
  async getPortfolioPosition(accountNo: string): Promise<PortfolioPosition | null> {
    const position = MOCK_PORTFOLIOS[accountNo];
    return position ?? null;
  }

  async getTransactionHistory(accountNo: string, page: number): Promise<HistoryResponse> {
    const allEntries = MOCK_HISTORY[accountNo] ?? [];
    const totalPages = Math.max(1, Math.ceil(allEntries.length / ROWS_PER_PAGE));
    const safePage = Math.max(1, Math.min(page, totalPages));
    const startIndex = (safePage - 1) * ROWS_PER_PAGE;
    const entries = allEntries.slice(startIndex, startIndex + ROWS_PER_PAGE);

    return {
      entries,
      currentPage: safePage,
      totalPages,
      hasMore: safePage < totalPages,
    };
  }

  async authenticate(userId: string, password: string): Promise<AuthResult> {
    const storedPassword = MOCK_USERS[userId];
    if (storedPassword && storedPassword === password) {
      return {
        success: true,
        userId,
        token: `mock-jwt-${userId}-${Date.now()}`,
      };
    }
    return {
      success: false,
      userId,
      token: "",
      errorMessage: "Invalid credentials",
    };
  }
}
