/**
 * MSW request handlers implementing the mock JSON API.
 *
 * These mirror the CICS online inquiry flow:
 *   - GET /api/portfolios/:account          -> INQPORT (POSFILE read + POSMAP)
 *   - GET /api/portfolios/:account/transactions?page=  -> INQHIST (POSHIST fetch)
 *
 * "Not found" responses mirror the COBOL NOTFND / "Position not found for
 * account" (INQPORT P900-NOT-FOUND) and empty-history flows.
 */
import { http, HttpResponse } from 'msw';
import {
  portfolios,
  positionsByAccount,
  transactionsByAccount,
} from './data';
import {
  TRANSACTION_PAGE_SIZE,
  type ApiErrorBody,
  type PortfolioResponse,
  type TransactionsResponse,
} from '../types/portfolio';

const notFound = (message: string, code = '00013') =>
  HttpResponse.json<ApiErrorBody>({ message, code }, { status: 404 });

export const handlers = [
  // GET /api/portfolios/:account  — portfolio master + current positions.
  http.get('/api/portfolios/:account', ({ params }) => {
    const account = String(params.account).trim().toUpperCase();
    const portfolio = portfolios.find((p) => p.accountNo === account);

    if (!portfolio) {
      return notFound('Position not found for account');
    }

    const body: PortfolioResponse = {
      portfolio,
      positions: positionsByAccount[account] ?? [],
    };
    return HttpResponse.json(body);
  }),

  // GET /api/portfolios/:account/transactions?page=  — paginated history.
  http.get('/api/portfolios/:account/transactions', ({ params, request }) => {
    const account = String(params.account).trim().toUpperCase();
    const portfolio = portfolios.find((p) => p.accountNo === account);

    if (!portfolio) {
      return notFound('Position not found for account');
    }

    const url = new URL(request.url);
    const requestedPage = Number.parseInt(url.searchParams.get('page') ?? '1', 10);
    const rows = transactionsByAccount[account] ?? [];
    const total = rows.length;
    const totalPages = Math.max(1, Math.ceil(total / TRANSACTION_PAGE_SIZE));
    const page = Number.isNaN(requestedPage)
      ? 1
      : Math.min(Math.max(requestedPage, 1), totalPages);
    const start = (page - 1) * TRANSACTION_PAGE_SIZE;

    const body: TransactionsResponse = {
      account,
      page,
      pageSize: TRANSACTION_PAGE_SIZE,
      total,
      totalPages,
      transactions: rows.slice(start, start + TRANSACTION_PAGE_SIZE),
    };
    return HttpResponse.json(body);
  }),
];
