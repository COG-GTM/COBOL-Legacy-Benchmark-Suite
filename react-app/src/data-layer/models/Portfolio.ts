/**
 * Portfolio Model
 * Migrated from: PORTFLIO.cpy, PORTFOLIO_MASTER DB2 table
 * 
 * Represents a client's investment portfolio containing positions and transactions.
 */

import { PortfolioStatus, ClientType, CurrencyCode } from '../types';

/**
 * Portfolio key structure
 * From: PORTFLIO.cpy (PORT-KEY)
 */
export interface PortfolioKey {
  /** Portfolio identifier (8 characters) */
  portfolioId: string;
  /** Account number (10 characters) */
  accountNumber: string;
}

/**
 * Portfolio client information
 * From: PORTFLIO.cpy (PORT-CLIENT-INFO)
 */
export interface PortfolioClientInfo {
  /** Client name (up to 30 characters) */
  clientName: string;
  /** Client type: Individual, Corporate, or Trust */
  clientType: ClientType;
}

/**
 * Portfolio financial information
 * From: PORTFLIO.cpy (PORT-FINANCIAL-INFO)
 */
export interface PortfolioFinancialInfo {
  /** Total portfolio value */
  totalValue: number;
  /** Cash balance available */
  cashBalance: number;
  /** Currency code for the portfolio */
  currencyCode: CurrencyCode;
}

/**
 * Portfolio audit information
 * From: PORTFLIO.cpy (PORT-AUDIT-INFO)
 */
export interface PortfolioAuditInfo {
  /** Last user who modified the portfolio */
  lastUser: string;
  /** Last transaction date (YYYYMMDD format) */
  lastTransactionDate: string;
  /** Last maintenance timestamp */
  lastMaintenanceDate: Date;
}

/**
 * Complete Portfolio record
 * Combines all portfolio information from PORTFLIO.cpy and PORTFOLIO_MASTER table
 */
export interface Portfolio {
  /** Portfolio key containing ID and account number */
  key: PortfolioKey;
  /** Client information */
  clientInfo: PortfolioClientInfo;
  /** Portfolio creation date */
  createDate: Date;
  /** Portfolio close date (null if still open) */
  closeDate: Date | null;
  /** Portfolio status */
  status: PortfolioStatus;
  /** Financial information */
  financialInfo: PortfolioFinancialInfo;
  /** Audit trail information */
  auditInfo: PortfolioAuditInfo;
  /** Risk level (1 character) */
  riskLevel: string;
  /** Branch ID (2 characters) */
  branchId: string;
}

/**
 * Portfolio creation request
 * Used when creating a new portfolio
 */
export interface CreatePortfolioRequest {
  portfolioId: string;
  accountNumber: string;
  clientName: string;
  clientType: ClientType;
  currencyCode: CurrencyCode;
  riskLevel: string;
  branchId: string;
  initialCashBalance?: number;
}

/**
 * Portfolio update request
 * Used when updating an existing portfolio
 */
export interface UpdatePortfolioRequest {
  portfolioId: string;
  clientName?: string;
  status?: PortfolioStatus;
  riskLevel?: string;
  cashBalance?: number;
}

/**
 * Portfolio search criteria
 * Used for querying portfolios
 */
export interface PortfolioSearchCriteria {
  portfolioId?: string;
  accountNumber?: string;
  clientName?: string;
  clientType?: ClientType;
  status?: PortfolioStatus;
  branchId?: string;
  createDateFrom?: Date;
  createDateTo?: Date;
}

/**
 * Portfolio summary for list views
 */
export interface PortfolioSummary {
  portfolioId: string;
  accountNumber: string;
  clientName: string;
  status: PortfolioStatus;
  totalValue: number;
  cashBalance: number;
  currencyCode: CurrencyCode;
}

/**
 * Factory function to create a default Portfolio object
 */
export function createDefaultPortfolio(): Portfolio {
  return {
    key: {
      portfolioId: '',
      accountNumber: '',
    },
    clientInfo: {
      clientName: '',
      clientType: ClientType.INDIVIDUAL,
    },
    createDate: new Date(),
    closeDate: null,
    status: PortfolioStatus.ACTIVE,
    financialInfo: {
      totalValue: 0,
      cashBalance: 0,
      currencyCode: CurrencyCode.USD,
    },
    auditInfo: {
      lastUser: '',
      lastTransactionDate: '',
      lastMaintenanceDate: new Date(),
    },
    riskLevel: '',
    branchId: '',
  };
}
