import { HISTORY_FIXTURE } from '../data/history.fixture';
import { PORTFOLIO_FIXTURE } from '../data/portfolios.fixture';
import { historyKey, type HistoryQuery, type HistoryRecord } from '../types/history';
import type { Portfolio } from '../types/portfolio';
import type { HistoryService } from './historyService';

const SIMULATED_LATENCY_MS = 150;

function delay<T>(value: T): Promise<T> {
  return new Promise((resolve) =>
    setTimeout(() => resolve(value), SIMULATED_LATENCY_MS),
  );
}

/** YYYYMMDD strings sort lexicographically, so range checks are plain compares. */
function withinRange(record: HistoryRecord, query: HistoryQuery): boolean {
  if (query.startDate && record.date < query.startDate) {
    return false;
  }
  if (query.endDate && record.date > query.endDate) {
    return false;
  }
  return true;
}

/** Newest first: HIST-DATE, then HIST-TIME, then HIST-SEQ-NO, all descending. */
function byMostRecent(a: HistoryRecord, b: HistoryRecord): number {
  return (
    b.date.localeCompare(a.date) ||
    b.time.localeCompare(a.time) ||
    b.seqNo.localeCompare(a.seqNo)
  );
}

/**
 * In-memory {@link HistoryService} backed by {@link HISTORY_FIXTURE}.
 *
 * Mirrors the INQHIST lookup: an account number is resolved to its
 * portfolio(s) via the portfolio master, then the history rows for those
 * portfolios are returned. This stands in for the POSHIST DB2 cursor until the
 * backend API is connected.
 */
export class MockHistoryService implements HistoryService {
  private readonly history: readonly HistoryRecord[];
  private readonly portfolios: readonly Portfolio[];

  constructor(
    historySeed: readonly HistoryRecord[] = HISTORY_FIXTURE,
    portfolioSeed: readonly Portfolio[] = PORTFOLIO_FIXTURE,
  ) {
    this.history = historySeed;
    this.portfolios = portfolioSeed;
  }

  async listByAccount(
    accountNo: string,
    query: HistoryQuery = {},
  ): Promise<HistoryRecord[]> {
    const account = accountNo.trim().toLowerCase();
    if (!account) {
      return delay([]);
    }

    const portfolioIds = new Set(
      this.portfolios
        .filter((p) => p.accountNo.toLowerCase() === account)
        .map((p) => p.portId),
    );

    const results = this.history
      .filter((record) => portfolioIds.has(record.portfolioId))
      .filter((record) => withinRange(record, query))
      .filter(
        (record) => !query.recordType || record.recordType === query.recordType,
      )
      .sort(byMostRecent)
      .map((record) => ({ ...record }));

    return delay(results);
  }

  async get(recordKey: string): Promise<HistoryRecord | undefined> {
    const found = this.history.find(
      (record) => historyKey(record) === recordKey,
    );
    return delay(found ? { ...found } : undefined);
  }
}
