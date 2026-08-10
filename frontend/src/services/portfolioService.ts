import type {
  Portfolio,
  PortfolioInput,
  PortfolioQuery,
} from '../types/portfolio';

/**
 * Service interface for portfolio operations.
 *
 * This is the swappable API integration point described in the modernization
 * epic. The current implementation ({@link MockPortfolioService}) is backed by
 * an in-memory fixture; a future implementation will call the REST endpoints
 * that front the COBOL programs:
 *
 *   list   -> GET    /api/portfolios          (PORTMSTR)
 *   get    -> GET    /api/portfolios/:id      (PORTREAD)
 *   create -> POST   /api/portfolios          (PORTADD)
 *   update -> PUT    /api/portfolios/:id      (PORTUPDT)
 *   remove -> DELETE /api/portfolios/:id      (PORTDEL)
 */
export interface PortfolioService {
  list(query?: PortfolioQuery): Promise<Portfolio[]>;
  get(portId: string): Promise<Portfolio | undefined>;
  create(input: PortfolioInput): Promise<Portfolio>;
  update(portId: string, input: PortfolioInput): Promise<Portfolio>;
  remove(portId: string): Promise<void>;
}

/** Thrown when an operation references a portfolio ID that does not exist. */
export class PortfolioNotFoundError extends Error {
  constructor(portId: string) {
    super(`Portfolio "${portId}" was not found.`);
    this.name = 'PortfolioNotFoundError';
  }
}

/** Thrown by create() when the PORT-ID key already exists. */
export class DuplicatePortfolioError extends Error {
  constructor(portId: string) {
    super(`Portfolio "${portId}" already exists.`);
    this.name = 'DuplicatePortfolioError';
  }
}
