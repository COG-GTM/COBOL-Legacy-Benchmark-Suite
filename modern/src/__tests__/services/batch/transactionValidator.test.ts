/**
 * Unit tests for Batch Transaction Validator
 * Covers validation pass/fail scenarios from PORTTRAN.cbl:
 *  - 2110-CHECK-PORTFOLIO: portfolio presence and existence
 *  - 2120-CHECK-TRANSACTION-TYPE: valid BU/SL/TR/FE types
 *  - 2130-CHECK-AMOUNTS: quantity, price, amount positivity
 */

import { describe, it, expect, vi, beforeEach } from "vitest";
import { BatchTransactionValidator, BatchTransactionInput } from "@/services/batch/transactionValidator";
import { TransactionType } from "@prisma/client";

function createMockPrisma() {
  return {
    portfolio: {
      findUnique: vi.fn().mockResolvedValue({
        id: "PORT0001",
        accountNo: "1234567890",
      }),
    },
  };
}

describe("BatchTransactionValidator", () => {
  let mockPrisma: ReturnType<typeof createMockPrisma>;
  let validator: BatchTransactionValidator;

  beforeEach(() => {
    mockPrisma = createMockPrisma();
    validator = new BatchTransactionValidator(mockPrisma as any);
  });

  describe("all-valid batch", () => {
    it("passes for a batch with valid transactions", async () => {
      const txns: BatchTransactionInput[] = [
        {
          portfolioId: "PORT0001",
          investmentId: "AAPL",
          type: TransactionType.BUY,
          quantity: 10,
          price: 150,
          amount: 1500,
        },
        {
          portfolioId: "PORT0001",
          investmentId: "GOOGL",
          type: TransactionType.SELL,
          quantity: 5,
          price: 2800,
          amount: 14000,
        },
      ];

      const result = await validator.validate(txns);
      expect(result.valid).toBe(true);
      expect(result.errors).toHaveLength(0);
      expect(result.validCount).toBe(2);
      expect(result.errorCount).toBe(0);
    });
  });

  describe("portfolio validation (2110-CHECK-PORTFOLIO)", () => {
    it("fails when portfolio ID is empty", async () => {
      const txns: BatchTransactionInput[] = [
        {
          portfolioId: "",
          investmentId: "AAPL",
          type: TransactionType.BUY,
          quantity: 10,
          price: 150,
          amount: 1500,
        },
      ];

      const result = await validator.validate(txns);
      expect(result.valid).toBe(false);
      expect(result.errors[0].field).toBe("portfolioId");
      expect(result.errors[0].message).toBe("Portfolio ID is required");
    });

    it("fails when portfolio does not exist in database", async () => {
      mockPrisma.portfolio.findUnique.mockResolvedValue(null);

      const txns: BatchTransactionInput[] = [
        {
          portfolioId: "PORT9999",
          investmentId: "AAPL",
          type: TransactionType.BUY,
          quantity: 10,
          price: 150,
          amount: 1500,
        },
      ];

      const result = await validator.validate(txns);
      expect(result.valid).toBe(false);
      expect(result.errors[0].message).toBe("Invalid Portfolio ID: PORT9999");
    });

    it("short-circuits on missing portfolio — no amount checks", async () => {
      const txns: BatchTransactionInput[] = [
        {
          portfolioId: "",
          investmentId: "AAPL",
          type: TransactionType.BUY,
          quantity: -1,
          price: -1,
          amount: -1,
        },
      ];

      const result = await validator.validate(txns);
      expect(result.errors).toHaveLength(1);
      expect(result.errors[0].field).toBe("portfolioId");
    });
  });

  describe("transaction type validation (2120-CHECK-TRANSACTION-TYPE)", () => {
    it.each([
      TransactionType.BUY,
      TransactionType.SELL,
      TransactionType.TRANSFER,
      TransactionType.FEE,
    ])("accepts valid type %s", async (type) => {
      const txns: BatchTransactionInput[] = [
        {
          portfolioId: "PORT0001",
          investmentId: "AAPL",
          type,
          quantity: 10,
          price: type === TransactionType.TRANSFER ? 0 : 150,
          amount: type === TransactionType.TRANSFER ? 0 : 1500,
        },
      ];

      const result = await validator.validate(txns);
      const typeErrors = result.errors.filter((e) => e.field === "type");
      expect(typeErrors).toHaveLength(0);
    });
  });

  describe("amount validation (2130-CHECK-AMOUNTS)", () => {
    it("fails when quantity is zero", async () => {
      const txns: BatchTransactionInput[] = [
        {
          portfolioId: "PORT0001",
          investmentId: "AAPL",
          type: TransactionType.BUY,
          quantity: 0,
          price: 150,
          amount: 1500,
        },
      ];

      const result = await validator.validate(txns);
      expect(result.valid).toBe(false);
      const qtyError = result.errors.find((e) => e.field === "quantity");
      expect(qtyError?.message).toBe("Quantity must be greater than zero");
    });

    it("fails when quantity is negative", async () => {
      const txns: BatchTransactionInput[] = [
        {
          portfolioId: "PORT0001",
          investmentId: "AAPL",
          type: TransactionType.BUY,
          quantity: -5,
          price: 150,
          amount: 1500,
        },
      ];

      const result = await validator.validate(txns);
      expect(result.errors.some((e) => e.field === "quantity")).toBe(true);
    });

    it("fails when price is zero for non-transfer", async () => {
      const txns: BatchTransactionInput[] = [
        {
          portfolioId: "PORT0001",
          investmentId: "AAPL",
          type: TransactionType.BUY,
          quantity: 10,
          price: 0,
          amount: 1500,
        },
      ];

      const result = await validator.validate(txns);
      expect(result.errors.some((e) => e.field === "price")).toBe(true);
    });

    it("allows zero price for TRANSFER type", async () => {
      const txns: BatchTransactionInput[] = [
        {
          portfolioId: "PORT0001",
          investmentId: "AAPL",
          type: TransactionType.TRANSFER,
          quantity: 10,
          price: 0,
          amount: 0,
        },
      ];

      const result = await validator.validate(txns);
      const priceErrors = result.errors.filter((e) => e.field === "price");
      expect(priceErrors).toHaveLength(0);
    });

    it("fails when amount is zero for non-transfer", async () => {
      const txns: BatchTransactionInput[] = [
        {
          portfolioId: "PORT0001",
          investmentId: "AAPL",
          type: TransactionType.SELL,
          quantity: 10,
          price: 150,
          amount: 0,
        },
      ];

      const result = await validator.validate(txns);
      expect(result.errors.some((e) => e.field === "amount")).toBe(true);
    });
  });

  describe("multiple errors in a batch", () => {
    it("reports errors for each invalid transaction", async () => {
      mockPrisma.portfolio.findUnique
        .mockResolvedValueOnce({ id: "PORT0001" }) // first txn valid
        .mockResolvedValueOnce(null); // second txn invalid

      const txns: BatchTransactionInput[] = [
        {
          portfolioId: "PORT0001",
          investmentId: "AAPL",
          type: TransactionType.BUY,
          quantity: 10,
          price: 150,
          amount: 1500,
        },
        {
          portfolioId: "PORT9999",
          investmentId: "AAPL",
          type: TransactionType.BUY,
          quantity: 10,
          price: 150,
          amount: 1500,
        },
      ];

      const result = await validator.validate(txns);
      expect(result.valid).toBe(false);
      expect(result.validCount).toBe(1);
      expect(result.errorCount).toBe(1);
      expect(result.errors[0].index).toBe(1);
    });

    it("accumulates multiple errors for a single transaction", async () => {
      const txns: BatchTransactionInput[] = [
        {
          portfolioId: "PORT0001",
          investmentId: "AAPL",
          type: TransactionType.BUY,
          quantity: -5,
          price: 0,
          amount: 0,
        },
      ];

      const result = await validator.validate(txns);
      expect(result.errors.length).toBeGreaterThanOrEqual(3);
    });
  });
});
