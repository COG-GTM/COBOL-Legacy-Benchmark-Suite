import type {
  Portfolio,
  Position,
  Transaction,
} from "@/types/domain";

const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "/api";

// ---------------------------------------------------------------------------
// Paginated response envelope (matches Rust PaginatedResponse<T>)
// ---------------------------------------------------------------------------

export interface PaginatedResponse<T> {
  data: T[];
  total: number;
  limit: number;
  offset: number;
}

// ---------------------------------------------------------------------------
// Query parameters
// ---------------------------------------------------------------------------

export interface ListParams {
  limit?: number;
  offset?: number;
  status?: string;
}

// ---------------------------------------------------------------------------
// Dashboard summary (computed client-side from portfolio list)
// ---------------------------------------------------------------------------

export interface DashboardSummary {
  totalPortfolios: number;
  totalValue: number;
  recentTransactions: Transaction[];
}

// ---------------------------------------------------------------------------
// HTTP helper
// ---------------------------------------------------------------------------

function authHeaders(): Record<string, string> {
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
  };
  if (typeof window !== "undefined") {
    const token = localStorage.getItem("auth_token");
    if (token) {
      headers["Authorization"] = `Bearer ${token}`;
    }
  }
  return headers;
}

function buildQuery(params?: ListParams): string {
  if (!params) return "";
  const parts: string[] = [];
  if (params.limit != null) parts.push(`limit=${params.limit}`);
  if (params.offset != null) parts.push(`offset=${params.offset}`);
  if (params.status) parts.push(`status=${encodeURIComponent(params.status)}`);
  return parts.length ? `?${parts.join("&")}` : "";
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    ...init,
    headers: { ...authHeaders(), ...init?.headers },
  });
  if (!res.ok) {
    throw new Error(`API ${res.status}: ${res.statusText}`);
  }
  if (res.status === 204 || res.headers.get("content-length") === "0") {
    return undefined as T;
  }
  return res.json() as Promise<T>;
}

// ---------------------------------------------------------------------------
// Portfolio endpoints — GET /api/portfolios
// ---------------------------------------------------------------------------

export async function getPortfolios(
  params?: ListParams,
): Promise<PaginatedResponse<Portfolio>> {
  try {
    return await request<PaginatedResponse<Portfolio>>(
      `/portfolios${buildQuery(params)}`,
    );
  } catch {
    return { data: [], total: 0, limit: 20, offset: 0 };
  }
}

export async function getPortfolio(id: string): Promise<Portfolio | null> {
  try {
    return await request<Portfolio>(`/portfolios/${encodeURIComponent(id)}`);
  } catch {
    return null;
  }
}

export async function createPortfolio(
  data: Omit<Portfolio, "id">,
): Promise<Portfolio | null> {
  try {
    return await request<Portfolio>("/portfolios", {
      method: "POST",
      body: JSON.stringify(data),
    });
  } catch {
    return null;
  }
}

export async function updatePortfolio(
  id: string,
  data: Partial<Portfolio>,
): Promise<Portfolio | null> {
  try {
    return await request<Portfolio>(
      `/portfolios/${encodeURIComponent(id)}`,
      { method: "PUT", body: JSON.stringify(data) },
    );
  } catch {
    return null;
  }
}

export async function deletePortfolio(id: string): Promise<boolean> {
  try {
    await request(`/portfolios/${encodeURIComponent(id)}`, {
      method: "DELETE",
    });
    return true;
  } catch {
    return false;
  }
}

// ---------------------------------------------------------------------------
// Position endpoints — GET /api/portfolios/:id/positions
// ---------------------------------------------------------------------------

export async function getPositions(
  portfolioId: string,
  params?: ListParams,
): Promise<PaginatedResponse<Position>> {
  try {
    return await request<PaginatedResponse<Position>>(
      `/portfolios/${encodeURIComponent(portfolioId)}/positions${buildQuery(params)}`,
    );
  } catch {
    return { data: [], total: 0, limit: 20, offset: 0 };
  }
}

// ---------------------------------------------------------------------------
// Transaction endpoints — GET /api/portfolios/:id/transactions
// ---------------------------------------------------------------------------

export async function getTransactions(
  portfolioId: string,
  params?: ListParams,
): Promise<PaginatedResponse<Transaction>> {
  try {
    return await request<PaginatedResponse<Transaction>>(
      `/portfolios/${encodeURIComponent(portfolioId)}/transactions${buildQuery(params)}`,
    );
  } catch {
    return { data: [], total: 0, limit: 20, offset: 0 };
  }
}

// ---------------------------------------------------------------------------
// Dashboard summary — aggregates portfolio + transaction data
// ---------------------------------------------------------------------------

export async function getDashboardSummary(): Promise<DashboardSummary> {
  const portfolioResp = await getPortfolios({ limit: 100 });
  const totalValue = portfolioResp.data.reduce(
    (sum, p) => sum + p.totalValue,
    0,
  );

  let recentTransactions: Transaction[] = [];
  if (portfolioResp.data.length > 0) {
    const firstPortfolio = portfolioResp.data[0];
    const txResp = await getTransactions(firstPortfolio.id, { limit: 5 });
    recentTransactions = txResp.data;
  }

  return {
    totalPortfolios: portfolioResp.total,
    totalValue,
    recentTransactions,
  };
}
