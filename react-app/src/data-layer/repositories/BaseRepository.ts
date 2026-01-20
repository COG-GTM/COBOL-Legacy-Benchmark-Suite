/**
 * Base Repository Interface
 * Provides common CRUD operations for all data entities
 * 
 * This pattern mirrors the data access patterns from the COBOL programs:
 * - VSAM file operations (READ, WRITE, REWRITE, DELETE)
 * - DB2 SQL operations (SELECT, INSERT, UPDATE, DELETE)
 */

/**
 * Pagination options for list queries
 */
export interface PaginationOptions {
  /** Page number (1-indexed) */
  page: number;
  /** Number of items per page */
  pageSize: number;
}

/**
 * Sort options for list queries
 */
export interface SortOptions<T> {
  /** Field to sort by */
  field: keyof T;
  /** Sort direction */
  direction: 'asc' | 'desc';
}

/**
 * Paginated result
 */
export interface PaginatedResult<T> {
  /** Data items */
  data: T[];
  /** Total count of items matching the query */
  totalCount: number;
  /** Current page number */
  page: number;
  /** Page size */
  pageSize: number;
  /** Total number of pages */
  totalPages: number;
  /** Whether there are more pages */
  hasMore: boolean;
}

/**
 * Repository operation result
 */
export interface RepositoryResult<T> {
  /** Whether the operation was successful */
  success: boolean;
  /** The data returned by the operation */
  data?: T;
  /** Error message if the operation failed */
  error?: string;
  /** Error code if the operation failed */
  errorCode?: string;
}

/**
 * Base repository interface
 * Defines common CRUD operations for all entities
 */
export interface IBaseRepository<T, TKey, TCreateRequest, TUpdateRequest, TSearchCriteria> {
  /**
   * Find an entity by its key
   * Mirrors VSAM READ with key or DB2 SELECT with primary key
   */
  findByKey(key: TKey): Promise<RepositoryResult<T>>;

  /**
   * Find all entities matching the search criteria
   * Mirrors VSAM sequential read or DB2 SELECT with WHERE clause
   */
  findAll(
    criteria?: TSearchCriteria,
    pagination?: PaginationOptions,
    sort?: SortOptions<T>
  ): Promise<PaginatedResult<T>>;

  /**
   * Create a new entity
   * Mirrors VSAM WRITE or DB2 INSERT
   */
  create(request: TCreateRequest): Promise<RepositoryResult<T>>;

  /**
   * Update an existing entity
   * Mirrors VSAM REWRITE or DB2 UPDATE
   */
  update(request: TUpdateRequest): Promise<RepositoryResult<T>>;

  /**
   * Delete an entity by its key
   * Mirrors VSAM DELETE or DB2 DELETE
   */
  delete(key: TKey): Promise<RepositoryResult<boolean>>;

  /**
   * Check if an entity exists
   * Mirrors VSAM READ with key check or DB2 SELECT COUNT
   */
  exists(key: TKey): Promise<boolean>;

  /**
   * Count entities matching the search criteria
   * Mirrors DB2 SELECT COUNT
   */
  count(criteria?: TSearchCriteria): Promise<number>;
}

/**
 * Abstract base repository implementation
 * Provides common functionality for all repositories
 */
export abstract class BaseRepository<T, TKey, TCreateRequest, TUpdateRequest, TSearchCriteria>
  implements IBaseRepository<T, TKey, TCreateRequest, TUpdateRequest, TSearchCriteria> {
  
  protected entityName: string;

  constructor(entityName: string) {
    this.entityName = entityName;
  }

  abstract findByKey(key: TKey): Promise<RepositoryResult<T>>;
  abstract findAll(
    criteria?: TSearchCriteria,
    pagination?: PaginationOptions,
    sort?: SortOptions<T>
  ): Promise<PaginatedResult<T>>;
  abstract create(request: TCreateRequest): Promise<RepositoryResult<T>>;
  abstract update(request: TUpdateRequest): Promise<RepositoryResult<T>>;
  abstract delete(key: TKey): Promise<RepositoryResult<boolean>>;
  abstract exists(key: TKey): Promise<boolean>;
  abstract count(criteria?: TSearchCriteria): Promise<number>;

  /**
   * Create a successful result
   */
  protected successResult<R>(data: R): RepositoryResult<R> {
    return {
      success: true,
      data,
    };
  }

  /**
   * Create an error result
   */
  protected errorResult<R>(error: string, errorCode?: string): RepositoryResult<R> {
    return {
      success: false,
      error,
      errorCode,
    };
  }

  /**
   * Create a paginated result
   */
  protected paginatedResult<R>(
    data: R[],
    totalCount: number,
    page: number,
    pageSize: number
  ): PaginatedResult<R> {
    const totalPages = Math.ceil(totalCount / pageSize);
    return {
      data,
      totalCount,
      page,
      pageSize,
      totalPages,
      hasMore: page < totalPages,
    };
  }

  /**
   * Apply pagination to an array
   */
  protected applyPagination<R>(data: R[], pagination?: PaginationOptions): R[] {
    if (!pagination) {
      return data;
    }
    const start = (pagination.page - 1) * pagination.pageSize;
    const end = start + pagination.pageSize;
    return data.slice(start, end);
  }

  /**
   * Apply sorting to an array
   */
  protected applySort<R>(data: R[], sort?: SortOptions<R>): R[] {
    if (!sort) {
      return data;
    }
    return [...data].sort((a, b) => {
      const aValue = a[sort.field];
      const bValue = b[sort.field];
      
      if (aValue === bValue) return 0;
      if (aValue === null || aValue === undefined) return 1;
      if (bValue === null || bValue === undefined) return -1;
      
      const comparison = aValue < bValue ? -1 : 1;
      return sort.direction === 'asc' ? comparison : -comparison;
    });
  }
}

/**
 * In-memory storage adapter
 * Used for development and testing
 */
export class InMemoryStorage<T> {
  private data: Map<string, T> = new Map();

  get(key: string): T | undefined {
    return this.data.get(key);
  }

  set(key: string, value: T): void {
    this.data.set(key, value);
  }

  delete(key: string): boolean {
    return this.data.delete(key);
  }

  has(key: string): boolean {
    return this.data.has(key);
  }

  getAll(): T[] {
    return Array.from(this.data.values());
  }

  clear(): void {
    this.data.clear();
  }

  size(): number {
    return this.data.size;
  }
}
