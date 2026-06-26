import type {
  AuthResult,
  HistoryResult,
  PositionResult,
  Transaction,
} from '../types';
import {
  ERROR_ACCOUNT,
  MOCK_HISTORY,
  MOCK_POSITIONS,
} from '../data/mockData';

/**
 * Mock service layer for the online inquiry subsystem.
 *
 * Every screen talks to the system ONLY through this module, so a real backend
 * API can be dropped in later by reimplementing these functions (e.g. with
 * `fetch`) without changing any UI code. The async signatures already model a
 * network round-trip.
 *
 * Mapping to legacy programs:
 *   getPosition  -> INQPORT (EXEC CICS READ FILE('POSFILE') + NOTFND/ERROR)
 *   getHistory   -> INQHIST (DB2 cursor over POSHIST, ORDER BY TRANS_DATE DESC)
 *   authenticate -> SECMGR  (EXEC CICS ASSIGN USERID + access check)
 */

export const ACCOUNT_LENGTH = 10;
export const HISTORY_PAGE_SIZE = 10;

/** Simulated latency so the async/loading states are realistic. */
const LATENCY_MS = 250;
const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

/**
 * Normalize raw account input the way the legacy 3270 field would: trim and
 * pad-compare on a fixed 10-char width. Returns an error message when invalid.
 */
export function validateAccount(raw: string): string | null {
  const value = raw.trim();
  if (value.length === 0) {
    return 'Account number is required';
  }
  if (value.length > ACCOUNT_LENGTH) {
    return `Account number must be ${ACCOUNT_LENGTH} characters or fewer`;
  }
  if (!/^[0-9]+$/.test(value)) {
    return 'Account number must be numeric';
  }
  return null;
}

/** Left-pad a numeric account to the 10-char VSAM key width. */
function normalizeAccount(raw: string): string {
  return raw.trim().padStart(ACCOUNT_LENGTH, '0');
}

/**
 * Portfolio position lookup. Mirrors INQPORT:
 *  - NORMAL response  -> { FOUND }
 *  - NOTFND condition -> { NOT_FOUND, 'Position not found for account' }
 *  - ERROR condition  -> { ERROR,  'Error accessing position data' }
 */
export async function getPosition(rawAccount: string): Promise<PositionResult> {
  await delay(LATENCY_MS);
  const account = normalizeAccount(rawAccount);

  if (account === ERROR_ACCOUNT) {
    return {
      status: 'ERROR',
      responseCode: 12,
      errorMsg: 'Error accessing position data',
    };
  }

  const position = MOCK_POSITIONS[account];
  if (!position) {
    return { status: 'NOT_FOUND', errorMsg: 'Position not found for account' };
  }
  return { status: 'FOUND', position };
}

/**
 * Transaction history lookup with paging. Mirrors INQHIST: rows are sorted by
 * date descending and returned 10 at a time (HISTORY_PAGE_SIZE).
 *
 * @param page 1-based page index.
 */
export async function getHistory(
  rawAccount: string,
  page = 1,
): Promise<HistoryResult> {
  await delay(LATENCY_MS);
  const account = normalizeAccount(rawAccount);

  if (account === ERROR_ACCOUNT) {
    return {
      status: 'ERROR',
      responseCode: -911,
      errorMsg: 'Error accessing transaction history',
    };
  }

  const all: Transaction[] = [...(MOCK_HISTORY[account] ?? [])].sort(
    (a, b) => b.date.localeCompare(a.date),
  );

  const totalRows = all.length;
  const totalPages = Math.max(1, Math.ceil(totalRows / HISTORY_PAGE_SIZE));
  const safePage = Math.min(Math.max(1, page), totalPages);
  const start = (safePage - 1) * HISTORY_PAGE_SIZE;
  const rows = all.slice(start, start + HISTORY_PAGE_SIZE);

  return {
    status: 'OK',
    page: {
      rows,
      page: safePage,
      pageSize: HISTORY_PAGE_SIZE,
      totalRows,
      totalPages,
      hasPrevious: safePage > 1,
      hasNext: safePage < totalPages,
    },
  };
}

/**
 * Stubbed SECMGR USERID check. Any non-empty user id is accepted today; wire
 * this to real authentication (RACF/SSO) later by replacing the body.
 */
export async function authenticate(rawUserId: string): Promise<AuthResult> {
  await delay(LATENCY_MS);
  const userId = rawUserId.trim().toUpperCase();
  if (userId.length === 0) {
    return { status: 'DENIED', errorMsg: 'User ID is required to sign on' };
  }
  if (userId.length > 8) {
    return { status: 'DENIED', errorMsg: 'User ID must be 8 characters or fewer' };
  }
  return { status: 'OK', userId };
}
