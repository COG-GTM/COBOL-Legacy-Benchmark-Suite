import {
  InvestmentPosition,
  PortfolioMaster,
  TransactionRecord,
} from "../types";

/**
 * Data-access contract for the portfolio system. The HTTP layer depends only on
 * this interface, so the underlying store (in-memory seed data now, real DB2
 * later) can be swapped without touching the routes. See
 * `InMemoryPortfolioRepository` and `Db2PortfolioRepository`.
 */
export interface PortfolioRepository {
  /** Resolve a portfolio by its account number (the online inquiry key). */
  findPortfolioByAccountNo(accountNo: string): Promise<PortfolioMaster | null>;

  /** Resolve a portfolio by its PORTFOLIO_ID (DB2 primary key). */
  findPortfolioById(portfolioId: string): Promise<PortfolioMaster | null>;

  /**
   * Current position for a portfolio (latest POSITION_DATE). Mirrors the single
   * READ performed by INQPORT against POSFILE.
   */
  findCurrentPositionByPortfolioId(
    portfolioId: string
  ): Promise<InvestmentPosition | null>;

  /**
   * Transaction history for a portfolio ordered by date descending. Mirrors the
   * cursor SELECT in INQHIST.
   */
  findHistoryByPortfolioId(
    portfolioId: string
  ): Promise<TransactionRecord[]>;
}
