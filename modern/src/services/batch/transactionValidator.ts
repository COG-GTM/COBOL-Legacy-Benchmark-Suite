/**
 * Transaction Validator (Batch)
 * Ported from: src/programs/batch/TRNVAL00.cbl (referenced in system-architecture.md)
 * and validation logic from src/programs/portfolio/PORTTRAN.cbl (2100-2130)
 *
 * Validates input transactions: checks required fields, valid transaction types,
 * valid portfolio IDs, positive quantities/prices.
 * Returns RC <= 4 to proceed, RC > 4 to halt pipeline.
 */

import { Decimal } from "decimal.js";
import { prisma } from "../../lib/prisma";
import {
  TransactionType,
  ReturnCode,
  type TransactionInput,
  type ValidationError,
  type ValidationResult,
} from "../../types";

const VALID_TRANSACTION_TYPES = new Set<string>([
  TransactionType.BUY,
  TransactionType.SELL,
  TransactionType.TRANSFER,
  TransactionType.FEE,
]);

/**
 * Validate a batch of transactions.
 * Mirrors TRNVAL00 pipeline step: validates all transactions before processing.
 */
export async function validateTransactions(
  transactions: TransactionInput[]
): Promise<ValidationResult> {
  const errors: ValidationError[] = [];
  let validCount = 0;

  for (let i = 0; i < transactions.length; i++) {
    const txn = transactions[i];
    const txnErrors = await validateSingleTransaction(txn, i);

    if (txnErrors.length === 0) {
      validCount++;
    } else {
      errors.push(...txnErrors);
    }
  }

  const errorCount = transactions.length - validCount;
  const returnCode = determineReturnCode(validCount, errorCount) as ReturnCode;

  return {
    valid: returnCode <= ReturnCode.WARNING,
    returnCode,
    errors,
    validCount,
    errorCount,
  };
}

/**
 * Validate a single transaction input.
 * Mirrors 2100-VALIDATE-TRANSACTION through 2130-CHECK-AMOUNTS in PORTTRAN.cbl.
 */
async function validateSingleTransaction(
  txn: TransactionInput,
  index: number
): Promise<ValidationError[]> {
  const errors: ValidationError[] = [];

  // 2110-CHECK-PORTFOLIO: Portfolio ID is required and must exist
  if (!txn.portfolioId || txn.portfolioId.trim() === "") {
    errors.push({
      field: "portfolioId",
      message: "Portfolio ID is required",
      transactionIndex: index,
    });
  } else if (txn.portfolioId.length > 8) {
    errors.push({
      field: "portfolioId",
      message: "Portfolio ID must be 8 characters or less",
      transactionIndex: index,
    });
  } else {
    const portfolio = await prisma.portfolio.findUnique({
      where: { portfolioId: txn.portfolioId },
      select: { status: true },
    });

    if (!portfolio) {
      errors.push({
        field: "portfolioId",
        message: `Invalid Portfolio ID: ${txn.portfolioId}`,
        transactionIndex: index,
      });
    } else if (portfolio.status !== "A") {
      errors.push({
        field: "portfolioId",
        message: `Portfolio ${txn.portfolioId} is not active (status: ${portfolio.status})`,
        transactionIndex: index,
      });
    }
  }

  // Investment ID is required
  if (!txn.investmentId || txn.investmentId.trim() === "") {
    errors.push({
      field: "investmentId",
      message: "Investment ID is required",
      transactionIndex: index,
    });
  } else if (txn.investmentId.length > 10) {
    errors.push({
      field: "investmentId",
      message: "Investment ID must be 10 characters or less",
      transactionIndex: index,
    });
  }

  // 2120-CHECK-TRANSACTION-TYPE: Must be valid type
  if (!txn.type || !VALID_TRANSACTION_TYPES.has(txn.type)) {
    errors.push({
      field: "type",
      message: `Invalid Transaction Type: ${txn.type ?? "(empty)"}`,
      transactionIndex: index,
    });
  }

  // 2130-CHECK-AMOUNTS: Positive quantities and prices
  if (!txn.quantity || new Decimal(txn.quantity.toString()).lte(0)) {
    errors.push({
      field: "quantity",
      message: "Quantity must be greater than zero",
      transactionIndex: index,
    });
  }

  // Price must be positive (except for transfers)
  if (txn.type !== TransactionType.TRANSFER) {
    if (!txn.price || new Decimal(txn.price.toString()).lte(0)) {
      errors.push({
        field: "price",
        message: "Price must be greater than zero",
        transactionIndex: index,
      });
    }

    if (!txn.amount || new Decimal(txn.amount.toString()).lte(0)) {
      errors.push({
        field: "amount",
        message: "Amount must be greater than zero",
        transactionIndex: index,
      });
    }
  }

  // Transfer-specific validation
  if (txn.type === TransactionType.TRANSFER) {
    if (!txn.targetPortfolioId || txn.targetPortfolioId.trim() === "") {
      errors.push({
        field: "targetPortfolioId",
        message: "Target portfolio ID is required for transfers",
        transactionIndex: index,
      });
    } else {
      const targetPortfolio = await prisma.portfolio.findUnique({
        where: { portfolioId: txn.targetPortfolioId },
        select: { status: true },
      });

      if (!targetPortfolio) {
        errors.push({
          field: "targetPortfolioId",
          message: `Invalid target Portfolio ID: ${txn.targetPortfolioId}`,
          transactionIndex: index,
        });
      } else if (targetPortfolio.status !== "A") {
        errors.push({
          field: "targetPortfolioId",
          message: `Target portfolio ${txn.targetPortfolioId} is not active`,
          transactionIndex: index,
        });
      }
    }

    if (txn.portfolioId === txn.targetPortfolioId) {
      errors.push({
        field: "targetPortfolioId",
        message: "Cannot transfer to the same portfolio",
        transactionIndex: index,
      });
    }
  }

  return errors;
}

/**
 * Determine return code based on validation results.
 * RC 0 = all valid, RC 4 = warnings (some invalid but can proceed),
 * RC 8+ = too many errors, halt pipeline.
 */
function determineReturnCode(validCount: number, errorCount: number): number {
  if (errorCount === 0) {
    return ReturnCode.SUCCESS;
  }

  const totalCount = validCount + errorCount;
  const errorRate = errorCount / totalCount;

  // If more than 50% errors, halt the pipeline
  if (errorRate > 0.5) {
    return ReturnCode.ERROR;
  }

  // Some errors but majority valid: warning, proceed
  return ReturnCode.WARNING;
}
