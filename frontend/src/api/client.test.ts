import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { server } from '../mocks/server';
import { ApiRequestError } from './errors';
import { createHttpClient } from './client';

const base = () => `${window.location.origin}/api/v1`;

describe('typed REST client (section 7)', () => {
  it('sends X-Request-Id and Accept headers and a fixed pageSize', async () => {
    let seen: { requestId: string | null; accept: string | null; url: string } | null = null;
    server.use(
      http.get('*/api/v1/accounts/:acct/transactions', ({ request }) => {
        seen = { requestId: request.headers.get('X-Request-Id'), accept: request.headers.get('Accept'), url: request.url };
        return HttpResponse.json({ items: [], page: 2, pageSize: 10, totalCount: 0, hasNext: false, hasPrevious: true });
      }),
    );
    await createHttpClient({ baseUrl: base() }).getTransactions('1000000001', 2);
    expect(seen).not.toBeNull();
    expect(seen!.requestId).toMatch(/.+/);
    expect(seen!.accept).toBe('application/json');
    expect(seen!.url).toContain('/accounts/1000000001/transactions?page=2&pageSize=10');
  });

  it('keeps decimals as strings', async () => {
    const page = await createHttpClient({ baseUrl: base() }).getPositions('1000000002', 1);
    expect(typeof page.items[0]?.units).toBe('string');
  });

  it('normalises an ApiError body and falls back to the X-Trace-Id header', async () => {
    server.use(
      http.get('*/api/v1/accounts/:acct/positions', () =>
        HttpResponse.json({ code: 'POSITION_READ_ERROR' }, { status: 500, headers: { 'X-Trace-Id': 'TRACE123' } }),
      ),
    );
    const err = await createHttpClient({ baseUrl: base() })
      .getPositions('1000000001', 1)
      .catch((e: unknown) => e);
    expect(err).toBeInstanceOf(ApiRequestError);
    const { body: apiError, status } = err as ApiRequestError;
    expect(status).toBe(500);
    expect(apiError).toMatchObject({
      code: 'POSITION_READ_ERROR',
      message: 'Error accessing position data',
      severity: 'ERROR',
      action: 'CONTINUE',
      traceId: 'TRACE123',
    });
  });

  it('treats a non-JSON gateway failure as an inline SYSTEM_ERROR, not an abend', async () => {
    server.use(http.get('*/api/v1/accounts/:acct/transactions', () => new HttpResponse('bad gateway', { status: 502 })));
    const err = (await createHttpClient({ baseUrl: base() })
      .getTransactions('1000000001', 1)
      .catch((e: unknown) => e)) as ApiRequestError;
    expect(err.body.code).toBe('SYSTEM_ERROR');
    expect(err.body.action).toBe('CONTINUE');
  });
});
