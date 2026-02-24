const API_BASE = '/api';

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = localStorage.getItem('token');
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...options.headers,
  };

  if (token) {
    (headers as Record<string, string>)['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
  });

  if (response.status === 401) {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('role');
    window.location.href = '/login';
    throw new Error('Session expired');
  }

  if (!response.ok) {
    const errorData = await response.json().catch(() => null);
    throw new Error(errorData?.message || `Request failed: ${response.status}`);
  }

  return response.json();
}

export const api = {
  getPortfolios: () => request<import('../types').Portfolio[]>('/portfolios'),

  getPortfolio: (id: string) => request<import('../types').Portfolio>(`/portfolios/${id}`),

  getHistory: (id: string, page = 0, size = 10) =>
    request<import('../types').PageResponse<import('../types').Transaction>>(
      `/portfolios/${id}/history?page=${page}&size=${size}`
    ),
};
