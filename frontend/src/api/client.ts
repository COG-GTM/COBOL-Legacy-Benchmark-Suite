import type { DashboardStats, PortfolioDetail, PortfolioSummary, Transaction } from "../types";

const BASE = "/api";

async function get<T>(path: string): Promise<T> {
  const res = await fetch(`${BASE}${path}`);
  if (!res.ok) {
    const err = await res.json().catch(() => ({ detail: res.statusText }));
    throw new Error(err.detail || res.statusText);
  }
  return res.json();
}

async function post<T>(path: string, body: unknown): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ detail: res.statusText }));
    throw new Error(err.detail || res.statusText);
  }
  return res.json();
}

export const api = {
  getDashboard: () => get<DashboardStats>("/dashboard"),
  getPortfolios: (params?: Record<string, string>) => {
    const qs = params ? "?" + new URLSearchParams(params).toString() : "";
    return get<PortfolioSummary[]>(`/portfolios${qs}`);
  },
  getPortfolio: (id: string) => get<PortfolioDetail>(`/portfolios/${id}`),
  getTransactions: (params?: Record<string, string>) => {
    const qs = params ? "?" + new URLSearchParams(params).toString() : "";
    return get<Transaction[]>(`/transactions${qs}`);
  },
  createPortfolio: (data: Record<string, unknown>) =>
    post<PortfolioSummary>("/portfolios", data),
  createTransaction: (data: Record<string, unknown>) =>
    post<Transaction>("/transactions", data),
};
