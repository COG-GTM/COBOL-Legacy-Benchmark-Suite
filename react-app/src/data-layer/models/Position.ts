/**
 * Position Model
 * Migrated from: POSREC.cpy, INVESTMENT_POSITIONS DB2 table
 * 
 * Represents an investment position within a portfolio.
 */

import { PositionStatus, CurrencyCode } from '../types';

/**
 * Position key structure
 * From: POSREC.cpy (POS-KEY)
 */
export interface PositionKey {
  /** Portfolio identifier (8 characters) */
  portfolioId: string;
  /** Position date (YYYYMMDD format) */
  positionDate: string;
  /** Investment identifier (10 characters) */
  investmentId: string;
}

/**
 * Position data
 * From: POSREC.cpy (POS-DATA)
 */
export interface PositionData {
  /** Holding quantity (up to 11 digits with 4 decimal places) */
  quantity: number;
  /** Total cost basis (up to 13 digits with 2 decimal places) */
  costBasis: number;
  /** Current market value (up to 13 digits with 2 decimal places) */
  marketValue: number;
  /** Currency code */
  currencyCode: CurrencyCode;
  /** Position status */
  status: PositionStatus;
}

/**
 * Position audit information
 * From: POSREC.cpy (POS-AUDIT)
 */
export interface PositionAuditInfo {
  /** Last maintenance timestamp */
  lastMaintenanceDate: Date;
  /** Last user who modified the position */
  lastMaintenanceUser: string;
}

/**
 * Complete Position record
 * Combines all position information from POSREC.cpy and INVESTMENT_POSITIONS table
 */
export interface Position {
  /** Position key */
  key: PositionKey;
  /** Position data */
  data: PositionData;
  /** Audit information */
  auditInfo: PositionAuditInfo;
}

/**
 * Position with calculated fields
 * Extended position with derived values
 */
export interface PositionWithCalculations extends Position {
  /** Unrealized gain/loss (market value - cost basis) */
  unrealizedGainLoss: number;
  /** Unrealized gain/loss percentage */
  unrealizedGainLossPercent: number;
  /** Average cost per unit */
  averageCost: number;
  /** Current price per unit */
  currentPrice: number;
}

/**
 * Position creation request
 */
export interface CreatePositionRequest {
  portfolioId: string;
  investmentId: string;
  quantity: number;
  costBasis: number;
  marketValue: number;
  currencyCode: CurrencyCode;
}

/**
 * Position update request
 */
export interface UpdatePositionRequest {
  portfolioId: string;
  investmentId: string;
  positionDate: string;
  quantity?: number;
  costBasis?: number;
  marketValue?: number;
  status?: PositionStatus;
}

/**
 * Position search criteria
 */
export interface PositionSearchCriteria {
  portfolioId?: string;
  investmentId?: string;
  positionDateFrom?: string;
  positionDateTo?: string;
  status?: PositionStatus;
  currencyCode?: CurrencyCode;
  minQuantity?: number;
  maxQuantity?: number;
  minMarketValue?: number;
  maxMarketValue?: number;
}

/**
 * Position summary for list views
 */
export interface PositionSummary {
  portfolioId: string;
  investmentId: string;
  positionDate: string;
  quantity: number;
  marketValue: number;
  unrealizedGainLoss: number;
  status: PositionStatus;
}

/**
 * Portfolio positions aggregate
 */
export interface PortfolioPositionsAggregate {
  portfolioId: string;
  totalPositions: number;
  totalMarketValue: number;
  totalCostBasis: number;
  totalUnrealizedGainLoss: number;
  positions: PositionSummary[];
}

/**
 * Factory function to create a default Position object
 */
export function createDefaultPosition(): Position {
  const today = new Date();
  const dateStr = today.toISOString().slice(0, 10).replace(/-/g, '');
  
  return {
    key: {
      portfolioId: '',
      investmentId: '',
      positionDate: dateStr,
    },
    data: {
      quantity: 0,
      costBasis: 0,
      marketValue: 0,
      currencyCode: CurrencyCode.USD,
      status: PositionStatus.ACTIVE,
    },
    auditInfo: {
      lastMaintenanceDate: new Date(),
      lastMaintenanceUser: '',
    },
  };
}

/**
 * Calculate derived position values
 */
export function calculatePositionMetrics(position: Position): PositionWithCalculations {
  const unrealizedGainLoss = position.data.marketValue - position.data.costBasis;
  const unrealizedGainLossPercent = position.data.costBasis !== 0
    ? (unrealizedGainLoss / position.data.costBasis) * 100
    : 0;
  const averageCost = position.data.quantity !== 0
    ? position.data.costBasis / position.data.quantity
    : 0;
  const currentPrice = position.data.quantity !== 0
    ? position.data.marketValue / position.data.quantity
    : 0;

  return {
    ...position,
    unrealizedGainLoss,
    unrealizedGainLossPercent,
    averageCost,
    currentPrice,
  };
}
