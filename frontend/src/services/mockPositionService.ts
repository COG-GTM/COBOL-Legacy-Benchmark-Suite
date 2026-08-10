import { PORTFOLIO_FIXTURE } from '../data/portfolios.fixture';
import { POSITION_FIXTURE } from '../data/positions.fixture';
import type { Portfolio } from '../types/portfolio';
import type { Position, PositionQuery } from '../types/position';
import type { PositionService } from './positionService';

const SIMULATED_LATENCY_MS = 150;

function delay<T>(value: T): Promise<T> {
  return new Promise((resolve) =>
    setTimeout(() => resolve(value), SIMULATED_LATENCY_MS),
  );
}

/**
 * In-memory {@link PositionService} backed by {@link POSITION_FIXTURE}.
 *
 * Mirrors the INQPORT lookup: an account number is resolved to its portfolio(s)
 * via the portfolio master, then the positions for those portfolios are
 * returned. This stands in for the POSFILE VSAM reads until the backend API is
 * connected.
 */
export class MockPositionService implements PositionService {
  private readonly positions: readonly Position[];
  private readonly portfolios: readonly Portfolio[];

  constructor(
    positionSeed: readonly Position[] = POSITION_FIXTURE,
    portfolioSeed: readonly Portfolio[] = PORTFOLIO_FIXTURE,
  ) {
    this.positions = positionSeed;
    this.portfolios = portfolioSeed;
  }

  async listByAccount(
    accountNo: string,
    query: PositionQuery = {},
  ): Promise<Position[]> {
    const account = accountNo.trim().toLowerCase();
    if (!account) {
      return delay([]);
    }

    const portfolioIds = new Set(
      this.portfolios
        .filter((p) => p.accountNo.toLowerCase() === account)
        .map((p) => p.portId),
    );

    const results = this.positions
      .filter((position) => portfolioIds.has(position.portfolioId))
      .filter((position) => !query.status || position.status === query.status)
      .sort(
        (a, b) =>
          a.portfolioId.localeCompare(b.portfolioId) ||
          a.investmentId.localeCompare(b.investmentId),
      )
      .map((position) => ({ ...position }));

    return delay(results);
  }
}
