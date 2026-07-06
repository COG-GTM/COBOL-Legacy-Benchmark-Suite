/**
 * API client for the Portfolio Management System frontend.
 *
 * In development these endpoints are served by the MSW mock (see src/mocks).
 * In production they are expected to be fronted by a JSON API that exposes the
 * COBOL INQPORT / INQHIST programs over z/OS Connect EE or CICS Web Services
 * (see web/README.md). The client itself is transport-agnostic: point
 * VITE_API_BASE_URL at the real gateway and nothing else needs to change.
 */
import type {
  ApiErrorBody,
  PortfolioResponse,
  TransactionsResponse,
} from '../types/portfolio';

/** Base URL for the API. Empty string keeps requests same-origin (MSW dev). */
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '';

/** Error thrown for non-2xx API responses; carries the parsed error body. */
export class ApiError extends Error {
  readonly status: number;
  readonly code?: string;

  constructor(status: number, body: ApiErrorBody) {
    super(body.message);
    this.name = 'ApiError';
    this.status = status;
    this.code = body.code;
  }
}

async function request<T>(path: string, signal?: AbortSignal): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      headers: { Accept: 'application/json' },
      signal,
    });
  } catch (cause) {
    if (cause instanceof DOMException && cause.name === 'AbortError') {
      throw cause;
    }
    throw new ApiError(0, { message: 'Unable to reach the inquiry service' });
  }

  if (!response.ok) {
    let body: ApiErrorBody = { message: `Request failed (${response.status})` };
    try {
      body = (await response.json()) as ApiErrorBody;
    } catch {
      // Non-JSON error body; keep the default message.
    }
    throw new ApiError(response.status, body);
  }

  return (await response.json()) as T;
}

/** GET /api/portfolios/{account} — mirrors INQPORT position inquiry. */
export function getPortfolio(
  account: string,
  signal?: AbortSignal,
): Promise<PortfolioResponse> {
  return request<PortfolioResponse>(
    `/api/portfolios/${encodeURIComponent(account)}`,
    signal,
  );
}

/**
 * GET /api/portfolios/{account}/transactions?page= — mirrors INQHIST history
 * inquiry. Page size is fixed server-side at 10 (COBOL fetch of 10 rows).
 */
export function getTransactions(
  account: string,
  page: number,
  signal?: AbortSignal,
): Promise<TransactionsResponse> {
  const query = new URLSearchParams({ page: String(page) });
  return request<TransactionsResponse>(
    `/api/portfolios/${encodeURIComponent(account)}/transactions?${query}`,
    signal,
  );
}
