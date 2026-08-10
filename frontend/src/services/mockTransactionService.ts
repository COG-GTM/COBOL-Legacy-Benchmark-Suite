import { PORTFOLIO_FIXTURE } from '../data/portfolios.fixture';
import { POSITION_FIXTURE } from '../data/positions.fixture';
import { TRANSACTION_FIXTURE } from '../data/transactions.fixture';
import type { Portfolio } from '../types/portfolio';
import type { Position } from '../types/position';
import type {
  Transaction,
  TransactionInput,
  TransactionQuery,
} from '../types/transaction';
import {
  compareDecimals,
  subtractDecimals,
  sumDecimals,
} from '../utils/decimal';
import { calculateAmount } from '../features/transactions/amount';
import { todayCobolDate } from '../utils/date';
import {
  InsufficientUnitsError,
  UnknownPortfolioError,
  type TransactionService,
} from './transactionService';

const SIMULATED_LATENCY_MS = 150;
/** Stands in for the signed-on RACF user until authentication is wired up. */
const CURRENT_USER = 'WEBUSER';

function delay<T>(value: T): Promise<T> {
  return new Promise((resolve) =>
    setTimeout(() => resolve(value), SIMULATED_LATENCY_MS),
  );
}

/** Formats a Date as TRN-TIME PIC X(6) — HHMMSS. */
function cobolTime(now: Date): string {
  const hh = now.getHours().toString().padStart(2, '0');
  const mm = now.getMinutes().toString().padStart(2, '0');
  const ss = now.getSeconds().toString().padStart(2, '0');
  return `${hh}${mm}${ss}`;
}

/**
 * In-memory {@link TransactionService} backed by {@link TRANSACTION_FIXTURE}.
 *
 * Mirrors PORTTRAN's front-end behaviour: a submission is validated against the
 * portfolio master (2110-CHECK-PORTFOLIO) and, for a SELL, against the units
 * held (2220-PROCESS-SELL), then written with TRN-STATUS 'P' for the batch run
 * to settle. Submitted records live for the lifetime of the service instance;
 * this stands in for the TRANFILE writes until the backend API is connected.
 */
export class MockTransactionService implements TransactionService {
  private transactions: Transaction[];
  private readonly portfolios: readonly Portfolio[];
  private readonly positions: readonly Position[];

  constructor(
    transactionSeed: readonly Transaction[] = TRANSACTION_FIXTURE,
    portfolioSeed: readonly Portfolio[] = PORTFOLIO_FIXTURE,
    positionSeed: readonly Position[] = POSITION_FIXTURE,
  ) {
    this.transactions = transactionSeed.map((t) => ({ ...t }));
    this.portfolios = portfolioSeed;
    this.positions = positionSeed;
  }

  async list(query: TransactionQuery = {}): Promise<Transaction[]> {
    const portfolioId = query.portfolioId?.trim().toUpperCase() ?? '';

    const results = this.transactions
      .filter((t) => !portfolioId || t.portfolioId === portfolioId)
      .filter((t) => !query.status || t.status === query.status)
      .filter((t) => !query.type || t.type === query.type)
      .sort(
        (a, b) =>
          // Newest first: TRN-KEY is date + time + portfolio + sequence.
          b.date.localeCompare(a.date) ||
          b.time.localeCompare(a.time) ||
          a.portfolioId.localeCompare(b.portfolioId) ||
          b.sequenceNo.localeCompare(a.sequenceNo),
      )
      .map((t) => ({ ...t }));

    return delay(results);
  }

  async availableUnits(
    portfolioId: string,
    investmentId: string,
  ): Promise<string | null> {
    return delay(this.unitsHeld(portfolioId, investmentId));
  }

  async submit(input: TransactionInput): Promise<Transaction> {
    const portfolioId = input.portfolioId.trim().toUpperCase();
    const investmentId = input.investmentId.trim().toUpperCase();

    if (!this.portfolios.some((p) => p.portId === portfolioId)) {
      throw new UnknownPortfolioError(portfolioId);
    }

    if (input.type === 'SL') {
      const available = this.unitsHeld(portfolioId, investmentId) ?? '0';
      if (compareDecimals(available, input.quantity) < 0) {
        throw new InsufficientUnitsError(input.quantity, available);
      }
    }

    const now = new Date();
    const date = todayCobolDate(now);
    const transaction: Transaction = {
      date,
      time: cobolTime(now),
      portfolioId,
      sequenceNo: this.nextSequenceNo(portfolioId, date),
      investmentId,
      type: input.type,
      quantity: input.quantity,
      price: input.price,
      amount: calculateAmount(input.type, input.quantity, input.price),
      currency: input.currency,
      // Written as pending; the PORTTRAN batch run settles it to D or F.
      status: 'P',
      processDate: '',
      processUser: CURRENT_USER,
    };

    this.transactions = [...this.transactions, transaction];
    return delay({ ...transaction });
  }

  /**
   * Units available to sell: the position quantity held for the pair, less any
   * SELL still pending settlement, so two successive sells cannot oversell the
   * same holding.
   */
  private unitsHeld(portfolioId: string, investmentId: string): string | null {
    const portId = portfolioId.trim().toUpperCase();
    const invId = investmentId.trim().toUpperCase();

    const holdings = this.positions.filter(
      (p) => p.portfolioId === portId && p.investmentId === invId,
    );
    if (holdings.length === 0) {
      return null;
    }

    const held = sumDecimals(
      holdings.map((p) => p.quantity),
      4,
    );
    const pendingSells = sumDecimals(
      this.transactions
        .filter(
          (t) =>
            t.portfolioId === portId &&
            t.investmentId === invId &&
            t.type === 'SL' &&
            t.status === 'P',
        )
        .map((t) => t.quantity),
      4,
    );
    return subtractDecimals(held, pendingSells, 4);
  }

  private nextSequenceNo(portfolioId: string, date: string): string {
    const used = this.transactions
      .filter((t) => t.portfolioId === portfolioId && t.date === date)
      .map((t) => Number.parseInt(t.sequenceNo, 10))
      .filter((n) => Number.isFinite(n));
    const next = used.length === 0 ? 1 : Math.max(...used) + 1;
    return next.toString().padStart(6, '0');
  }
}
