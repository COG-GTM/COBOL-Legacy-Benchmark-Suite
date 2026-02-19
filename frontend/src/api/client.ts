const API_BASE = import.meta.env.VITE_API_URL || "http://localhost:3001";

interface ApiResponse<T> {
  data: T;
}

interface ApiError {
  error: string;
}

interface PortfolioPosition {
  accountNo: string;
  fundId: string;
  fundName: string;
  units: number;
  costBasis: number;
  marketValue: number;
}

interface HistoryEntry {
  date: string;
  type: string;
  units: number;
  price: number;
  amount: number;
}

interface HistoryResponse {
  entries: HistoryEntry[];
  currentPage: number;
  totalPages: number;
  hasMore: boolean;
}

interface LoginResponse {
  userId: string;
  token: string;
}

async function request<T>(url: string, options?: RequestInit): Promise<ApiResponse<T>> {
  const response = await fetch(`${API_BASE}${url}`, {
    headers: { "Content-Type": "application/json" },
    ...options,
  });

  if (!response.ok) {
    const errorBody: ApiError = await response.json().catch(() => ({
      error: "Request failed",
    }));
    throw new Error(errorBody.error);
  }

  return response.json();
}

export const apiClient = {
  getPortfolio(accountNo: string): Promise<ApiResponse<PortfolioPosition>> {
    return request<PortfolioPosition>(`/api/portfolios/${encodeURIComponent(accountNo)}`);
  },

  getHistory(accountNo: string, page: number): Promise<ApiResponse<HistoryResponse>> {
    return request<HistoryResponse>(
      `/api/history?portfolio=${encodeURIComponent(accountNo)}&page=${page}`
    );
  },

  login(userId: string, password: string): Promise<ApiResponse<LoginResponse>> {
    return request<LoginResponse>("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({ userId, password }),
    });
  },
};

export type { PortfolioPosition, HistoryEntry, HistoryResponse, LoginResponse };
