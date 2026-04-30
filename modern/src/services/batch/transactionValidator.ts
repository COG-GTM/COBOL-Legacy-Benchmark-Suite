/**
 * Batch Transaction Validator — migrated from PORTTRAN.cbl 2100-VALIDATE-TRANSACTION
 *
 * Validates an array of transactions before batch processing:
 *  - 2110-CHECK-PORTFOLIO: portfolio ID present and exists
 *  - 2120-CHECK-TRANSACTION-TYPE: valid type (BU/SL/TR/FE)
 *  - 2130-CHECK-AMOUNTS: quantity, price, amount > 0 (except Transfer)
 */

import { PrismaClient, TransactionType } from "@prisma/client";
import Decimal from "decimal.js";

export interface BatchTransactionInput {
  portfolioId: string;
  investmentId: string;
  type: TransactionType;
  quantity: number | string;
  price: number | string;
  amount: number | string;
}

export interface ValidationError {
  index: number;
  field: string;
  message: string;
}

export interface BatchValidationResult {
  valid: boolean;
  errors: ValidationError[];
  validCount: number;
  errorCount: number;
}

const VALID_TYPES = new Set<string>([
  TransactionType.BUY,
  TransactionType.SELL,
  TransactionType.TRANSFER,
  TransactionType.FEE,
]);

export class BatchTransactionValidator {
  constructor(private readonly prisma: PrismaClient) {}

  async validate(
    transactions: BatchTransactionInput[],
  ): Promise<BatchValidationResult> {
    const errors: ValidationError[] = [];

    for (let i = 0; i < transactions.length; i++) {
      const txn = transactions[i];
      const itemErrors = await this.validateSingle(i, txn);
      errors.push(...itemErrors);
    }

    return {
      valid: errors.length === 0,
      errors,
      validCount: transactions.length - new Set(errors.map((e) => e.index)).size,
      errorCount: new Set(errors.map((e) => e.index)).size,
    };
  }

  private async validateSingle(
    index: number,
    txn: BatchTransactionInput,
  ): Promise<ValidationError[]> {
    const errors: ValidationError[] = [];

    // 2110-CHECK-PORTFOLIO
    if (!txn.portfolioId || txn.portfolioId.trim() === "") {
      errors.push({
        index,
        field: "portfolioId",
        message: "Portfolio ID is required",
      });
      return errors; // short-circuit like COBOL
    }

    const portfolio = await this.prisma.portfolio.findUnique({
      where: { id: txn.portfolioId },
    });
    if (!portfolio) {
      errors.push({
        index,
        field: "portfolioId",
        message: `Invalid Portfolio ID: ${txn.portfolioId}`,
      });
      return errors;
    }

    // 2120-CHECK-TRANSACTION-TYPE
    if (!VALID_TYPES.has(txn.type)) {
      errors.push({
        index,
        field: "type",
        message: `Invalid Transaction Type: ${txn.type}`,
      });
    }

    // 2130-CHECK-AMOUNTS
    const qty = new Decimal(txn.quantity);
    const price = new Decimal(txn.price);
    const amount = new Decimal(txn.amount);

    if (qty.lte(0)) {
      errors.push({
        index,
        field: "quantity",
        message: "Quantity must be greater than zero",
      });
    }

    if (price.lte(0) && txn.type !== TransactionType.TRANSFER) {
      errors.push({
        index,
        field: "price",
        message: "Price must be greater than zero",
      });
    }

    if (amount.lte(0) && txn.type !== TransactionType.TRANSFER) {
      errors.push({
        index,
        field: "amount",
        message: "Amount must be greater than zero",
      });
    }

    return errors;
  }
}
