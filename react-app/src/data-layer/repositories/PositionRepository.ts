/**
 * Position Repository
 * Provides data access operations for Position entities
 * 
 * Mirrors data access patterns from:
 * - VSAM POSHIST file operations
 * - DB2 INVESTMENT_POSITIONS table operations
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
  Position,
  PositionKey,
  CreatePositionRequest,
  UpdatePositionRequest,
  PositionSearchCriteria,
  PositionSummary,
  PortfolioPositionsAggregate,
  createDefaultPosition,
  calculatePositionMetrics,
} from '../models/Position';
import { PositionStatus, CurrencyCode } from '../types';

/**
 * Position Repository Interface
 */
export interface IPositionRepository {
  findByKey(key: PositionKey): Promise<RepositoryResult<Position>>;
  findByPortfolioId(portfolioId: string): Promise<RepositoryResult<Position[]>>;
  findByInvestmentId(investmentId: string): Promise<RepositoryResult<Position[]>>;
  findAll(
    criteria?: PositionSearchCriteria,
    pagination?: PaginationOptions,
    sort?: SortOptions<Position>
  ): Promise<PaginatedResult<Position>>;
  findActivePositions(portfolioId: string): Promise<Position[]>;
  findCurrentPositions(portfolioId: string, positionDate: string): Promise<Position[]>;
  create(request: CreatePositionRequest): Promise<RepositoryResult<Position>>;
  update(request: UpdatePositionRequest): Promise<RepositoryResult<Position>>;
  delete(key: PositionKey): Promise<RepositoryResult<boolean>>;
  exists(key: PositionKey): Promise<boolean>;
  count(criteria?: PositionSearchCriteria): Promise<number>;
  getSummaries(portfolioId: string): Promise<PositionSummary[]>;
  getPortfolioAggregate(portfolioId: string): Promise<PortfolioPositionsAggregate>;
  updateQuantity(key: PositionKey, quantity: number, costBasis: number): Promise<RepositoryResult<Position>>;
  updateMarketValue(key: PositionKey, marketValue: number): Promise<RepositoryResult<Position>>;
  closePosition(key: PositionKey): Promise<RepositoryResult<Position>>;
}

/**
 * In-memory Position Repository implementation
 */
export class InMemoryPositionRepository
  extends BaseRepository<Position, PositionKey, CreatePositionRequest, UpdatePositionRequest, PositionSearchCriteria>
  implements IPositionRepository {
  
  private storage: InMemoryStorage<Position>;

  constructor() {
    super('Position');
    this.storage = new InMemoryStorage<Position>();
  }

  private getStorageKey(key: PositionKey): string {
    return `${key.portfolioId}-${key.positionDate}-${key.investmentId}`;
  }

  async findByKey(key: PositionKey): Promise<RepositoryResult<Position>> {
    const storageKey = this.getStorageKey(key);
    const position = this.storage.get(storageKey);
    
    if (!position) {
      return this.errorResult('Position not found', 'E001');
    }
    
    return this.successResult(position);
  }

  async findByPortfolioId(portfolioId: string): Promise<RepositoryResult<Position[]>> {
    const positions = this.storage.getAll();
    const filtered = positions.filter(p => p.key.portfolioId === portfolioId);
    return this.successResult(filtered);
  }

  async findByInvestmentId(investmentId: string): Promise<RepositoryResult<Position[]>> {
    const positions = this.storage.getAll();
    const filtered = positions.filter(p => p.key.investmentId === investmentId);
    return this.successResult(filtered);
  }

  async findAll(
    criteria?: PositionSearchCriteria,
    pagination?: PaginationOptions,
    sort?: SortOptions<Position>
  ): Promise<PaginatedResult<Position>> {
    let positions = this.storage.getAll();
    
    if (criteria) {
      positions = this.applyFilters(positions, criteria);
    }
    
    const totalCount = positions.length;
    positions = this.applySort(positions, sort);
    
    const page = pagination?.page ?? 1;
    const pageSize = pagination?.pageSize ?? 10;
    positions = this.applyPagination(positions, pagination);
    
    return this.paginatedResult(positions, totalCount, page, pageSize);
  }

  async findActivePositions(portfolioId: string): Promise<Position[]> {
    const positions = this.storage.getAll();
    return positions.filter(p => 
      p.key.portfolioId === portfolioId &&
      p.data.status === PositionStatus.ACTIVE
    );
  }

  async findCurrentPositions(portfolioId: string, positionDate: string): Promise<Position[]> {
    const positions = this.storage.getAll();
    return positions.filter(p => 
      p.key.portfolioId === portfolioId &&
      p.key.positionDate === positionDate &&
      p.data.status === PositionStatus.ACTIVE
    );
  }

  async create(request: CreatePositionRequest): Promise<RepositoryResult<Position>> {
    const today = new Date();
    const dateStr = today.toISOString().slice(0, 10).replace(/-/g, '');
    
    const key: PositionKey = {
      portfolioId: request.portfolioId,
      positionDate: dateStr,
      investmentId: request.investmentId,
    };
    
    const existingResult = await this.findByKey(key);
    if (existingResult.success) {
      return this.errorResult('Position already exists', 'E002');
    }
    
    const position: Position = {
      key,
      data: {
        quantity: request.quantity,
        costBasis: request.costBasis,
        marketValue: request.marketValue,
        currencyCode: request.currencyCode,
        status: PositionStatus.ACTIVE,
      },
      auditInfo: {
        lastMaintenanceDate: new Date(),
        lastMaintenanceUser: 'SYSTEM',
      },
    };
    
    const storageKey = this.getStorageKey(key);
    this.storage.set(storageKey, position);
    
    return this.successResult(position);
  }

  async update(request: UpdatePositionRequest): Promise<RepositoryResult<Position>> {
    const key: PositionKey = {
      portfolioId: request.portfolioId,
      positionDate: request.positionDate,
      investmentId: request.investmentId,
    };
    
    const existingResult = await this.findByKey(key);
    if (!existingResult.success || !existingResult.data) {
      return this.errorResult('Position not found', 'E001');
    }
    
    const position = existingResult.data;
    
    if (request.quantity !== undefined) {
      position.data.quantity = request.quantity;
    }
    if (request.costBasis !== undefined) {
      position.data.costBasis = request.costBasis;
    }
    if (request.marketValue !== undefined) {
      position.data.marketValue = request.marketValue;
    }
    if (request.status !== undefined) {
      position.data.status = request.status;
    }
    
    position.auditInfo.lastMaintenanceDate = new Date();
    
    const storageKey = this.getStorageKey(key);
    this.storage.set(storageKey, position);
    
    return this.successResult(position);
  }

  async delete(key: PositionKey): Promise<RepositoryResult<boolean>> {
    const storageKey = this.getStorageKey(key);
    const deleted = this.storage.delete(storageKey);
    
    if (!deleted) {
      return this.errorResult('Position not found', 'E001');
    }
    
    return this.successResult(true);
  }

  async exists(key: PositionKey): Promise<boolean> {
    const storageKey = this.getStorageKey(key);
    return this.storage.has(storageKey);
  }

  async count(criteria?: PositionSearchCriteria): Promise<number> {
    let positions = this.storage.getAll();
    
    if (criteria) {
      positions = this.applyFilters(positions, criteria);
    }
    
    return positions.length;
  }

  async getSummaries(portfolioId: string): Promise<PositionSummary[]> {
    const positions = this.storage.getAll();
    const filtered = positions.filter(p => p.key.portfolioId === portfolioId);
    
    return filtered.map(p => {
      const metrics = calculatePositionMetrics(p);
      return {
        portfolioId: p.key.portfolioId,
        investmentId: p.key.investmentId,
        positionDate: p.key.positionDate,
        quantity: p.data.quantity,
        marketValue: p.data.marketValue,
        unrealizedGainLoss: metrics.unrealizedGainLoss,
        status: p.data.status,
      };
    });
  }

  async getPortfolioAggregate(portfolioId: string): Promise<PortfolioPositionsAggregate> {
    const summaries = await this.getSummaries(portfolioId);
    
    const totalMarketValue = summaries.reduce((sum, p) => sum + p.marketValue, 0);
    const positions = this.storage.getAll().filter(p => p.key.portfolioId === portfolioId);
    const totalCostBasis = positions.reduce((sum, p) => sum + p.data.costBasis, 0);
    const totalUnrealizedGainLoss = totalMarketValue - totalCostBasis;
    
    return {
      portfolioId,
      totalPositions: summaries.length,
      totalMarketValue,
      totalCostBasis,
      totalUnrealizedGainLoss,
      positions: summaries,
    };
  }

  async updateQuantity(key: PositionKey, quantity: number, costBasis: number): Promise<RepositoryResult<Position>> {
    return this.update({
      portfolioId: key.portfolioId,
      investmentId: key.investmentId,
      positionDate: key.positionDate,
      quantity,
      costBasis,
    });
  }

  async updateMarketValue(key: PositionKey, marketValue: number): Promise<RepositoryResult<Position>> {
    return this.update({
      portfolioId: key.portfolioId,
      investmentId: key.investmentId,
      positionDate: key.positionDate,
      marketValue,
    });
  }

  async closePosition(key: PositionKey): Promise<RepositoryResult<Position>> {
    return this.update({
      portfolioId: key.portfolioId,
      investmentId: key.investmentId,
      positionDate: key.positionDate,
      status: PositionStatus.CLOSED,
    });
  }

  private applyFilters(positions: Position[], criteria: PositionSearchCriteria): Position[] {
    return positions.filter(p => {
      if (criteria.portfolioId && p.key.portfolioId !== criteria.portfolioId) {
        return false;
      }
      if (criteria.investmentId && p.key.investmentId !== criteria.investmentId) {
        return false;
      }
      if (criteria.positionDateFrom && p.key.positionDate < criteria.positionDateFrom) {
        return false;
      }
      if (criteria.positionDateTo && p.key.positionDate > criteria.positionDateTo) {
        return false;
      }
      if (criteria.status && p.data.status !== criteria.status) {
        return false;
      }
      if (criteria.currencyCode && p.data.currencyCode !== criteria.currencyCode) {
        return false;
      }
      if (criteria.minQuantity !== undefined && p.data.quantity < criteria.minQuantity) {
        return false;
      }
      if (criteria.maxQuantity !== undefined && p.data.quantity > criteria.maxQuantity) {
        return false;
      }
      if (criteria.minMarketValue !== undefined && p.data.marketValue < criteria.minMarketValue) {
        return false;
      }
      if (criteria.maxMarketValue !== undefined && p.data.marketValue > criteria.maxMarketValue) {
        return false;
      }
      return true;
    });
  }
}

let positionRepositoryInstance: IPositionRepository | null = null;

export function getPositionRepository(): IPositionRepository {
  if (!positionRepositoryInstance) {
    positionRepositoryInstance = new InMemoryPositionRepository();
  }
  return positionRepositoryInstance;
}

export function resetPositionRepository(): void {
  positionRepositoryInstance = null;
}
