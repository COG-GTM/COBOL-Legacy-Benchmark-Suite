import type {
  Portfolio,
  Position,
  Transaction,
} from "@/types/domain";

const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "/api";

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    headers: { "Content-Type": "application/json", ...init?.headers },
    ...init,
  });
  if (!res.ok) {
    throw new Error(`API ${res.status}: ${res.statusText}`);
  }
  return res.json() as Promise<T>;
}

// ---------------------------------------------------------------------------
// Portfolio endpoints (stubs — return mock data until Wave 3 API)
// ---------------------------------------------------------------------------

export async function getPortfolios(): Promise<Portfolio[]> {
  try {
    return await request<Portfolio[]>("/portfolios");
  } catch {
    return [];
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
// Position endpoints
// ---------------------------------------------------------------------------

export async function getPositions(portfolioId: string): Promise<Position[]> {
  try {
    return await request<Position[]>(
      `/portfolios/${encodeURIComponent(portfolioId)}/positions`,
    );
  } catch {
    return [];
  }
}

// ---------------------------------------------------------------------------
// Transaction endpoints
// ---------------------------------------------------------------------------

export async function getTransactions(
  portfolioId: string,
): Promise<Transaction[]> {
  try {
    return await request<Transaction[]>(
      `/portfolios/${encodeURIComponent(portfolioId)}/transactions`,
    );
  } catch {
    return [];
  }
}
