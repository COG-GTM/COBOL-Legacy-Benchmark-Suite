/**
 * Integration tests — error scenarios:
 *  - Invalid inputs rejected by validation
 *  - Duplicate IDs
 *  - Insufficient funds for sell
 *  - Transfer not implemented
 */

import { describe, it, expect, beforeAll, afterAll, beforeEach } from "vitest";
import { PrismaClient } from "@prisma/client";
import { TransactionProcessor } from "@/services/transactions/processor";
import {
  validatePortfolioId,
  validateAccountNumber,
  validateInvestmentType,
  validateAmount,
  ValidationCode,
} from "@/services/portfolio/validation";

const TEST_DB_URL = process.env.DATABASE_URL;
const describeIntegration = TEST_DB_URL ? describe : describe.skip;

describeIntegration("Error Scenarios Integration", () => {
  let prisma: PrismaClient;
  let processor: TransactionProcessor;

  beforeAll(async () => {
    prisma = new PrismaClient();
    await prisma.$connect();
    processor = new TransactionProcessor(prisma);
  });

  afterAll(async () => {
    await prisma.$disconnect();
  });

  beforeEach(async () => {
    await prisma.auditLog.deleteMany({});
    await prisma.transaction.deleteMany({});
    await prisma.position.deleteMany({});
    await prisma.batchJob.deleteMany({});
    await prisma.portfolio.deleteMany({});
  });

  describe("validation errors", () => {
    it("rejects invalid portfolio ID format", () => {
      expect(validatePortfolioId("ACCT1234").code).toBe(
        ValidationCode.INVALID_ID,
      );
    });

    it("rejects non-numeric account number", () => {
      expect(validateAccountNumber("123ABC7890").code).toBe(
        ValidationCode.INVALID_ACCOUNT,
      );
    });

    it("rejects unknown investment type", () => {
      expect(validateInvestmentType("FUT").code).toBe(
        ValidationCode.INVALID_TYPE,
      );
    });

    it("rejects out-of-range amount", () => {
      expect(validateAmount("99999999999999.99").code).toBe(
        ValidationCode.INVALID_AMOUNT,
      );
    });
  });

  describe("transaction errors", () => {
    it("rejects transaction for non-existent portfolio", async () => {
      const result = await processor.process({
        portfolioId: "PORT9999",
        investmentId: "AAPL",
        type: "BUY",
        quantity: 10,
        price: 150,
        amount: 1500,
      });

      expect(result.success).toBe(false);
      expect(result.error).toContain("Invalid Portfolio ID");
    });

    it("rejects sell with insufficient units", async () => {
      await prisma.portfolio.create({
        data: {
          id: "PORT0001",
          accountNo: "1234567890",
          clientName: "Error Test Client",
        },
      });

      // Buy 10 shares
      await processor.process({
        portfolioId: "PORT0001",
        investmentId: "AAPL",
        type: "BUY",
        quantity: 10,
        price: 150,
        amount: 1500,
      });

      // Try to sell 50 — should fail
      const result = await processor.process({
        portfolioId: "PORT0001",
        investmentId: "AAPL",
        type: "SELL",
        quantity: 50,
        price: 150,
        amount: 7500,
      });

      expect(result.success).toBe(false);
      expect(result.error).toBe("Insufficient units for sale");
    });

    it("rejects transfer — not implemented", async () => {
      await prisma.portfolio.create({
        data: {
          id: "PORT0002",
          accountNo: "2345678901",
          clientName: "Transfer Test",
        },
      });

      const result = await processor.process({
        portfolioId: "PORT0002",
        investmentId: "AAPL",
        type: "TRANSFER",
        quantity: 10,
        price: 0,
        amount: 0,
      });

      expect(result.success).toBe(false);
      expect(result.error).toBe("Transfer processing not implemented");
    });
  });

  describe("duplicate ID errors", () => {
    it("rejects duplicate portfolio ID", async () => {
      await prisma.portfolio.create({
        data: {
          id: "PORT0003",
          accountNo: "3456789012",
          clientName: "First",
        },
      });

      await expect(
        prisma.portfolio.create({
          data: {
            id: "PORT0003",
            accountNo: "9876543210",
            clientName: "Duplicate",
          },
        }),
      ).rejects.toThrow();
    });

    it("rejects duplicate account number", async () => {
      await prisma.portfolio.create({
        data: {
          id: "PORT0004",
          accountNo: "4567890123",
          clientName: "First",
        },
      });

      await expect(
        prisma.portfolio.create({
          data: {
            id: "PORT0005",
            accountNo: "4567890123",
            clientName: "Duplicate Acct",
          },
        }),
      ).rejects.toThrow();
    });
  });
});
