/**
 * Integration tests — full CRUD lifecycle:
 *  create portfolio → add position → submit transaction →
 *  verify position update → check history
 *
 * These tests exercise the API routes end-to-end with a real (test)
 * database. In CI they run against docker-compose PostgreSQL.
 * Locally they can be pointed at any Postgres instance via DATABASE_URL.
 *
 * Requires: DATABASE_URL pointing to a test database
 */

import { describe, it, expect, beforeAll, afterAll, beforeEach } from "vitest";
import { PrismaClient } from "@prisma/client";
import { TransactionProcessor } from "@/services/transactions/processor";
import {
  validatePortfolioId,
  validateAccountNumber,
} from "@/services/portfolio/validation";

const TEST_DB_URL = process.env.DATABASE_URL;

// Skip integration tests if no database is configured
const describeIntegration = TEST_DB_URL
  ? describe
  : describe.skip;

describeIntegration("Portfolio Lifecycle Integration", () => {
  let prisma: PrismaClient;

  beforeAll(async () => {
    prisma = new PrismaClient();
    await prisma.$connect();
  });

  afterAll(async () => {
    await prisma.$disconnect();
  });

  beforeEach(async () => {
    // Clean up test data in correct order (respect FK constraints)
    await prisma.auditLog.deleteMany({});
    await prisma.transaction.deleteMany({});
    await prisma.position.deleteMany({});
    await prisma.batchJob.deleteMany({});
    await prisma.portfolio.deleteMany({});
  });

  it("creates a portfolio, adds a position via BUY transaction, and verifies", async () => {
    // Step 1: Validate inputs
    expect(validatePortfolioId("PORT0001").code).toBe(0);
    expect(validateAccountNumber("1234567890").code).toBe(0);

    // Step 2: Create portfolio
    const portfolio = await prisma.portfolio.create({
      data: {
        id: "PORT0001",
        accountNo: "1234567890",
        clientName: "Integration Test Client",
        clientType: "INDIVIDUAL",
      },
    });
    expect(portfolio.id).toBe("PORT0001");
    expect(portfolio.status).toBe("ACTIVE");

    // Step 3: Submit BUY transaction
    const processor = new TransactionProcessor(prisma);
    const buyResult = await processor.process({
      portfolioId: "PORT0001",
      investmentId: "AAPL",
      type: "BUY",
      quantity: 100,
      price: 150,
      amount: 15000,
    });
    expect(buyResult.success).toBe(true);
    expect(buyResult.transactionId).toBeDefined();

    // Step 4: Verify position was created
    const position = await prisma.position.findUnique({
      where: {
        portfolioId_investmentId: {
          portfolioId: "PORT0001",
          investmentId: "AAPL",
        },
      },
    });
    expect(position).not.toBeNull();
    expect(Number(position!.quantity)).toBe(100);
    expect(Number(position!.costBasis)).toBe(15000);

    // Step 5: Verify portfolio total updated
    const updated = await prisma.portfolio.findUnique({
      where: { id: "PORT0001" },
    });
    expect(Number(updated!.totalValue)).toBe(15000);

    // Step 6: Verify transaction history
    const transactions = await prisma.transaction.findMany({
      where: { portfolioId: "PORT0001" },
    });
    expect(transactions).toHaveLength(1);
    expect(transactions[0].status).toBe("DONE");
    expect(transactions[0].type).toBe("BUY");
  });

  it("processes BUY then SELL and verifies position updates", async () => {
    await prisma.portfolio.create({
      data: {
        id: "PORT0002",
        accountNo: "2345678901",
        clientName: "Sell Test Client",
      },
    });

    const processor = new TransactionProcessor(prisma);

    // Buy 100 shares
    await processor.process({
      portfolioId: "PORT0002",
      investmentId: "GOOGL",
      type: "BUY",
      quantity: 100,
      price: 2800,
      amount: 280000,
    });

    // Sell 40 shares
    const sellResult = await processor.process({
      portfolioId: "PORT0002",
      investmentId: "GOOGL",
      type: "SELL",
      quantity: 40,
      price: 2900,
      amount: 116000,
    });
    expect(sellResult.success).toBe(true);

    // Verify position
    const position = await prisma.position.findUnique({
      where: {
        portfolioId_investmentId: {
          portfolioId: "PORT0002",
          investmentId: "GOOGL",
        },
      },
    });
    expect(Number(position!.quantity)).toBe(60);
    expect(Number(position!.costBasis)).toBe(164000); // 280000 - 116000
  });

  it("rejects duplicate portfolio ID", async () => {
    await prisma.portfolio.create({
      data: {
        id: "PORT0003",
        accountNo: "3456789012",
        clientName: "Original Client",
      },
    });

    await expect(
      prisma.portfolio.create({
        data: {
          id: "PORT0003",
          accountNo: "4567890123",
          clientName: "Duplicate Client",
        },
      }),
    ).rejects.toThrow();
  });

  it("rejects duplicate account number", async () => {
    await prisma.portfolio.create({
      data: {
        id: "PORT0004",
        accountNo: "5678901234",
        clientName: "First Client",
      },
    });

    await expect(
      prisma.portfolio.create({
        data: {
          id: "PORT0005",
          accountNo: "5678901234",
          clientName: "Second Client",
        },
      }),
    ).rejects.toThrow();
  });
});
