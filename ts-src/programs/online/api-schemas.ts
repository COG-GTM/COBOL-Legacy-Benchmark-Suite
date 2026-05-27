/**
 * API Request/Response Schemas.
 * Migrated from: src/maps/INQSET.bms (BMS mapset)
 *
 * BMS map fields become typed request/response interfaces for the REST API.
 */

// ── Menu ────────────────────────────────────────────────────────────────

export interface MenuRequest {
  option: string;
  accountNo?: string;
}

export interface MenuResponse {
  title: string;
  options: { code: string; description: string }[];
  message?: string;
}

// ── Portfolio Position Inquiry ──────────────────────────────────────────

export interface PositionInquiryRequest {
  accountNo: string;
}

export interface PositionInquiryResponse {
  accountNo: string;
  portfolioId: string;
  clientName: string;
  status: string;
  totalValue: number;
  cashBalance: number;
  positions: PositionLineItem[];
  message?: string;
}

export interface PositionLineItem {
  investmentId: string;
  description: string;
  investmentType: string;
  quantity: number;
  costBasis: number;
  marketValue: number;
  percentChange: number;
}

// ── Transaction History Inquiry ────────────────────────────────────────

export interface HistoryInquiryRequest {
  accountNo: string;
  startDate?: string;
  endDate?: string;
  limit?: number;
}

export interface HistoryInquiryResponse {
  accountNo: string;
  transactions: HistoryLineItem[];
  totalCount: number;
  message?: string;
}

export interface HistoryLineItem {
  date: string;
  time: string;
  type: string;
  securityId: string;
  quantity: number;
  price: number;
  amount: number;
  fees: number;
}

// ── Error response ─────────────────────────────────────────────────────

export interface ErrorResponse {
  error: string;
  code?: string;
  details?: string;
}
