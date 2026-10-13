import { ApiError, type AccountDetail, type AccountSummary, type PortfolioApi } from './types';

interface ErrorBody {
  kind?: 'NOT_FOUND' | 'ACCESS_DENIED' | 'SYSTEM';
  message?: string;
  code?: string;
  traceId?: string;
}

export function createHttpApi(baseUrl: string): PortfolioApi {
  async function get<T>(path: string): Promise<T> {
    let res: Response;
    try {
      res = await fetch(`${baseUrl.replace(/\/$/, '')}${path}`, { headers: { Accept: 'application/json' }, credentials: 'include' });
    } catch {
      throw new ApiError('SYSTEM', 'Unable to reach the portfolio service.', 'NETWORK');
    }
    if (res.ok) return (await res.json()) as T;
    const body: ErrorBody = await res.json().catch(() => ({}));
    if (res.status === 404) throw new ApiError('NOT_FOUND', body.message ?? 'Account not found');
    if (res.status === 403) throw new ApiError('ACCESS_DENIED', body.message ?? 'Access denied');
    throw new ApiError('SYSTEM', body.message ?? `Request failed (${res.status})`, body.code ?? `HTTP${res.status}`, body.traceId);
  }
  return {
    listAccounts: () => get<AccountSummary[]>('/accounts'),
    getAccount: (accountNo) => get<AccountDetail>(`/accounts/${encodeURIComponent(accountNo)}`),
  };
}
