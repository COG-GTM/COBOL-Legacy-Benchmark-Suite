const API_BASE = '/api';

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = localStorage.getItem('token');
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(token && { Authorization: `Bearer ${token}` }),
    ...(options.headers as Record<string, string>),
  };

  const res = await fetch(`${API_BASE}${path}`, { ...options, headers });
  const data = await res.json();

  if (!res.ok) {
    throw new Error(data.error?.message || 'Request failed');
  }
  return data;
}

export const api = {
  // Auth
  login: (body: { username: string; password: string }) =>
    request('/auth/login', { method: 'POST', body: JSON.stringify(body) }),
  register: (body: { username: string; email: string; password: string }) =>
    request('/auth/register', { method: 'POST', body: JSON.stringify(body) }),
  getMe: () => request('/auth/me'),

  // Portfolios
  getPortfolios: (params?: Record<string, string>) => {
    const qs = params ? '?' + new URLSearchParams(params).toString() : '';
    return request(`/portfolios${qs}`);
  },
  getPortfolio: (id: string) => request(`/portfolios/${id}`),
  createPortfolio: (body: Record<string, unknown>) =>
    request('/portfolios', { method: 'POST', body: JSON.stringify(body) }),
  updatePortfolio: (id: string, body: Record<string, unknown>) =>
    request(`/portfolios/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  deletePortfolio: (id: string) =>
    request(`/portfolios/${id}`, { method: 'DELETE' }),

  // Positions
  getCurrentPositions: (params?: Record<string, string>) => {
    const qs = params ? '?' + new URLSearchParams(params).toString() : '';
    return request(`/positions/current${qs}`);
  },
  getPortfolioPositions: (id: string) => request(`/positions/portfolio/${id}`),
  addPosition: (portfolioId: string, body: Record<string, unknown>) =>
    request(`/positions/portfolio/${portfolioId}`, { method: 'POST', body: JSON.stringify(body) }),

  // Transactions
  getTransactions: (params?: Record<string, string>) => {
    const qs = params ? '?' + new URLSearchParams(params).toString() : '';
    return request(`/transactions${qs}`);
  },
  getPortfolioTransactions: (id: string, params?: Record<string, string>) => {
    const qs = params ? '?' + new URLSearchParams(params).toString() : '';
    return request(`/transactions/portfolio/${id}${qs}`);
  },
  createTransaction: (body: Record<string, unknown>) =>
    request('/transactions', { method: 'POST', body: JSON.stringify(body) }),

  // Reports
  getPositionReport: (portfolioId?: string) => {
    const qs = portfolioId ? `?portfolioId=${portfolioId}` : '';
    return request(`/reports/positions${qs}`);
  },
  getAuditReport: (params?: Record<string, string>) => {
    const qs = params ? '?' + new URLSearchParams(params).toString() : '';
    return request(`/reports/audit${qs}`);
  },
  getStatistics: () => request('/reports/statistics'),

  // Jobs
  processTransactions: () =>
    request('/jobs/process-transactions', { method: 'POST' }),
  generateReports: () =>
    request('/jobs/generate-reports', { method: 'POST' }),
  getJobStatus: () => request('/jobs/status'),
};
