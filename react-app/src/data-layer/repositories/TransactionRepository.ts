/**
 * Transaction Repository
 * Provides data access operations for Transaction entities
 * 
 * Mirrors data access patterns from:
 * - VSAM TRANHIST file operations
 * - DB2 TRANSACTION_HISTORY table operations
 * - INQHIST online program queries
 * - TRNVAL00 batch validation program
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
  Transaction,
  TransactionKey,
  TransactionWithId,
  CreateTransactionRequest,
  UpdateTransactionRequest,
  TransactionSearchCriteria,
  TransactionSummary,
  TransactionHistoryPage,
  createDefaultTransaction,
  generateTransactionId,
  calculateTransactionAmount,
} from '../models/Transaction';
import { TransactionType, TransactionStatus, CurrencyCode } from '../types';

/**
 * Transaction Repository Interface
 */
export interface ITransactionRepository {
  findByKey(key: TransactionKey): Promise<RepositoryResult<Transaction>>;
  findByTransactionId(transactionId: string): Promise<RepositoryResult<TransactionWithId>>;
  findByPortfolioId(portfolioId: string): Promise<RepositoryResult<Transaction[]>>;
  findByInvestmentId(investmentId: string): Promise<RepositoryResult<Transaction[]>>;
  findAll(
    criteria?: TransactionSearchCriteria,
    pagination?: PaginationOptions,
    sort?: SortOptions<Transaction>
  ): Promise<PaginatedResult<Transaction>>;
  findByDateRange(
    portfolioId: string,
    startDate: string,
    endDate: string,
    pagination?: PaginationOptions
  ): Promise<TransactionHistoryPage>;
  findPendingTransactions(): Promise<Transaction[]>;
  create(request: CreateTransactionRequest): Promise<RepositoryResult<TransactionWithId>>;
  update(request: UpdateTransactionRequest): Promise<RepositoryResult<Transaction>>;
  delete(key: TransactionKey): Promise<RepositoryResult<boolean>>;
  exists(key: TransactionKey): Promise<boolean>;
  count(criteria?: TransactionSearchCriteria): Promise<number>;
  getSummaries(portfolioId: string, limit?: number): Promise<TransactionSummary[]>;
  markAsProcessed(transactionId: string): Promise<RepositoryResult<Transaction>>;
  markAsFailed(transactionId: string, reason: string): Promise<RepositoryResult<Transaction>>;
  reverseTransaction(transactionId: string): Promise<RepositoryResult<Transaction>>;
}

/**
 * In-memory Transaction Repository implementation
 */
export class InMemoryTransactionRepository
  extends BaseRepository<Transaction, TransactionKey, CreateTransactionRequest, UpdateTransactionRequest, TransactionSearchCriteria>
  implements ITransactionRepository {
  
  private storage: InMemoryStorage<TransactionWithId>;
  private sequenceCounter: number = 0;

  constructor() {
    super('Transaction');
    this.storage = new InMemoryStorage<TransactionWithId>();
  }

  private getStorageKey(key: TransactionKey): string {
    return `${key.transactionDate}-${key.transactionTime}-${key.portfolioId}-${key.sequenceNumber}`;
  }

  private getStorageKeyFromId(transactionId: string): string | null {
    const transactions = this.storage.getAll();
    const transaction = transactions.find(t => t.transactionId === transactionId);
    return transaction ? this.getStorageKey(transaction.key) : null;
  }

  async findByKey(key: TransactionKey): Promise<RepositoryResult<Transaction>> {
    const storageKey = this.getStorageKey(key);
    const transaction = this.storage.get(storageKey);
    
    if (!transaction) {
      return this.errorResult('Transaction not found', 'E001');
    }
    
    return this.successResult(transaction);
  }

  async findByTransactionId(transactionId: string): Promise<RepositoryResult<TransactionWithId>> {
    const transactions = this.storage.getAll();
    const transaction = transactions.find(t => t.transactionId === transactionId);
    
    if (!transaction) {
      return this.errorResult('Transaction not found', 'E001');
    }
    
    return this.successResult(transaction);
  }

  async findByPortfolioId(portfolioId: string): Promise<RepositoryResult<Transaction[]>> {
    const transactions = this.storage.getAll();
    const filtered = transactions.filter(t => t.key.portfolioId === portfolioId);
    return this.successResult(filtered);
  }

  async findByInvestmentId(investmentId: string): Promise<RepositoryResult<Transaction[]>> {
    const transactions = this.storage.getAll();
    const filtered = transactions.filter(t => t.data.investmentId === investmentId);
    return this.successResult(filtered);
  }

  async findAll(
    criteria?: TransactionSearchCriteria,
    pagination?: PaginationOptions,
    sort?: SortOptions<Transaction>
  ): Promise<PaginatedResult<Transaction>> {
    let transactions = this.storage.getAll();
    
    if (criteria) {
      transactions = this.applyFilters(transactions, criteria);
    }
    
    const totalCount = transactions.length;
    transactions = this.applySort(transactions, sort);
    
    const page = pagination?.page ?? 1;
    const pageSize = pagination?.pageSize ?? 10;
    transactions = this.applyPagination(transactions, pagination);
    
    return this.paginatedResult(transactions, totalCount, page, pageSize);
  }

  async findByDateRange(
    portfolioId: string,
    startDate: string,
    endDate: string,
    pagination?: PaginationOptions
  ): Promise<TransactionHistoryPage> {
    let transactions = this.storage.getAll();
    
    transactions = transactions.filter(t => 
      t.key.portfolioId === portfolioId &&
      t.key.transactionDate >= startDate &&
      t.key.transactionDate <= endDate
    );
    
    // Sort by date descending (most recent first)
    transactions.sort((a, b) => {
      const dateCompare = b.key.transactionDate.localeCompare(a.key.transactionDate);
      if (dateCompare !== 0) return dateCompare;
      return b.key.transactionTime.localeCompare(a.key.transactionTime);
    });
    
    const totalCount = transactions.length;
    const page = pagination?.page ?? 1;
    const pageSize = pagination?.pageSize ?? 10;
    
    transactions = this.applyPagination(transactions, pagination);
    
    const summaries: TransactionSummary[] = transactions.map(t => ({
      transactionId: t.transactionId,
      portfolioId: t.key.portfolioId,
      investmentId: t.data.investmentId,
      transactionDate: t.key.transactionDate,
      transactionType: t.data.transactionType,
      quantity: t.data.quantity,
      price: t.data.price,
      amount: t.data.amount,
      status: t.data.status,
    }));
    
    return {
      transactions: summaries,
      totalCount,
      pageNumber: page,
      pageSize,
      hasMore: page * pageSize < totalCount,
    };
  }

  async findPendingTransactions(): Promise<Transaction[]> {
    const transactions = this.storage.getAll();
    return transactions.filter(t => t.data.status === TransactionStatus.PENDING);
  }

  async create(request: CreateTransactionRequest): Promise<RepositoryResult<TransactionWithId>> {
    this.sequenceCounter++;
    const transactionId = generateTransactionId(this.sequenceCounter);
    
    const now = new Date();
    const dateStr = now.toISOString().slice(0, 10).replace(/-/g, '');
    const timeStr = now.toISOString().slice(11, 19).replace(/:/g, '');
    
    const amount = calculateTransactionAmount(
      request.quantity,
      request.price,
      request.transactionType
    );
    
    const transaction: TransactionWithId = {
      transactionId,
      key: {
        transactionDate: dateStr,
        transactionTime: timeStr,
        portfolioId: request.portfolioId,
        sequenceNumber: this.sequenceCounter.toString().padStart(6, '0'),
      },
      data: {
        investmentId: request.investmentId,
        transactionType: request.transactionType,
        quantity: request.quantity,
        price: request.price,
        amount,
        currencyCode: request.currencyCode,
        status: TransactionStatus.PENDING,
      },
      auditInfo: {
        processDate: now,
        processUser: 'SYSTEM',
      },
    };
    
    const storageKey = this.getStorageKey(transaction.key);
    this.storage.set(storageKey, transaction);
    
    return this.successResult(transaction);
  }

  async update(request: UpdateTransactionRequest): Promise<RepositoryResult<Transaction>> {
    const existingResult = await this.findByTransactionId(request.transactionId);
    if (!existingResult.success || !existingResult.data) {
      return this.errorResult('Transaction not found', 'E001');
    }
    
    const transaction = existingResult.data;
    transaction.data.status = request.status;
    transaction.auditInfo.processDate = new Date();
    
    const storageKey = this.getStorageKey(transaction.key);
    this.storage.set(storageKey, transaction);
    
    return this.successResult(transaction);
  }

  async delete(key: TransactionKey): Promise<RepositoryResult<boolean>> {
    const storageKey = this.getStorageKey(key);
    const deleted = this.storage.delete(storageKey);
    
    if (!deleted) {
      return this.errorResult('Transaction not found', 'E001');
    }
    
    return this.successResult(true);
  }

  async exists(key: TransactionKey): Promise<boolean> {
    const storageKey = this.getStorageKey(key);
    return this.storage.has(storageKey);
  }

  async count(criteria?: TransactionSearchCriteria): Promise<number> {
    let transactions = this.storage.getAll();
    
    if (criteria) {
      transactions = this.applyFilters(transactions, criteria);
    }
    
    return transactions.length;
  }

  async getSummaries(portfolioId: string, limit?: number): Promise<TransactionSummary[]> {
    let transactions = this.storage.getAll();
    transactions = transactions.filter(t => t.key.portfolioId === portfolioId);
    
    // Sort by date descending
    transactions.sort((a, b) => {
      const dateCompare = b.key.transactionDate.localeCompare(a.key.transactionDate);
      if (dateCompare !== 0) return dateCompare;
      return b.key.transactionTime.localeCompare(a.key.transactionTime);
    });
    
    if (limit) {
      transactions = transactions.slice(0, limit);
    }
    
    return transactions.map(t => ({
      transactionId: t.transactionId,
      portfolioId: t.key.portfolioId,
      investmentId: t.data.investmentId,
      transactionDate: t.key.transactionDate,
      transactionType: t.data.transactionType,
      quantity: t.data.quantity,
      price: t.data.price,
      amount: t.data.amount,
      status: t.data.status,
    }));
  }

  async markAsProcessed(transactionId: string): Promise<RepositoryResult<Transaction>> {
    return this.update({
      transactionId,
      status: TransactionStatus.DONE,
    });
  }

  async markAsFailed(transactionId: string, reason: string): Promise<RepositoryResult<Transaction>> {
    return this.update({
      transactionId,
      status: TransactionStatus.FAILED,
    });
  }

  async reverseTransaction(transactionId: string): Promise<RepositoryResult<Transaction>> {
    return this.update({
      transactionId,
      status: TransactionStatus.REVERSED,
    });
  }

  private applyFilters(transactions: TransactionWithId[], criteria: TransactionSearchCriteria): TransactionWithId[] {
    return transactions.filter(t => {
      if (criteria.portfolioId && t.key.portfolioId !== criteria.portfolioId) {
        return false;
      }
      if (criteria.investmentId && t.data.investmentId !== criteria.investmentId) {
        return false;
      }
      if (criteria.transactionType && t.data.transactionType !== criteria.transactionType) {
        return false;
      }
      if (criteria.status && t.data.status !== criteria.status) {
        return false;
      }
      if (criteria.transactionDateFrom && t.key.transactionDate < criteria.transactionDateFrom) {
        return false;
      }
      if (criteria.transactionDateTo && t.key.transactionDate > criteria.transactionDateTo) {
        return false;
      }
      if (criteria.minAmount !== undefined && Math.abs(t.data.amount) < criteria.minAmount) {
        return false;
      }
      if (criteria.maxAmount !== undefined && Math.abs(t.data.amount) > criteria.maxAmount) {
        return false;
      }
      if (criteria.currencyCode && t.data.currencyCode !== criteria.currencyCode) {
        return false;
      }
      return true;
    });
  }
}

let transactionRepositoryInstance: ITransactionRepository | null = null;

export function getTransactionRepository(): ITransactionRepository {
  if (!transactionRepositoryInstance) {
    transactionRepositoryInstance = new InMemoryTransactionRepository();
  }
  return transactionRepositoryInstance;
}

export function resetTransactionRepository(): void {
  transactionRepositoryInstance = null;
}
