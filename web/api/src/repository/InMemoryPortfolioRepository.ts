import {
  INVESTMENT_POSITIONS,
  PORTFOLIO_MASTER,
  TRANSACTION_HISTORY,
} from "../data/seed";
import {
  InvestmentPosition,
  PortfolioMaster,
  TransactionRecord,
} from "../types";
import { PortfolioRepository } from "./PortfolioRepository";

/**
 * In-memory implementation backed by the seed data. Behaves like the VSAM/DB2
 * reads performed by the COBOL online programs but requires no mainframe
 * runtime. Trimming of the account key mirrors the fixed-width CHAR/PIC X
 * fields, whose values are space padded on the mainframe.
 */
export class InMemoryPortfolioRepository implements PortfolioRepository {
  async findPortfolioByAccountNo(
    accountNo: string
  ): Promise<PortfolioMaster | null> {
    const key = accountNo.trim();
    return (
      PORTFOLIO_MASTER.find((p) => p.accountNo.trim() === key) ?? null
    );
  }

  async findPortfolioById(
    portfolioId: string
  ): Promise<PortfolioMaster | null> {
    const key = portfolioId.trim().toUpperCase();
    return (
      PORTFOLIO_MASTER.find((p) => p.portfolioId.trim().toUpperCase() === key) ??
      null
    );
  }

  async findCurrentPositionByPortfolioId(
    portfolioId: string
  ): Promise<InvestmentPosition | null> {
    const key = portfolioId.trim().toUpperCase();
    const positions = INVESTMENT_POSITIONS.filter(
      (pos) => pos.portfolioId.trim().toUpperCase() === key
    ).sort((a, b) => b.positionDate.localeCompare(a.positionDate));
    return positions[0] ?? null;
  }

  async findHistoryByPortfolioId(
    portfolioId: string
  ): Promise<TransactionRecord[]> {
    const key = portfolioId.trim().toUpperCase();
    return TRANSACTION_HISTORY.filter(
      (t) => t.portfolioId.trim().toUpperCase() === key
    ).sort((a, b) => {
      const dateCmp = b.transactionDate.localeCompare(a.transactionDate);
      return dateCmp !== 0
        ? dateCmp
        : b.transactionTime.localeCompare(a.transactionTime);
    });
  }
}
