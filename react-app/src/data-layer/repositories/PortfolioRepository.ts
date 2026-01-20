/**
 * Portfolio Repository
 * Provides data access operations for Portfolio entities
 * 
 * Mirrors data access patterns from:
 * - VSAM PORTMSTR file operations
 * - DB2 PORTFOLIO_MASTER table operations
 * - INQPORT online program queries
 */

import {
  BaseRepository,
  InMemoryStorage,
  PaginationOptions,
  SortOptions,
  PaginatedResult,
  RepositoryResult,
} from './BaseRepository';
import {
  Portfolio,
  PortfolioKey,
  CreatePortfolioRequest,
  UpdatePortfolioRequest,
  PortfolioSearchCriteria,
  PortfolioSummary,
  createDefaultPortfolio,
} from '../models/Portfolio';
import { PortfolioStatus, ClientType, CurrencyCode } from '../types';

/**
 * Portfolio Repository Interface
 */
export interface IPortfolioRepository {
  findByKey(key: PortfolioKey): Promise<RepositoryResult<Portfolio>>;
  findByPortfolioId(portfolioId: string): Promise<RepositoryResult<Portfolio>>;
  findByAccountNumber(accountNumber: string): Promise<RepositoryResult<Portfolio[]>>;
  findByClientName(clientName: string): Promise<RepositoryResult<Portfolio[]>>;
  findAll(
    criteria?: PortfolioSearchCriteria,
    pagination?: PaginationOptions,
    sort?: SortOptions<Portfolio>
  ): Promise<PaginatedResult<Portfolio>>;
  findActivePortfolios(): Promise<Portfolio[]>;
  create(request: CreatePortfolioRequest): Promise<RepositoryResult<Portfolio>>;
  update(request: UpdatePortfolioRequest): Promise<RepositoryResult<Portfolio>>;
  delete(key: PortfolioKey): Promise<RepositoryResult<boolean>>;
  exists(key: PortfolioKey): Promise<boolean>;
  count(criteria?: PortfolioSearchCriteria): Promise<number>;
  getSummaries(criteria?: PortfolioSearchCriteria): Promise<PortfolioSummary[]>;
  updateTotalValue(portfolioId: string, totalValue: number): Promise<RepositoryResult<Portfolio>>;
  updateCashBalance(portfolioId: string, cashBalance: number): Promise<RepositoryResult<Portfolio>>;
  closePortfolio(portfolioId: string): Promise<RepositoryResult<Portfolio>>;
}

/**
 * In-memory Portfolio Repository implementation
 * Used for development and testing
 */
export class InMemoryPortfolioRepository
  extends BaseRepository<Portfolio, PortfolioKey, CreatePortfolioRequest, UpdatePortfolioRequest, PortfolioSearchCriteria>
  implements IPortfolioRepository {
  
  private storage: InMemoryStorage<Portfolio>;

  constructor() {
    super('Portfolio');
    this.storage = new InMemoryStorage<Portfolio>();
  }

  /**
   * Generate a storage key from a portfolio key
   */
  private getStorageKey(key: PortfolioKey): string {
    return `${key.portfolioId}-${key.accountNumber}`;
  }

  /**
   * Find a portfolio by its composite key
   */
  async findByKey(key: PortfolioKey): Promise<RepositoryResult<Portfolio>> {
    const storageKey = this.getStorageKey(key);
    const portfolio = this.storage.get(storageKey);
    
    if (!portfolio) {
      return this.errorResult('Portfolio not found', 'E001');
    }
    
    return this.successResult(portfolio);
  }

  /**
   * Find a portfolio by portfolio ID only
   */
  async findByPortfolioId(portfolioId: string): Promise<RepositoryResult<Portfolio>> {
    const portfolios = this.storage.getAll();
    const portfolio = portfolios.find(p => p.key.portfolioId === portfolioId);
    
    if (!portfolio) {
      return this.errorResult('Portfolio not found', 'E001');
    }
    
    return this.successResult(portfolio);
  }

  /**
   * Find all portfolios for an account number
   */
  async findByAccountNumber(accountNumber: string): Promise<RepositoryResult<Portfolio[]>> {
    const portfolios = this.storage.getAll();
    const filtered = portfolios.filter(p => p.key.accountNumber === accountNumber);
    return this.successResult(filtered);
  }

  /**
   * Find portfolios by client name (partial match)
   */
  async findByClientName(clientName: string): Promise<RepositoryResult<Portfolio[]>> {
    const portfolios = this.storage.getAll();
    const searchTerm = clientName.toLowerCase();
    const filtered = portfolios.filter(p => 
      p.clientInfo.clientName.toLowerCase().includes(searchTerm)
    );
    return this.successResult(filtered);
  }

  /**
   * Find all portfolios matching criteria with pagination
   */
  async findAll(
    criteria?: PortfolioSearchCriteria,
    pagination?: PaginationOptions,
    sort?: SortOptions<Portfolio>
  ): Promise<PaginatedResult<Portfolio>> {
    let portfolios = this.storage.getAll();
    
    // Apply filters
    if (criteria) {
      portfolios = this.applyFilters(portfolios, criteria);
    }
    
    // Get total count before pagination
    const totalCount = portfolios.length;
    
    // Apply sorting
    portfolios = this.applySort(portfolios, sort);
    
    // Apply pagination
    const page = pagination?.page ?? 1;
    const pageSize = pagination?.pageSize ?? 10;
    portfolios = this.applyPagination(portfolios, pagination);
    
    return this.paginatedResult(portfolios, totalCount, page, pageSize);
  }

  /**
   * Find all active portfolios
   */
  async findActivePortfolios(): Promise<Portfolio[]> {
    const portfolios = this.storage.getAll();
    return portfolios.filter(p => 
      p.status === PortfolioStatus.ACTIVE &&
      (p.closeDate === null || p.closeDate > new Date())
    );
  }

  /**
   * Create a new portfolio
   */
  async create(request: CreatePortfolioRequest): Promise<RepositoryResult<Portfolio>> {
    // Check for duplicate
    const existingResult = await this.findByPortfolioId(request.portfolioId);
    if (existingResult.success) {
      return this.errorResult('Portfolio already exists', 'E002');
    }
    
    // Create new portfolio
    const portfolio: Portfolio = {
      ...createDefaultPortfolio(),
      key: {
        portfolioId: request.portfolioId,
        accountNumber: request.accountNumber,
      },
      clientInfo: {
        clientName: request.clientName,
        clientType: request.clientType,
      },
      createDate: new Date(),
      closeDate: null,
      status: PortfolioStatus.ACTIVE,
      financialInfo: {
        totalValue: request.initialCashBalance ?? 0,
        cashBalance: request.initialCashBalance ?? 0,
        currencyCode: request.currencyCode,
      },
      auditInfo: {
        lastUser: 'SYSTEM',
        lastTransactionDate: '',
        lastMaintenanceDate: new Date(),
      },
      riskLevel: request.riskLevel,
      branchId: request.branchId,
    };
    
    const storageKey = this.getStorageKey(portfolio.key);
    this.storage.set(storageKey, portfolio);
    
    return this.successResult(portfolio);
  }

  /**
   * Update an existing portfolio
   */
  async update(request: UpdatePortfolioRequest): Promise<RepositoryResult<Portfolio>> {
    const existingResult = await this.findByPortfolioId(request.portfolioId);
    if (!existingResult.success || !existingResult.data) {
      return this.errorResult('Portfolio not found', 'E001');
    }
    
    const portfolio = existingResult.data;
    
    // Apply updates
    if (request.clientName !== undefined) {
      portfolio.clientInfo.clientName = request.clientName;
    }
    if (request.status !== undefined) {
      portfolio.status = request.status;
    }
    if (request.riskLevel !== undefined) {
      portfolio.riskLevel = request.riskLevel;
    }
    if (request.cashBalance !== undefined) {
      portfolio.financialInfo.cashBalance = request.cashBalance;
    }
    
    // Update audit info
    portfolio.auditInfo.lastMaintenanceDate = new Date();
    
    const storageKey = this.getStorageKey(portfolio.key);
    this.storage.set(storageKey, portfolio);
    
    return this.successResult(portfolio);
  }

  /**
   * Delete a portfolio
   */
  async delete(key: PortfolioKey): Promise<RepositoryResult<boolean>> {
    const storageKey = this.getStorageKey(key);
    const deleted = this.storage.delete(storageKey);
    
    if (!deleted) {
      return this.errorResult('Portfolio not found', 'E001');
    }
    
    return this.successResult(true);
  }

  /**
   * Check if a portfolio exists
   */
  async exists(key: PortfolioKey): Promise<boolean> {
    const storageKey = this.getStorageKey(key);
    return this.storage.has(storageKey);
  }

  /**
   * Count portfolios matching criteria
   */
  async count(criteria?: PortfolioSearchCriteria): Promise<number> {
    let portfolios = this.storage.getAll();
    
    if (criteria) {
      portfolios = this.applyFilters(portfolios, criteria);
    }
    
    return portfolios.length;
  }

  /**
   * Get portfolio summaries for list views
   */
  async getSummaries(criteria?: PortfolioSearchCriteria): Promise<PortfolioSummary[]> {
    let portfolios = this.storage.getAll();
    
    if (criteria) {
      portfolios = this.applyFilters(portfolios, criteria);
    }
    
    return portfolios.map(p => ({
      portfolioId: p.key.portfolioId,
      accountNumber: p.key.accountNumber,
      clientName: p.clientInfo.clientName,
      status: p.status,
      totalValue: p.financialInfo.totalValue,
      cashBalance: p.financialInfo.cashBalance,
      currencyCode: p.financialInfo.currencyCode,
    }));
  }

  /**
   * Update portfolio total value
   */
  async updateTotalValue(portfolioId: string, totalValue: number): Promise<RepositoryResult<Portfolio>> {
    const existingResult = await this.findByPortfolioId(portfolioId);
    if (!existingResult.success || !existingResult.data) {
      return this.errorResult('Portfolio not found', 'E001');
    }
    
    const portfolio = existingResult.data;
    portfolio.financialInfo.totalValue = totalValue;
    portfolio.auditInfo.lastMaintenanceDate = new Date();
    
    const storageKey = this.getStorageKey(portfolio.key);
    this.storage.set(storageKey, portfolio);
    
    return this.successResult(portfolio);
  }

  /**
   * Update portfolio cash balance
   */
  async updateCashBalance(portfolioId: string, cashBalance: number): Promise<RepositoryResult<Portfolio>> {
    const existingResult = await this.findByPortfolioId(portfolioId);
    if (!existingResult.success || !existingResult.data) {
      return this.errorResult('Portfolio not found', 'E001');
    }
    
    const portfolio = existingResult.data;
    portfolio.financialInfo.cashBalance = cashBalance;
    portfolio.auditInfo.lastMaintenanceDate = new Date();
    
    const storageKey = this.getStorageKey(portfolio.key);
    this.storage.set(storageKey, portfolio);
    
    return this.successResult(portfolio);
  }

  /**
   * Close a portfolio
   */
  async closePortfolio(portfolioId: string): Promise<RepositoryResult<Portfolio>> {
    const existingResult = await this.findByPortfolioId(portfolioId);
    if (!existingResult.success || !existingResult.data) {
      return this.errorResult('Portfolio not found', 'E001');
    }
    
    const portfolio = existingResult.data;
    portfolio.status = PortfolioStatus.CLOSED;
    portfolio.closeDate = new Date();
    portfolio.auditInfo.lastMaintenanceDate = new Date();
    
    const storageKey = this.getStorageKey(portfolio.key);
    this.storage.set(storageKey, portfolio);
    
    return this.successResult(portfolio);
  }

  /**
   * Apply search filters to portfolios
   */
  private applyFilters(portfolios: Portfolio[], criteria: PortfolioSearchCriteria): Portfolio[] {
    return portfolios.filter(p => {
      if (criteria.portfolioId && p.key.portfolioId !== criteria.portfolioId) {
        return false;
      }
      if (criteria.accountNumber && p.key.accountNumber !== criteria.accountNumber) {
        return false;
      }
      if (criteria.clientName && !p.clientInfo.clientName.toLowerCase().includes(criteria.clientName.toLowerCase())) {
        return false;
      }
      if (criteria.clientType && p.clientInfo.clientType !== criteria.clientType) {
        return false;
      }
      if (criteria.status && p.status !== criteria.status) {
        return false;
      }
      if (criteria.branchId && p.branchId !== criteria.branchId) {
        return false;
      }
      if (criteria.createDateFrom && p.createDate < criteria.createDateFrom) {
        return false;
      }
      if (criteria.createDateTo && p.createDate > criteria.createDateTo) {
        return false;
      }
      return true;
    });
  }
}

/**
 * Create a singleton instance of the portfolio repository
 */
let portfolioRepositoryInstance: IPortfolioRepository | null = null;

export function getPortfolioRepository(): IPortfolioRepository {
  if (!portfolioRepositoryInstance) {
    portfolioRepositoryInstance = new InMemoryPortfolioRepository();
  }
  return portfolioRepositoryInstance;
}

/**
 * Reset the portfolio repository (for testing)
 */
export function resetPortfolioRepository(): void {
  portfolioRepositoryInstance = null;
}
