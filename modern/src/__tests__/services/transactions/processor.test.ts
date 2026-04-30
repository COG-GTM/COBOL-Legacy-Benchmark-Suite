/**
 * Unit tests for Transaction Processor
 * Covers Buy/Sell/Transfer/Fee logic from PORTTRAN.cbl:
 *  - 2210-PROCESS-BUY: add units and cost
 *  - 2220-PROCESS-SELL: subtract units with sufficiency check
 *  - 2230-PROCESS-TRANSFER: not implemented (error)
 *  - 2240-PROCESS-FEE: deduct from cost basis
 *  - 2100-VALIDATE-TRANSACTION: input validation
 */

import { describe, it, expect, vi, beforeEach } from "vitest";
import { TransactionProcessor } from "@/services/transactions/processor";
import { TransactionType, TransactionStatus } from "@prisma/client";

// Mock Prisma client
function createMockPrisma() {
  const mockPortfolio = {
    id: "PORT0001",
    accountNo: "1234567890",
    clientName: "Test Client",
    totalValue: 10000,
  };

  const mockPosition = {
    id: "pos-1",
    portfolioId: "PORT0001",
    investmentId: "AAPL",
    quantity: { toString: () => "100" },
    costBasis: { toString: () => "15000" },
    marketValue: { toString: () => "17500" },
  };

  const mockTransaction = { id: "txn-1" };

  const txClient = {
    transaction: {
      create: vi.fn().mockResolvedValue(mockTransaction),
      update: vi.fn().mockResolvedValue(mockTransaction),
    },
    position: {
      findUnique: vi.fn().mockResolvedValue(mockPosition),
      create: vi.fn().mockResolvedValue(mockPosition),
      update: vi.fn().mockResolvedValue(mockPosition),
    },
    portfolio: {
      update: vi.fn().mockResolvedValue(mockPortfolio),
    },
  };

  return {
    portfolio: {
      findUnique: vi.fn().mockResolvedValue(mockPortfolio),
    },
    $transaction: vi.fn(async (fn: (tx: typeof txClient) => Promise<unknown>) => {
      return fn(txClient);
    }),
    _txClient: txClient,
    _mockPosition: mockPosition,
  };
}

describe("TransactionProcessor", () => {
  let mockPrisma: ReturnType<typeof createMockPrisma>;
  let processor: TransactionProcessor;

  beforeEach(() => {
    mockPrisma = createMockPrisma();
    processor = new TransactionProcessor(mockPrisma as any);
  });

  describe("input validation (2100-VALIDATE-TRANSACTION)", () => {
    it("rejects empty portfolio ID", async () => {
      const result = await processor.process({
        portfolioId: "",
        investmentId: "AAPL",
        type: TransactionType.BUY,
        quantity: 10,
        price: 150,
        amount: 1500,
      });
      expect(result.success).toBe(false);
      expect(result.error).toBe("Portfolio ID is required");
    });

    it("rejects zero quantity", async () => {
      const result = await processor.process({
        portfolioId: "PORT0001",
        investmentId: "AAPL",
        type: TransactionType.BUY,
        quantity: 0,
        price: 150,
        amount: 1500,
      });
      expect(result.success).toBe(false);
      expect(result.error).toBe("Quantity must be greater than zero");
    });

    it("rejects zero price for non-transfer", async () => {
      const result = await processor.process({
        portfolioId: "PORT0001",
        investmentId: "AAPL",
        type: TransactionType.BUY,
        quantity: 10,
        price: 0,
        amount: 1500,
      });
      expect(result.success).toBe(false);
      expect(result.error).toBe("Price must be greater than zero");
    });

    it("rejects zero amount for non-transfer", async () => {
      const result = await processor.process({
        portfolioId: "PORT0001",
        investmentId: "AAPL",
        type: TransactionType.BUY,
        quantity: 10,
        price: 150,
        amount: 0,
      });
      expect(result.success).toBe(false);
      expect(result.error).toBe("Amount must be greater than zero");
    });

    it("rejects non-existent portfolio", async () => {
      mockPrisma.portfolio.findUnique.mockResolvedValue(null);
      const result = await processor.process({
        portfolioId: "PORT9999",
        investmentId: "AAPL",
        type: TransactionType.BUY,
        quantity: 10,
        price: 150,
        amount: 1500,
      });
      expect(result.success).toBe(false);
      expect(result.error).toBe("Invalid Portfolio ID: PORT9999");
    });
  });

  describe("BUY processing (2210-PROCESS-BUY)", () => {
    it("creates a new position for a new investment", async () => {
      mockPrisma._txClient.position.findUnique.mockResolvedValue(null);

      const result = await processor.process({
        portfolioId: "PORT0001",
        investmentId: "MSFT",
        type: TransactionType.BUY,
        quantity: 50,
        price: 400,
        amount: 20000,
      });

      expect(result.success).toBe(true);
      expect(result.transactionId).toBe("txn-1");
      expect(mockPrisma._txClient.position.create).toHaveBeenCalledWith(
        expect.objectContaining({
          data: expect.objectContaining({
            portfolioId: "PORT0001",
            investmentId: "MSFT",
            quantity: 50,
            costBasis: 20000,
          }),
        }),
      );
    });

    it("adds to existing position quantity and cost", async () => {
      const result = await processor.process({
        portfolioId: "PORT0001",
        investmentId: "AAPL",
        type: TransactionType.BUY,
        quantity: 25,
        price: 150,
        amount: 3750,
      });

      expect(result.success).toBe(true);
      expect(mockPrisma._txClient.position.update).toHaveBeenCalledWith(
        expect.objectContaining({
          data: expect.objectContaining({
            quantity: 125,    // 100 + 25
            costBasis: 18750, // 15000 + 3750
          }),
        }),
      );
    });

    it("updates portfolio total value after buy", async () => {
      await processor.process({
        portfolioId: "PORT0001",
        investmentId: "AAPL",
        type: TransactionType.BUY,
        quantity: 10,
        price: 150,
        amount: 1500,
      });

      expect(mockPrisma._txClient.portfolio.update).toHaveBeenCalledWith(
        expect.objectContaining({
          data: expect.objectContaining({
            totalValue: { increment: 1500 },
          }),
        }),
      );
    });

    it("marks transaction as DONE on success", async () => {
      await processor.process({
        portfolioId: "PORT0001",
        investmentId: "AAPL",
        type: TransactionType.BUY,
        quantity: 10,
        price: 150,
        amount: 1500,
      });

      expect(mockPrisma._txClient.transaction.update).toHaveBeenCalledWith(
        expect.objectContaining({
          data: expect.objectContaining({
            status: TransactionStatus.DONE,
          }),
        }),
      );
    });
  });

  describe("SELL processing (2220-PROCESS-SELL)", () => {
    it("subtracts quantity and cost from position", async () => {
      const result = await processor.process({
        portfolioId: "PORT0001",
        investmentId: "AAPL",
        type: TransactionType.SELL,
        quantity: 30,
        price: 175,
        amount: 5250,
      });

      expect(result.success).toBe(true);
      expect(mockPrisma._txClient.position.update).toHaveBeenCalledWith(
        expect.objectContaining({
          data: expect.objectContaining({
            quantity: 70,      // 100 - 30
            costBasis: 9750,   // 15000 - 5250
          }),
        }),
      );
    });

    it("rejects sell when insufficient units", async () => {
      const result = await processor.process({
        portfolioId: "PORT0001",
        investmentId: "AAPL",
        type: TransactionType.SELL,
        quantity: 200,
        price: 175,
        amount: 35000,
      });

      expect(result.success).toBe(false);
      expect(result.error).toBe("Insufficient units for sale");
    });

    it("rejects sell for non-existent position", async () => {
      mockPrisma._txClient.position.findUnique.mockResolvedValue(null);

      const result = await processor.process({
        portfolioId: "PORT0001",
        investmentId: "TSLA",
        type: TransactionType.SELL,
        quantity: 10,
        price: 250,
        amount: 2500,
      });

      expect(result.success).toBe(false);
      expect(result.error).toBe("Position not found for sell");
    });

    it("decrements portfolio total value after sell", async () => {
      await processor.process({
        portfolioId: "PORT0001",
        investmentId: "AAPL",
        type: TransactionType.SELL,
        quantity: 10,
        price: 175,
        amount: 1750,
      });

      expect(mockPrisma._txClient.portfolio.update).toHaveBeenCalledWith(
        expect.objectContaining({
          data: expect.objectContaining({
            totalValue: { decrement: 1750 },
          }),
        }),
      );
    });
  });

  describe("TRANSFER processing (2230-PROCESS-TRANSFER)", () => {
    it("returns error — not implemented", async () => {
      const result = await processor.process({
        portfolioId: "PORT0001",
        investmentId: "AAPL",
        type: TransactionType.TRANSFER,
        quantity: 10,
        price: 0,
        amount: 0,
      });

      expect(result.success).toBe(false);
      expect(result.error).toBe("Transfer processing not implemented");
    });
  });

  describe("FEE processing (2240-PROCESS-FEE)", () => {
    it("deducts fee from position cost basis", async () => {
      const result = await processor.process({
        portfolioId: "PORT0001",
        investmentId: "AAPL",
        type: TransactionType.FEE,
        quantity: 1,
        price: 25,
        amount: 25,
      });

      expect(result.success).toBe(true);
      expect(mockPrisma._txClient.position.update).toHaveBeenCalledWith(
        expect.objectContaining({
          data: expect.objectContaining({
            costBasis: 14975,  // 15000 - 25
          }),
        }),
      );
    });

    it("deducts fee from portfolio total when no position", async () => {
      mockPrisma._txClient.position.findUnique.mockResolvedValue(null);

      const result = await processor.process({
        portfolioId: "PORT0001",
        investmentId: "UNKNOWN",
        type: TransactionType.FEE,
        quantity: 1,
        price: 50,
        amount: 50,
      });

      expect(result.success).toBe(true);
      expect(mockPrisma._txClient.portfolio.update).toHaveBeenCalledWith(
        expect.objectContaining({
          data: expect.objectContaining({
            totalValue: { decrement: 50 },
          }),
        }),
      );
    });
  });
});
