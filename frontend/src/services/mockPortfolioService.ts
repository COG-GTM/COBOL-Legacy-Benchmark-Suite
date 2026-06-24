import { PORTFOLIO_FIXTURE } from '../data/portfolios.fixture';
import type {
  Portfolio,
  PortfolioInput,
  PortfolioQuery,
} from '../types/portfolio';
import { normalizeDecimal } from '../utils/decimal';
import { todayCobolDate } from '../utils/date';
import {
  DuplicatePortfolioError,
  PortfolioNotFoundError,
  type PortfolioService,
} from './portfolioService';

const SIMULATED_LATENCY_MS = 150;

function delay<T>(value: T): Promise<T> {
  return new Promise((resolve) =>
    setTimeout(() => resolve(value), SIMULATED_LATENCY_MS),
  );
}

function matchesQuery(portfolio: Portfolio, query: PortfolioQuery): boolean {
  const account = query.accountNo?.trim().toLowerCase();
  if (account && !portfolio.accountNo.toLowerCase().includes(account)) {
    return false;
  }
  const name = query.clientName?.trim().toLowerCase();
  if (name && !portfolio.clientName.toLowerCase().includes(name)) {
    return false;
  }
  if (query.status && portfolio.status !== query.status) {
    return false;
  }
  return true;
}

/**
 * In-memory {@link PortfolioService} backed by {@link PORTFOLIO_FIXTURE}.
 *
 * Audit fields (createDate, lastMaintDate, lastUser, lastTransId) are stamped
 * by the service, mirroring how the COBOL PORTADD / PORTUPDT programs maintain
 * the PORT-AUDIT-INFO group.
 */
export class MockPortfolioService implements PortfolioService {
  private store: Map<string, Portfolio>;
  private nextTransSeq: number;

  constructor(
    seed: readonly Portfolio[] = PORTFOLIO_FIXTURE,
    private readonly currentUser = 'WEBUSER',
  ) {
    this.store = new Map(seed.map((p) => [p.portId, { ...p }]));
    const maxTrans = seed.reduce(
      (max, p) => Math.max(max, Number(p.lastTransId) || 0),
      0,
    );
    this.nextTransSeq = maxTrans + 1;
  }

  async list(query: PortfolioQuery = {}): Promise<Portfolio[]> {
    const results = [...this.store.values()]
      .filter((p) => matchesQuery(p, query))
      .sort((a, b) => a.portId.localeCompare(b.portId))
      .map((p) => ({ ...p }));
    return delay(results);
  }

  async get(portId: string): Promise<Portfolio | undefined> {
    const found = this.store.get(portId);
    return delay(found ? { ...found } : undefined);
  }

  async create(input: PortfolioInput): Promise<Portfolio> {
    const portId = input.portId.trim();
    if (this.store.has(portId)) {
      throw new DuplicatePortfolioError(portId);
    }
    const today = todayCobolDate();
    const portfolio: Portfolio = {
      ...this.normalizeInput(input),
      portId,
      createDate: today,
      lastMaintDate: today,
      lastUser: this.currentUser,
      lastTransId: this.takeTransId(),
    };
    this.store.set(portId, portfolio);
    return delay({ ...portfolio });
  }

  async update(portId: string, input: PortfolioInput): Promise<Portfolio> {
    const existing = this.store.get(portId);
    if (!existing) {
      throw new PortfolioNotFoundError(portId);
    }
    const portfolio: Portfolio = {
      ...existing,
      ...this.normalizeInput(input),
      portId: existing.portId,
      createDate: existing.createDate,
      lastMaintDate: todayCobolDate(),
      lastUser: this.currentUser,
      lastTransId: this.takeTransId(),
    };
    this.store.set(portId, portfolio);
    return delay({ ...portfolio });
  }

  async remove(portId: string): Promise<void> {
    if (!this.store.has(portId)) {
      throw new PortfolioNotFoundError(portId);
    }
    this.store.delete(portId);
    return delay(undefined);
  }

  private normalizeInput(input: PortfolioInput) {
    return {
      accountNo: input.accountNo.trim(),
      clientName: input.clientName.trim(),
      clientType: input.clientType,
      status: input.status,
      totalValue: normalizeDecimal(input.totalValue, 2),
      cashBalance: normalizeDecimal(input.cashBalance, 2),
    };
  }

  private takeTransId(): string {
    const id = this.nextTransSeq++;
    return id.toString().padStart(8, '0');
  }
}
