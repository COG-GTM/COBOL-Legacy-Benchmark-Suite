/**
 * Transaction Processor — migrated from PORTTRAN.cbl
 *
 * Processes four transaction types:
 *  BUY  — adds units & cost to portfolio position
 *  SELL — subtracts units & cost (checks sufficiency)
 *  TRANSFER — moves units between portfolios
 *  FEE  — deducts amount from portfolio cost basis
 */

import { PrismaClient, TransactionType, TransactionStatus } from "@prisma/client";
import Decimal from "decimal.js";

export interface TransactionInput {
  portfolioId: string;
  investmentId: string;
  type: TransactionType;
  quantity: number | string;
  price: number | string;
  amount: number | string;
  currency?: string;
}

export interface ProcessResult {
  success: boolean;
  transactionId?: string;
  error?: string;
}

export class TransactionProcessor {
  constructor(private readonly prisma: PrismaClient) {}

  async process(input: TransactionInput): Promise<ProcessResult> {
    const qty = new Decimal(input.quantity);
    const price = new Decimal(input.price);
    const amount = new Decimal(input.amount);

    const validationError = this.validateInput(input, qty, price, amount);
    if (validationError) {
      return { success: false, error: validationError };
    }

    const portfolio = await this.prisma.portfolio.findUnique({
      where: { id: input.portfolioId },
    });
    if (!portfolio) {
      return { success: false, error: `Invalid Portfolio ID: ${input.portfolioId}` };
    }

    try {
      return await this.prisma.$transaction(async (tx) => {
        const transaction = await tx.transaction.create({
          data: {
            portfolioId: input.portfolioId,
            investmentId: input.investmentId,
            type: input.type,
            quantity: qty.toNumber(),
            price: price.toNumber(),
            amount: amount.toNumber(),
            currency: input.currency ?? "USD",
            status: TransactionStatus.PENDING,
          },
        });

        switch (input.type) {
          case TransactionType.BUY:
            await this.processBuy(tx, input.portfolioId, input.investmentId, qty, amount);
            break;
          case TransactionType.SELL:
            await this.processSell(tx, input.portfolioId, input.investmentId, qty, amount);
            break;
          case TransactionType.TRANSFER:
            throw new Error("Transfer processing not implemented");
          case TransactionType.FEE:
            await this.processFee(tx, input.portfolioId, input.investmentId, amount);
            break;
        }

        await tx.transaction.update({
          where: { id: transaction.id },
          data: {
            status: TransactionStatus.DONE,
            processDate: new Date(),
          },
        });

        return { success: true, transactionId: transaction.id };
      });
    } catch (err) {
      const message = err instanceof Error ? err.message : "Unknown error";
      return { success: false, error: message };
    }
  }

  /** 2100-VALIDATE-TRANSACTION: check required fields and amounts */
  private validateInput(
    input: TransactionInput,
    qty: Decimal,
    price: Decimal,
    amount: Decimal,
  ): string | null {
    if (!input.portfolioId || input.portfolioId.trim() === "") {
      return "Portfolio ID is required";
    }
    if (!["BUY", "SELL", "TRANSFER", "FEE"].includes(input.type)) {
      return `Invalid Transaction Type: ${input.type}`;
    }
    if (qty.lte(0)) {
      return "Quantity must be greater than zero";
    }
    if (price.lte(0) && input.type !== TransactionType.TRANSFER) {
      return "Price must be greater than zero";
    }
    if (amount.lte(0) && input.type !== TransactionType.TRANSFER) {
      return "Amount must be greater than zero";
    }
    return null;
  }

  /** 2210-PROCESS-BUY: add quantity and cost to position */
  private async processBuy(
    tx: Parameters<Parameters<PrismaClient["$transaction"]>[0]>[0],
    portfolioId: string,
    investmentId: string,
    quantity: Decimal,
    amount: Decimal,
  ) {
    const existing = await (tx as PrismaClient).position.findUnique({
      where: { portfolioId_investmentId: { portfolioId, investmentId } },
    });

    if (existing) {
      await (tx as PrismaClient).position.update({
        where: { id: existing.id },
        data: {
          quantity: new Decimal(existing.quantity.toString()).plus(quantity).toNumber(),
          costBasis: new Decimal(existing.costBasis.toString()).plus(amount).toNumber(),
        },
      });
    } else {
      await (tx as PrismaClient).position.create({
        data: {
          portfolioId,
          investmentId,
          quantity: quantity.toNumber(),
          costBasis: amount.toNumber(),
          marketValue: amount.toNumber(),
          currency: "USD",
        },
      });
    }

    await (tx as PrismaClient).portfolio.update({
      where: { id: portfolioId },
      data: {
        totalValue: { increment: amount.toNumber() },
        lastTransDate: new Date(),
      },
    });
  }

  /** 2220-PROCESS-SELL: subtract quantity and cost, check sufficiency */
  private async processSell(
    tx: Parameters<Parameters<PrismaClient["$transaction"]>[0]>[0],
    portfolioId: string,
    investmentId: string,
    quantity: Decimal,
    amount: Decimal,
  ) {
    const position = await (tx as PrismaClient).position.findUnique({
      where: { portfolioId_investmentId: { portfolioId, investmentId } },
    });

    if (!position) {
      throw new Error("Position not found for sell");
    }

    const currentQty = new Decimal(position.quantity.toString());
    if (currentQty.lt(quantity)) {
      throw new Error("Insufficient units for sale");
    }

    await (tx as PrismaClient).position.update({
      where: { id: position.id },
      data: {
        quantity: currentQty.minus(quantity).toNumber(),
        costBasis: new Decimal(position.costBasis.toString()).minus(amount).toNumber(),
      },
    });

    await (tx as PrismaClient).portfolio.update({
      where: { id: portfolioId },
      data: {
        totalValue: { decrement: amount.toNumber() },
        lastTransDate: new Date(),
      },
    });
  }

  /** 2240-PROCESS-FEE: deduct amount from cost basis */
  private async processFee(
    tx: Parameters<Parameters<PrismaClient["$transaction"]>[0]>[0],
    portfolioId: string,
    investmentId: string,
    amount: Decimal,
  ) {
    const position = await (tx as PrismaClient).position.findUnique({
      where: { portfolioId_investmentId: { portfolioId, investmentId } },
    });

    if (position) {
      await (tx as PrismaClient).position.update({
        where: { id: position.id },
        data: {
          costBasis: new Decimal(position.costBasis.toString()).minus(amount).toNumber(),
        },
      });
    }

    await (tx as PrismaClient).portfolio.update({
      where: { id: portfolioId },
      data: {
        totalValue: { decrement: amount.toNumber() },
        lastTransDate: new Date(),
      },
    });
  }
}
