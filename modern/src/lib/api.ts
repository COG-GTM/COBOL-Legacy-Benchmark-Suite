import type {
  Portfolio,
  PortfolioListResponse,
  PortfolioDetail,
  Position,
  TransactionListResponse,
  TransactionFilters,
  CreatePortfolioInput,
  SubmitTransactionInput,
  BatchRun,
  ReportStats,
  PositionReport,
  AuditReport,
} from "@/types";

const BASE = "/api";

async function fetcher<T>(url: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${url}`, {
    headers: { "Content-Type": "application/json" },
    ...options,
  });
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error(body.error ?? `Request failed: ${res.status}`);
  }
  return res.json();
}

export async function getPortfolios(
  search?: string,
  status?: string
): Promise<PortfolioListResponse> {
  const params = new URLSearchParams();
  if (search) params.set("search", search);
  if (status) params.set("status", status);
  const qs = params.toString();
  return fetcher(`/portfolios${qs ? `?${qs}` : ""}`);
}

export async function getPortfolio(id: string): Promise<PortfolioDetail> {
  return fetcher(`/portfolios/${id}`);
}

export async function createPortfolio(
  data: CreatePortfolioInput
): Promise<Portfolio> {
  return fetcher("/portfolios", {
    method: "POST",
    body: JSON.stringify(data),
  });
}

export async function updatePortfolio(
  id: string,
  data: Partial<CreatePortfolioInput>
): Promise<Portfolio> {
  return fetcher(`/portfolios/${id}`, {
    method: "PATCH",
    body: JSON.stringify(data),
  });
}

export async function deletePortfolio(id: string): Promise<void> {
  await fetcher(`/portfolios/${id}`, { method: "DELETE" });
}

export async function getPositions(portfolioId: string): Promise<Position[]> {
  return fetcher(`/portfolios/${portfolioId}/positions`);
}

export async function getTransactionHistory(
  portfolioId?: string,
  filters?: TransactionFilters
): Promise<TransactionListResponse> {
  const params = new URLSearchParams();
  if (portfolioId) params.set("portfolioId", portfolioId);
  if (filters?.transactionType) params.set("type", filters.transactionType);
  if (filters?.startDate) params.set("startDate", filters.startDate);
  if (filters?.endDate) params.set("endDate", filters.endDate);
  const qs = params.toString();
  return fetcher(`/transactions${qs ? `?${qs}` : ""}`);
}

export async function submitTransaction(
  data: SubmitTransactionInput
): Promise<{ success: boolean; message: string; transactionId?: string }> {
  return fetcher("/transactions", {
    method: "POST",
    body: JSON.stringify(data),
  });
}

export async function triggerBatch(): Promise<BatchRun> {
  return fetcher("/batch", { method: "POST" });
}

export async function getBatchRuns(): Promise<BatchRun[]> {
  return fetcher("/batch");
}

export async function getReports(
  type: "statistics" | "positions" | "audit",
  params?: Record<string, string>
): Promise<ReportStats | PositionReport | AuditReport> {
  const qs = params ? `?${new URLSearchParams(params).toString()}` : "";
  return fetcher(`/reports?type=${type}${qs ? `&${qs.slice(1)}` : ""}`);
}

export const swrFetcher = (url: string) =>
  fetch(url).then((r) => {
    if (!r.ok) throw new Error(`Request failed: ${r.status}`);
    return r.json();
  });
