/**
 * API Layer - Replaces COBOL CICS transaction processing
 *
 * This module replaces the following COBOL programs:
 * - INQPORT (Portfolio Inquiry) → GET /api/portfolio/:accountId
 * - INQHIST (History Inquiry)   → GET /api/history/:accountId
 * - SECMGR  (Security Manager)  → POST /api/auth/validate
 * - DB2ONLN (DB2 Online Controller) → Database access abstraction
 *
 * In production, these would connect to a real REST API backend.
 * Currently uses mock data to simulate DB2/VSAM data access.
 */

import type {
  ApiResponse,
  PortfolioPosition,
  TransactionHistoryEntry,
  SecurityResponse,
} from '../types';

// Simulated network delay (replaces CICS transaction overhead)
const SIMULATED_DELAY_MS = 300;

function delay(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

// ============================================================
// Mock Data (replaces VSAM POSFILE and DB2 POSHIST tables)
// ============================================================

const mockPortfolios: Record<string, PortfolioPosition[]> = {
  '1000000001': [
    {
      fundId: 'FND001',
      fundName: 'Growth Equity Fund',
      units: '1,250.000',
      costBasis: '$45,000.00',
      marketValue: '$52,375.00',
    },
    {
      fundId: 'FND002',
      fundName: 'Bond Income Fund',
      units: '3,500.000',
      costBasis: '$35,000.00',
      marketValue: '$36,750.00',
    },
    {
      fundId: 'FND003',
      fundName: 'International Equity Fund',
      units: '800.000',
      costBasis: '$24,000.00',
      marketValue: '$22,400.00',
    },
  ],
  '1000000002': [
    {
      fundId: 'FND004',
      fundName: 'S&P 500 Index Fund',
      units: '500.000',
      costBasis: '$75,000.00',
      marketValue: '$82,500.00',
    },
    {
      fundId: 'FND005',
      fundName: 'Real Estate Investment Trust',
      units: '2,000.000',
      costBasis: '$60,000.00',
      marketValue: '$58,000.00',
    },
  ],
  '1000000003': [
    {
      fundId: 'FND006',
      fundName: 'Technology Growth Fund',
      units: '1,000.000',
      costBasis: '$50,000.00',
      marketValue: '$67,500.00',
    },
  ],
};

const mockHistory: Record<string, TransactionHistoryEntry[]> = {
  '1000000001': [
    { date: '2026-02-20', type: 'BUY', units: '100.000', price: '$42.30', amount: '$4,230.00' },
    { date: '2026-02-18', type: 'DIV', units: '0.000', price: '$0.00', amount: '$125.50' },
    { date: '2026-02-15', type: 'SELL', units: '50.000', price: '$43.10', amount: '$2,155.00' },
    { date: '2026-02-10', type: 'BUY', units: '200.000', price: '$41.80', amount: '$8,360.00' },
    { date: '2026-02-05', type: 'BUY', units: '150.000', price: '$40.50', amount: '$6,075.00' },
    { date: '2026-01-28', type: 'SELL', units: '75.000', price: '$44.00', amount: '$3,300.00' },
    { date: '2026-01-20', type: 'DIV', units: '0.000', price: '$0.00', amount: '$98.75' },
    { date: '2026-01-15', type: 'BUY', units: '300.000', price: '$39.90', amount: '$11,970.00' },
    { date: '2026-01-10', type: 'BUY', units: '250.000', price: '$38.50', amount: '$9,625.00' },
    { date: '2026-01-05', type: 'SELL', units: '100.000', price: '$42.00', amount: '$4,200.00' },
    { date: '2025-12-28', type: 'BUY', units: '175.000', price: '$37.80', amount: '$6,615.00' },
    { date: '2025-12-20', type: 'DIV', units: '0.000', price: '$0.00', amount: '$112.30' },
  ],
  '1000000002': [
    { date: '2026-02-19', type: 'BUY', units: '50.000', price: '$150.00', amount: '$7,500.00' },
    { date: '2026-02-12', type: 'SELL', units: '25.000', price: '$152.50', amount: '$3,812.50' },
    { date: '2026-02-01', type: 'BUY', units: '100.000', price: '$148.00', amount: '$14,800.00' },
    { date: '2026-01-25', type: 'DIV', units: '0.000', price: '$0.00', amount: '$450.00' },
    { date: '2026-01-15', type: 'BUY', units: '75.000', price: '$145.00', amount: '$10,875.00' },
  ],
  '1000000003': [
    { date: '2026-02-21', type: 'BUY', units: '200.000', price: '$50.00', amount: '$10,000.00' },
    { date: '2026-02-14', type: 'SELL', units: '50.000', price: '$52.00', amount: '$2,600.00' },
    { date: '2026-02-07', type: 'BUY', units: '150.000', price: '$48.50', amount: '$7,275.00' },
  ],
};

// ============================================================
// API Functions (replace CICS LINK PROGRAM calls)
// ============================================================

/**
 * GET /api/portfolio/:accountId
 * Replaces: INQPORT program (CICS READ FILE('POSFILE'))
 * Retrieves portfolio positions for a given account.
 */
export async function getPortfolioPositions(
  accountId: string,
  page: number = 0
): Promise<ApiResponse<{ positions: PortfolioPosition[]; totalCount: number; currentIndex: number }>> {
  await delay(SIMULATED_DELAY_MS);

  const trimmedId = accountId.trim();

  if (!trimmedId) {
    return {
      data: null,
      error: 'Account number is required',
      success: false,
    };
  }

  if (trimmedId.length > 10) {
    return {
      data: null,
      error: 'Account number must not exceed 10 characters',
      success: false,
    };
  }

  const positions = mockPortfolios[trimmedId];

  if (!positions || positions.length === 0) {
    // Mirrors INQPORT P900-NOT-FOUND: 'Position not found for account'
    return {
      data: null,
      error: 'Position not found for account',
      success: false,
    };
  }

  // Pagination: show one position at a time (like BMS screen)
  const index = Math.max(0, Math.min(page, positions.length - 1));

  return {
    data: {
      positions: [positions[index]],
      totalCount: positions.length,
      currentIndex: index,
    },
    error: null,
    success: true,
  };
}

/**
 * GET /api/history/:accountId
 * Replaces: INQHIST program (DB2 SELECT FROM POSHIST)
 * Retrieves transaction history for a given account.
 */
export async function getTransactionHistory(
  accountId: string,
  page: number = 0
): Promise<ApiResponse<{ entries: TransactionHistoryEntry[]; totalCount: number; currentPage: number; totalPages: number }>> {
  await delay(SIMULATED_DELAY_MS);

  const trimmedId = accountId.trim();

  if (!trimmedId) {
    return {
      data: null,
      error: 'Account number is required',
      success: false,
    };
  }

  if (trimmedId.length > 10) {
    return {
      data: null,
      error: 'Account number must not exceed 10 characters',
      success: false,
    };
  }

  const history = mockHistory[trimmedId];

  if (!history || history.length === 0) {
    return {
      data: null,
      error: 'No transaction history found for account',
      success: false,
    };
  }

  // Pagination: 10 rows per page (matches BMS HISMAP ROW1-ROW10)
  const pageSize = 10;
  const totalPages = Math.ceil(history.length / pageSize);
  const currentPage = Math.max(0, Math.min(page, totalPages - 1));
  const startIndex = currentPage * pageSize;
  const entries = history.slice(startIndex, startIndex + pageSize);

  return {
    data: {
      entries,
      totalCount: history.length,
      currentPage,
      totalPages,
    },
    error: null,
    success: true,
  };
}

/**
 * POST /api/auth/validate
 * Replaces: SECMGR program (Validate + Authorize + Log)
 * Validates user credentials and authorizes access.
 */
export async function validateAuth(
  userId: string,
  resourceName: string = 'INQONLN'
): Promise<ApiResponse<SecurityResponse>> {
  await delay(SIMULATED_DELAY_MS);

  if (!userId || userId.trim() === '') {
    return {
      data: null,
      error: 'Unable to obtain user credentials',
      success: false,
    };
  }

  // Simulate SECMGR P100-VALIDATE-USER
  const validUsers = ['ADMIN', 'USER01', 'USER02', 'ANALYST', 'DEMO'];
  const upperUserId = userId.toUpperCase().trim();

  if (!validUsers.includes(upperUserId)) {
    return {
      data: {
        responseCode: 8,
        errorInfo: 'User validation failed',
      },
      error: 'User validation failed',
      success: false,
    };
  }

  // Simulate SECMGR P200-CHECK-AUTH (DB2 AUTHFILE lookup)
  const authorizedResources: Record<string, string[]> = {
    ADMIN: ['INQONLN', 'INQPORT', 'INQHIST'],
    USER01: ['INQONLN', 'INQPORT', 'INQHIST'],
    USER02: ['INQONLN', 'INQPORT'],
    ANALYST: ['INQONLN', 'INQHIST'],
    DEMO: ['INQONLN', 'INQPORT', 'INQHIST'],
  };

  const userResources = authorizedResources[upperUserId] || [];
  if (!userResources.includes(resourceName)) {
    return {
      data: {
        responseCode: 8,
        errorInfo: 'Access denied',
      },
      error: 'Access denied',
      success: false,
    };
  }

  // Simulate SECMGR P300-LOG-ACCESS (DB2 INSERT INTO AUDITLOG)
  console.log(
    `[AUDIT] ${new Date().toISOString()} - User: ${upperUserId}, Resource: ${resourceName}, Access: READ`
  );

  return {
    data: {
      responseCode: 0,
      errorInfo: '',
    },
    error: null,
    success: true,
  };
}
