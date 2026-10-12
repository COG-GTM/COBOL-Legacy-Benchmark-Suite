import { delay, http, HttpResponse } from 'msw';
import { ERROR_CATALOGUE } from '../api/errors';
import type { ApiError, ApiErrorCode, Page, SessionInfo } from '../api/types';
import { isValidAccountNo } from '../state/validation';
import { positionsByAccount, transactionsByAccount, TRIGGER_ACCOUNTS } from './fixtures';

export type MockUser = 'default' | 'denied' | 'anon';

let mockUser: MockUser = 'default';

/** `?mockUser=denied|anon` on the app URL selects the mocked session (section 7.4). */
export function setMockUser(user: MockUser) {
  mockUser = user;
}

export function initMockUserFromUrl(search: string) {
  const value = new URLSearchParams(search).get('mockUser');
  setMockUser(value === 'denied' || value === 'anon' ? value : 'default');
}

function traceId(): string {
  let id = '';
  for (let i = 0; i < 16; i++) id += Math.floor(Math.random() * 16).toString(16);
  return id.toUpperCase();
}

function errorResponse(code: ApiErrorCode, overrides: Partial<ApiError> = {}) {
  const def = ERROR_CATALOGUE[code];
  const body: ApiError = {
    code,
    message: def.message,
    severity: def.severity,
    action: def.action,
    traceId: traceId(),
    legacyCode: def.legacyCode,
    timestamp: new Date().toISOString(),
    ...overrides,
  };
  return HttpResponse.json(body, { status: def.status, headers: { 'X-Trace-Id': body.traceId } });
}

function paginate<T>(rows: T[], url: URL): Page<T> {
  const page = Math.max(1, Number.parseInt(url.searchParams.get('page') ?? '1', 10) || 1);
  const requested = Number.parseInt(url.searchParams.get('pageSize') ?? '10', 10) || 10;
  const pageSize = Math.min(50, Math.max(1, requested));
  const start = (page - 1) * pageSize;
  const items = rows.slice(start, start + pageSize);
  return {
    items,
    page,
    pageSize,
    totalCount: rows.length,
    hasNext: start + pageSize < rows.length,
    hasPrevious: page > 1,
  };
}

/** Shared checks for both data endpoints, in the order a real API would apply them. */
function commonGuards(accountNo: string, readErrorCode: 'POSITION_READ_ERROR' | 'HISTORY_READ_ERROR') {
  if (mockUser === 'anon') return errorResponse('UNAUTHENTICATED');
  if (mockUser === 'denied') return errorResponse('ACCESS_DENIED');
  if (!isValidAccountNo(accountNo)) return errorResponse('INVALID_ACCOUNT_FORMAT', { field: 'accountNo' });
  switch (accountNo) {
    case TRIGGER_ACCOUNTS.accessDenied:
      return errorResponse('ACCESS_DENIED');
    case TRIGGER_ACCOUNTS.authCheckFailed:
      return errorResponse('AUTH_CHECK_FAILED');
    case TRIGGER_ACCOUNTS.readError:
      return errorResponse(readErrorCode);
    case TRIGGER_ACCOUNTS.systemError:
      return errorResponse('SYSTEM_ERROR');
    default:
      return null;
  }
}

function mockDelayMs(): number {
  const raw = import.meta.env.VITE_MOCK_DELAY_MS;
  const ms = raw === undefined ? 0 : Number(raw);
  return Number.isFinite(ms) && ms > 0 ? ms : 0;
}

export function createHandlers(baseUrl = '/api/v1') {
  const base = baseUrl.replace(/\/+$/, '');
  return [
    http.get(`${base}/session`, async () => {
      await delay(mockDelayMs());
      if (mockUser === 'anon') return errorResponse('UNAUTHENTICATED');
      const session: SessionInfo = {
        userId: 'PINQUSR1',
        displayName: 'Demo Inquiry User',
        permissions: mockUser === 'denied' ? [] : [{ resource: 'INQONLN', accessType: 'READ' }],
      };
      return HttpResponse.json(session);
    }),

    http.get(`${base}/accounts/:accountNo/positions`, async ({ params, request }) => {
      await delay(mockDelayMs());
      const accountNo = decodeURIComponent(String(params.accountNo));
      const guard = commonGuards(accountNo, 'POSITION_READ_ERROR');
      if (guard) return guard;
      const rows = [...(positionsByAccount[accountNo] ?? [])].sort((a, b) => a.investmentId.localeCompare(b.investmentId));
      if (rows.length === 0) return errorResponse('POSITION_NOT_FOUND', { field: 'accountNo' });
      return HttpResponse.json(paginate(rows, new URL(request.url)));
    }),

    http.get(`${base}/accounts/:accountNo/transactions`, async ({ params, request }) => {
      await delay(mockDelayMs());
      const accountNo = decodeURIComponent(String(params.accountNo));
      const guard = commonGuards(accountNo, 'HISTORY_READ_ERROR');
      if (guard) return guard;
      if (accountNo === TRIGGER_ACCOUNTS.dbUnavailable) return errorResponse('DB_UNAVAILABLE');
      const rows = [...(transactionsByAccount[accountNo] ?? [])].sort((a, b) =>
        `${b.tradeDate}T${b.tradeTime}`.localeCompare(`${a.tradeDate}T${a.tradeTime}`),
      );
      return HttpResponse.json(paginate(rows, new URL(request.url)));
    }),
  ];
}
