import { portfolios, positions, transactions } from './mockData';
import type {
  Portfolio,
  PortfolioSummary,
  PaginatedResult,
  Position,
  Transaction,
} from '@/types/portfolio';

const DELAY_MS = 300;

function delay(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

export async function searchPortfolio(accountNumber: string): Promise<Portfolio | null> {
  await delay(DELAY_MS);
  const portfolio = portfolios.find(
    (p) => p.accountNumber.toLowerCase() === accountNumber.toLowerCase()
  );
  return portfolio ?? null;
}

export async function getPortfolioPositions(
  accountNumber: string,
  page: number = 1,
  pageSize: number = 5
): Promise<PaginatedResult<Position> | null> {
  await delay(DELAY_MS);
  const portfolio = portfolios.find(
    (p) => p.accountNumber.toLowerCase() === accountNumber.toLowerCase()
  );
  if (!portfolio) return null;

  const portfolioPositions = positions.filter((p) => p.portfolioId === portfolio.portfolioId);
  const totalItems = portfolioPositions.length;
  const totalPages = Math.max(1, Math.ceil(totalItems / pageSize));
  const start = (page - 1) * pageSize;
  const data = portfolioPositions.slice(start, start + pageSize);

  return {
    data,
    totalItems,
    currentPage: page,
    totalPages,
    pageSize,
  };
}

export async function getPortfolioSummary(
  accountNumber: string
): Promise<PortfolioSummary | null> {
  await delay(DELAY_MS);
  const portfolio = portfolios.find(
    (p) => p.accountNumber.toLowerCase() === accountNumber.toLowerCase()
  );
  if (!portfolio) return null;

  const portfolioPositions = positions.filter((p) => p.portfolioId === portfolio.portfolioId);
  const totalMarketValue = portfolioPositions.reduce((sum, p) => sum + p.marketValue, 0);
  const totalCostBasis = portfolioPositions.reduce((sum, p) => sum + p.costBasis, 0);
  const totalGainLoss = totalMarketValue - totalCostBasis;
  const totalGainLossPercent = totalCostBasis > 0 ? (totalGainLoss / totalCostBasis) * 100 : 0;

  return {
    portfolio,
    positions: portfolioPositions,
    totalMarketValue,
    totalCostBasis,
    totalGainLoss,
    totalGainLossPercent,
  };
}

export async function getTransactionHistory(
  accountNumber: string,
  page: number = 1,
  pageSize: number = 10
): Promise<PaginatedResult<Transaction> | null> {
  await delay(DELAY_MS);
  const portfolio = portfolios.find(
    (p) => p.accountNumber.toLowerCase() === accountNumber.toLowerCase()
  );
  if (!portfolio) return null;

  const portfolioTransactions = transactions
    .filter((t) => t.portfolioId === portfolio.portfolioId)
    .sort((a, b) => {
      const dateCompare = b.date.localeCompare(a.date);
      if (dateCompare !== 0) return dateCompare;
      return b.time.localeCompare(a.time);
    });

  const totalItems = portfolioTransactions.length;
  const totalPages = Math.max(1, Math.ceil(totalItems / pageSize));
  const start = (page - 1) * pageSize;
  const data = portfolioTransactions.slice(start, start + pageSize);

  return {
    data,
    totalItems,
    currentPage: page,
    totalPages,
    pageSize,
  };
}
