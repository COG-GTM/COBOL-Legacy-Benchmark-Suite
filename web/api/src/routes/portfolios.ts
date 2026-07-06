import { Router } from "express";
import { PortfolioRepository } from "../repository";
import {
  HistoryResponse,
  PositionResponse,
} from "../types";

/**
 * Portfolio inquiry routes. These mirror the COBOL online transactions:
 *   - GET /:accountNo/position  -> INQPORT / POSMAP
 *   - GET /:accountNo/history   -> INQHIST / HISMAP
 *   - GET /:portfolioId         -> PORTFOLIO_MASTER master record
 *
 * The "not found" responses map to the COBOL not-found paths
 * (INQPORT P900-NOT-FOUND "Position not found for account").
 */
export function createPortfolioRouter(repo: PortfolioRepository): Router {
  const router = Router();

  // GET /api/portfolios/:portfolioId  -> portfolio master fields
  router.get("/:portfolioId", async (req, res, next) => {
    try {
      const portfolio = await repo.findPortfolioById(req.params.portfolioId);
      if (!portfolio) {
        return res.status(404).json({
          error: "PORTFOLIO_NOT_FOUND",
          message: `Portfolio not found: ${req.params.portfolioId}`,
        });
      }
      return res.json(portfolio);
    } catch (err) {
      return next(err);
    }
  });

  // GET /api/portfolios/:accountNo/position  -> INQPORT / POSMAP
  router.get("/:accountNo/position", async (req, res, next) => {
    try {
      const { accountNo } = req.params;
      const portfolio = await repo.findPortfolioByAccountNo(accountNo);
      if (!portfolio) {
        return res.status(404).json({
          error: "POSITION_NOT_FOUND",
          message: `Position not found for account ${accountNo}`,
        });
      }

      const position = await repo.findCurrentPositionByPortfolioId(
        portfolio.portfolioId
      );
      if (!position) {
        return res.status(404).json({
          error: "POSITION_NOT_FOUND",
          message: `Position not found for account ${accountNo}`,
        });
      }

      const body: PositionResponse = {
        accountNo: portfolio.accountNo,
        portfolioId: portfolio.portfolioId,
        fundId: position.investmentId,
        fundName: position.investmentName,
        units: position.quantity,
        costBasis: position.costBasis,
        marketValue: position.marketValue,
        currencyCode: position.currencyCode,
        positionDate: position.positionDate,
      };
      return res.json(body);
    } catch (err) {
      return next(err);
    }
  });

  // GET /api/portfolios/:accountNo/history  -> INQHIST / HISMAP
  router.get("/:accountNo/history", async (req, res, next) => {
    try {
      const { accountNo } = req.params;
      const portfolio = await repo.findPortfolioByAccountNo(accountNo);
      if (!portfolio) {
        return res.status(404).json({
          error: "HISTORY_NOT_FOUND",
          message: `No transaction history found for account ${accountNo}`,
        });
      }

      const transactions = await repo.findHistoryByPortfolioId(
        portfolio.portfolioId
      );

      const body: HistoryResponse = {
        accountNo: portfolio.accountNo,
        portfolioId: portfolio.portfolioId,
        transactions: transactions.map((t) => ({
          date: t.transactionDate,
          type: t.transactionType,
          units: t.quantity,
          price: t.price,
          amount: t.amount,
        })),
      };
      return res.json(body);
    } catch (err) {
      return next(err);
    }
  });

  return router;
}
