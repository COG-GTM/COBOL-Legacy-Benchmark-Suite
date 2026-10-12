import { ApiRequestError, normaliseApiError } from './errors';
import type { AccountNo, PositionPage, SessionInfo, TransactionPage } from './types';
import { HISTORY_PAGE_SIZE, POSITION_PAGE_SIZE } from './types';

export interface PortfolioApi {
  getSession(signal?: AbortSignal): Promise<SessionInfo>;
  getPositions(accountNo: AccountNo, page: number, signal?: AbortSignal): Promise<PositionPage>;
  getTransactions(accountNo: AccountNo, page: number, signal?: AbortSignal): Promise<TransactionPage>;
}

export interface HttpClientOptions {
  baseUrl: string;
  fetchImpl?: typeof fetch;
}

function newRequestId(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) return crypto.randomUUID();
  return `${Date.now().toString(16)}-${Math.random().toString(16).slice(2)}`;
}

/** REST client for the section 7.1 contract. The same client is used against the mock and a real backend. */
export function createHttpClient({ baseUrl, fetchImpl }: HttpClientOptions): PortfolioApi {
  const root = baseUrl.replace(/\/+$/, '');

  async function get<T>(path: string, signal?: AbortSignal): Promise<T> {
    const requestId = newRequestId();
    let response: Response;
    try {
      response = await (fetchImpl ?? fetch)(`${root}${path}`, {
        method: 'GET',
        headers: { Accept: 'application/json', 'X-Request-Id': requestId },
        credentials: 'include',
        signal,
      });
    } catch (cause) {
      if (cause instanceof DOMException && cause.name === 'AbortError') throw cause;
      throw new ApiRequestError(0, normaliseApiError(0, null, requestId));
    }

    const traceId = response.headers.get('X-Trace-Id');
    let body: unknown = null;
    const text = await response.text();
    if (text) {
      try {
        body = JSON.parse(text);
      } catch {
        body = null;
      }
    }

    if (!response.ok) {
      const error = normaliseApiError(response.status, body, traceId ?? requestId);
      if (import.meta.env.DEV) {
        console.warn(`[api] ${response.status} ${error.code} on ${path} (traceId ${error.traceId})`);
      }
      throw new ApiRequestError(response.status, error);
    }
    return body as T;
  }

  const enc = encodeURIComponent;
  return {
    getSession: (signal) => get<SessionInfo>('/session', signal),
    getPositions: (accountNo, page, signal) =>
      get<PositionPage>(`/accounts/${enc(accountNo)}/positions?page=${page}&pageSize=${POSITION_PAGE_SIZE}`, signal),
    getTransactions: (accountNo, page, signal) =>
      get<TransactionPage>(`/accounts/${enc(accountNo)}/transactions?page=${page}&pageSize=${HISTORY_PAGE_SIZE}`, signal),
  };
}

export const apiConfig = {
  mode: (import.meta.env.VITE_API_MODE ?? 'mock') as 'mock' | 'real',
  baseUrl: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  loginUrl: import.meta.env.VITE_AUTH_LOGIN_URL || '',
  logoutUrl: import.meta.env.VITE_AUTH_LOGOUT_URL || '',
};

export const api: PortfolioApi = createHttpClient({ baseUrl: apiConfig.baseUrl });
