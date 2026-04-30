import type { Portfolio, PortfolioDetail, Transaction, PositionReport, Statistics, AuditReport } from '../types';

const BASE = '/api';

async function fetchJson<T>(url: string): Promise<T> {
  const res = await fetch(url);
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error(body.detail?.toString() ?? `Request failed: ${res.status}`);
  }
  return res.json();
}

async function postJson<T>(url: string, data: unknown): Promise<T> {
  const res = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    const detail = body.detail;
    if (typeof detail === 'object' && detail.errors) {
      throw new Error(detail.errors.join('; '));
    }
    throw new Error(typeof detail === 'string' ? detail : `Request failed: ${res.status}`);
  }
  return res.json();
}

export function fetchPortfolios(status?: string) {
  const params = status ? `?status=${status}` : '';
  return fetchJson<{ portfolios: Portfolio[]; total: number }>(`${BASE}/portfolios${params}`);
}

export function fetchPortfolio(id: string) {
  return fetchJson<PortfolioDetail>(`${BASE}/portfolios/${id}`);
}

export function fetchTransactions(portfolioId?: string, type?: string, status?: string) {
  const params = new URLSearchParams();
  if (portfolioId) params.set('portfolio_id', portfolioId);
  if (type) params.set('transaction_type', type);
  if (status) params.set('status', status);
  const qs = params.toString();
  return fetchJson<{ transactions: Transaction[]; total: number }>(`${BASE}/transactions${qs ? '?' + qs : ''}`);
}

export function submitTransaction(data: {
  portfolio_id: string;
  investment_id: string;
  transaction_type: string;
  quantity: number;
  price: number;
  currency?: string;
}) {
  return postJson<Transaction>(`${BASE}/transactions`, data);
}

export function fetchPositionReport() {
  return fetchJson<PositionReport>(`${BASE}/reports/positions`);
}

export function fetchStatistics() {
  return fetchJson<Statistics>(`${BASE}/reports/statistics`);
}

export function fetchAuditReport() {
  return fetchJson<AuditReport>(`${BASE}/reports/audit`);
}
