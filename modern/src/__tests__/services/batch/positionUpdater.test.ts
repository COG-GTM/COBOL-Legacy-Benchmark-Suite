/**
 * Unit tests for Batch Position Updater
 * Covers position recalculation from POSUPDT.cbl / PORTTRAN.cbl:
 *  - BUY: creates or increments position
 *  - SELL: decrements position, checks sufficiency
 *  - FEE: deducts from cost basis
 *  - TRANSFER: not implemented
 *  - Portfolio total value recalculation
 *  - Error handling and transaction status updates
 */

import { describe, it, expect, vi, beforeEach } from "vitest";
import { PositionUpdater } from "@/services/batch/positionUpdater";
import { TransactionType, TransactionStatus } from "@prisma/client";

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
    status: "ACTIVE",
  };

  return {
    portfolio: {
      findUnique: vi.fn().mockResolvedValue(mockPortfolio),
      update: vi.fn().mockResolvedValue(mockPortfolio),
    },
    transaction: {
      findMany: vi.fn().mockResolvedValue([]),
      update: vi.fn().mockResolvedValue({}),
    },
    position: {
      findUnique: vi.fn().mockResolvedValue(mockPosition),
      findMany: vi.fn().mockResolvedValue([mockPosition]),
      create: vi.fn().mockResolvedValue(mockPosition),
      update: vi.fn().mockResolvedValue(mockPosition),
    },
    _mockPosition: mockPosition,
  };
}

describe("PositionUpdater", () => {
  let mockPrisma: ReturnType<typeof createMockPrisma>;
  let updater: PositionUpdater;

  beforeEach(() => {
    mockPrisma = createMockPrisma();
    updater = new PositionUpdater(mockPrisma as any);
  });

  describe("portfolio not found", () => {
    it("returns error when portfolio does not exist", async () => {
      mockPrisma.portfolio.findUnique.mockResolvedValue(null);

      const result = await updater.updatePositions("PORT9999");
      expect(result.errors).toContain("Portfolio not found: PORT9999");
      expect(result.positionsUpdated).toBe(0);
    });
  });

  describe("no pending transactions", () => {
    it("returns 0 updates when there are no pending transactions", async () => {
      mockPrisma.transaction.findMany.mockResolvedValue([]);

      const result = await updater.updatePositions("PORT0001");
      expect(result.positionsUpdated).toBe(0);
      expect(result.errors).toHaveLength(0);
    });

    it("still recalculates portfolio total from active positions", async () => {
      mockPrisma.transaction.findMany.mockResolvedValue([]);

      await updater.updatePositions("PORT0001");
      expect(mockPrisma.portfolio.update).toHaveBeenCalledWith(
        expect.objectContaining({
          data: expect.objectContaining({
            totalValue: 17500, // from mockPosition marketValue
          }),
        }),
      );
    });
  });

  describe("BUY transaction processing", () => {
    it("creates new position for new investment", async () => {
      mockPrisma.transaction.findMany.mockResolvedValue([
        {
          id: "txn-1",
          portfolioId: "PORT0001",
          investmentId: "MSFT",
          type: TransactionType.BUY,
          quantity: { toString: () => "50" },
          price: { toString: () => "400" },
          amount: { toString: () => "20000" },
          status: TransactionStatus.PENDING,
        },
      ]);
      mockPrisma.position.findUnique
        .mockResolvedValueOnce(null) // during applyBuy — no existing position
        .mockReturnValue(
          Promise.resolve(mockPrisma._mockPosition),
        ); // during recalculation

      const result = await updater.updatePositions("PORT0001");
      expect(result.positionsUpdated).toBe(1);
      expect(mockPrisma.position.create).toHaveBeenCalledWith(
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

    it("increments existing position quantity and cost", async () => {
      mockPrisma.transaction.findMany.mockResolvedValue([
        {
          id: "txn-1",
          portfolioId: "PORT0001",
          investmentId: "AAPL",
          type: TransactionType.BUY,
          quantity: { toString: () => "25" },
          price: { toString: () => "150" },
          amount: { toString: () => "3750" },
          status: TransactionStatus.PENDING,
        },
      ]);

      const result = await updater.updatePositions("PORT0001");
      expect(result.positionsUpdated).toBe(1);
      expect(mockPrisma.position.update).toHaveBeenCalledWith(
        expect.objectContaining({
          data: expect.objectContaining({
            quantity: 125,    // 100 + 25
            costBasis: 18750, // 15000 + 3750
          }),
        }),
      );
    });

    it("marks transaction as DONE after processing", async () => {
      mockPrisma.transaction.findMany.mockResolvedValue([
        {
          id: "txn-1",
          portfolioId: "PORT0001",
          investmentId: "AAPL",
          type: TransactionType.BUY,
          quantity: { toString: () => "10" },
          price: { toString: () => "150" },
          amount: { toString: () => "1500" },
          status: TransactionStatus.PENDING,
        },
      ]);

      await updater.updatePositions("PORT0001");
      expect(mockPrisma.transaction.update).toHaveBeenCalledWith(
        expect.objectContaining({
          where: { id: "txn-1" },
          data: expect.objectContaining({
            status: TransactionStatus.DONE,
          }),
        }),
      );
    });
  });

  describe("SELL transaction processing", () => {
    it("subtracts quantity from existing position", async () => {
      mockPrisma.transaction.findMany.mockResolvedValue([
        {
          id: "txn-2",
          portfolioId: "PORT0001",
          investmentId: "AAPL",
          type: TransactionType.SELL,
          quantity: { toString: () => "30" },
          price: { toString: () => "175" },
          amount: { toString: () => "5250" },
          status: TransactionStatus.PENDING,
        },
      ]);

      const result = await updater.updatePositions("PORT0001");
      expect(result.positionsUpdated).toBe(1);
      expect(mockPrisma.position.update).toHaveBeenCalledWith(
        expect.objectContaining({
          data: expect.objectContaining({
            quantity: 70,    // 100 - 30
            costBasis: 9750, // 15000 - 5250
          }),
        }),
      );
    });

    it("fails when insufficient units — marks transaction FAILED", async () => {
      mockPrisma.transaction.findMany.mockResolvedValue([
        {
          id: "txn-3",
          portfolioId: "PORT0001",
          investmentId: "AAPL",
          type: TransactionType.SELL,
          quantity: { toString: () => "200" },
          price: { toString: () => "175" },
          amount: { toString: () => "35000" },
          status: TransactionStatus.PENDING,
        },
      ]);

      const result = await updater.updatePositions("PORT0001");
      expect(result.errors).toHaveLength(1);
      expect(result.errors[0]).toContain("Insufficient units for sale");
      expect(mockPrisma.transaction.update).toHaveBeenCalledWith(
        expect.objectContaining({
          where: { id: "txn-3" },
          data: { status: TransactionStatus.FAILED },
        }),
      );
    });

    it("closes position when all units sold", async () => {
      mockPrisma.transaction.findMany.mockResolvedValue([
        {
          id: "txn-4",
          portfolioId: "PORT0001",
          investmentId: "AAPL",
          type: TransactionType.SELL,
          quantity: { toString: () => "100" },
          price: { toString: () => "175" },
          amount: { toString: () => "17500" },
          status: TransactionStatus.PENDING,
        },
      ]);

      await updater.updatePositions("PORT0001");
      expect(mockPrisma.position.update).toHaveBeenCalledWith(
        expect.objectContaining({
          data: expect.objectContaining({
            quantity: 0,
            status: "CLOSED",
          }),
        }),
      );
    });
  });

  describe("FEE transaction processing", () => {
    it("deducts fee from cost basis", async () => {
      mockPrisma.transaction.findMany.mockResolvedValue([
        {
          id: "txn-5",
          portfolioId: "PORT0001",
          investmentId: "AAPL",
          type: TransactionType.FEE,
          quantity: { toString: () => "1" },
          price: { toString: () => "25" },
          amount: { toString: () => "25" },
          status: TransactionStatus.PENDING,
        },
      ]);

      const result = await updater.updatePositions("PORT0001");
      expect(result.positionsUpdated).toBe(1);
      expect(mockPrisma.position.update).toHaveBeenCalledWith(
        expect.objectContaining({
          data: expect.objectContaining({
            costBasis: 14975, // 15000 - 25
          }),
        }),
      );
    });
  });

  describe("TRANSFER transaction processing", () => {
    it("fails with not-implemented error", async () => {
      mockPrisma.transaction.findMany.mockResolvedValue([
        {
          id: "txn-6",
          portfolioId: "PORT0001",
          investmentId: "AAPL",
          type: TransactionType.TRANSFER,
          quantity: { toString: () => "10" },
          price: { toString: () => "0" },
          amount: { toString: () => "0" },
          status: TransactionStatus.PENDING,
        },
      ]);

      const result = await updater.updatePositions("PORT0001");
      expect(result.errors).toHaveLength(1);
      expect(result.errors[0]).toContain("Transfer processing not implemented");
    });
  });

  describe("portfolio total recalculation", () => {
    it("sums market values of all active positions", async () => {
      mockPrisma.transaction.findMany.mockResolvedValue([]);
      mockPrisma.position.findMany.mockResolvedValue([
        { marketValue: { toString: () => "17500" }, status: "ACTIVE" },
        { marketValue: { toString: () => "25000" }, status: "ACTIVE" },
      ]);

      await updater.updatePositions("PORT0001");
      expect(mockPrisma.portfolio.update).toHaveBeenCalledWith(
        expect.objectContaining({
          data: expect.objectContaining({
            totalValue: 42500, // 17500 + 25000
          }),
        }),
      );
    });
  });
});
