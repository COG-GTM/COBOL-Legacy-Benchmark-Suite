/**
 * API request/response types for the Investment Portfolio Management system.
 * Error response mirrors ERRMAP fields ERRCOUT and ERRDOUT from INQSET.bms (lines 89-100).
 * Pagination replaces PF7/PF8 (Previous/Next) from BMS maps.
 */

export interface ErrorResponse {
  code: string;
  message: string;
}

export interface PaginationParams {
  page?: number;
  limit?: number;
}

export interface PaginatedResponse<T> {
  items: T[];
  total: number;
  page: number;
  limit: number;
  totalPages: number;
}

export interface PortfolioListParams extends PaginationParams {
  status?: 'ACTIVE' | 'CLOSED' | 'SUSPENDED';
  clientType?: 'INDIVIDUAL' | 'CORPORATE' | 'TRUST';
}

export interface TransactionListParams extends PaginationParams {
  startDate?: string;
  endDate?: string;
  type?: 'BUY' | 'SELL' | 'TRANSFER' | 'FEE';
  status?: 'PENDING' | 'DONE' | 'FAILED' | 'REVERSED';
}

export interface PositionListParams extends PaginationParams {
  status?: 'ACTIVE' | 'CLOSED' | 'PENDING';
}

export interface DashboardSummary {
  totalPortfolios: number;
  activePortfolios: number;
  totalMarketValue: number;
  totalCashBalance: number;
  recentTransactionCount: number;
}

export interface ApiSuccessResponse<T> {
  data: T;
}

export interface ApiErrorResponse {
  error: ErrorResponse;
}
