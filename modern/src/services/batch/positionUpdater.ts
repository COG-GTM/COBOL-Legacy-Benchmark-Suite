/**
 * Batch Position Updater — migrated from POSUPDT.cbl / PORTTRAN.cbl 2200-UPDATE-POSITIONS
 *
 * Recalculates positions for a portfolio after batch transactions:
 *  - Aggregates all DONE transactions per investment
 *  - Updates position quantity and cost basis
 *  - Handles BUY (add), SELL (subtract), FEE (deduct cost)
 *  - Updates portfolio total value
 */

import { PrismaClient, TransactionType, TransactionStatus } from "@prisma/client";
import Decimal from "decimal.js";

export interface PositionUpdateResult {
  portfolioId: string;
  positionsUpdated: number;
  totalValue: Decimal;
  errors: string[];
}

export class PositionUpdater {
  constructor(private readonly prisma: PrismaClient) {}

  async updatePositions(portfolioId: string): Promise<PositionUpdateResult> {
    const errors: string[] = [];
    let positionsUpdated = 0;

    const portfolio = await this.prisma.portfolio.findUnique({
      where: { id: portfolioId },
    });
    if (!portfolio) {
      return {
        portfolioId,
        positionsUpdated: 0,
        totalValue: new Decimal(0),
        errors: [`Portfolio not found: ${portfolioId}`],
      };
    }

    const pendingTransactions = await this.prisma.transaction.findMany({
      where: {
        portfolioId,
        status: TransactionStatus.PENDING,
      },
      orderBy: { createdAt: "asc" },
    });

    for (const txn of pendingTransactions) {
      try {
        await this.applyTransaction(txn);
        positionsUpdated++;
      } catch (err) {
        const msg = err instanceof Error ? err.message : "Unknown error";
        errors.push(`Transaction ${txn.id}: ${msg}`);

        await this.prisma.transaction.update({
          where: { id: txn.id },
          data: { status: TransactionStatus.FAILED },
        });
      }
    }

    // Recalculate portfolio total value from all active positions
    const positions = await this.prisma.position.findMany({
      where: { portfolioId, status: "ACTIVE" },
    });

    const totalValue = positions.reduce(
      (sum, pos) => sum.plus(new Decimal(pos.marketValue.toString())),
      new Decimal(0),
    );

    await this.prisma.portfolio.update({
      where: { id: portfolioId },
      data: {
        totalValue: totalValue.toNumber(),
        lastTransDate: new Date(),
      },
    });

    return { portfolioId, positionsUpdated, totalValue, errors };
  }

  private async applyTransaction(txn: {
    id: string;
    portfolioId: string;
    investmentId: string;
    type: TransactionType;
    quantity: Decimal | { toString(): string };
    price: Decimal | { toString(): string };
    amount: Decimal | { toString(): string };
  }) {
    const qty = new Decimal(txn.quantity.toString());
    const amount = new Decimal(txn.amount.toString());

    switch (txn.type) {
      case TransactionType.BUY:
        await this.applyBuy(txn.portfolioId, txn.investmentId, qty, amount);
        break;
      case TransactionType.SELL:
        await this.applySell(txn.portfolioId, txn.investmentId, qty, amount);
        break;
      case TransactionType.FEE:
        await this.applyFee(txn.portfolioId, txn.investmentId, amount);
        break;
      case TransactionType.TRANSFER:
        throw new Error("Transfer processing not implemented");
    }

    await this.prisma.transaction.update({
      where: { id: txn.id },
      data: {
        status: TransactionStatus.DONE,
        processDate: new Date(),
      },
    });
  }

  private async applyBuy(
    portfolioId: string,
    investmentId: string,
    quantity: Decimal,
    amount: Decimal,
  ) {
    const existing = await this.prisma.position.findUnique({
      where: { portfolioId_investmentId: { portfolioId, investmentId } },
    });

    if (existing) {
      await this.prisma.position.update({
        where: { id: existing.id },
        data: {
          quantity: new Decimal(existing.quantity.toString()).plus(quantity).toNumber(),
          costBasis: new Decimal(existing.costBasis.toString()).plus(amount).toNumber(),
          marketValue: new Decimal(existing.marketValue.toString()).plus(amount).toNumber(),
        },
      });
    } else {
      await this.prisma.position.create({
        data: {
          portfolioId,
          investmentId,
          quantity: quantity.toNumber(),
          costBasis: amount.toNumber(),
          marketValue: amount.toNumber(),
        },
      });
    }
  }

  private async applySell(
    portfolioId: string,
    investmentId: string,
    quantity: Decimal,
    amount: Decimal,
  ) {
    const position = await this.prisma.position.findUnique({
      where: { portfolioId_investmentId: { portfolioId, investmentId } },
    });

    if (!position) {
      throw new Error("Position not found for sell");
    }

    const currentQty = new Decimal(position.quantity.toString());
    if (currentQty.lt(quantity)) {
      throw new Error("Insufficient units for sale");
    }

    const newQty = currentQty.minus(quantity);
    await this.prisma.position.update({
      where: { id: position.id },
      data: {
        quantity: newQty.toNumber(),
        costBasis: new Decimal(position.costBasis.toString()).minus(amount).toNumber(),
        marketValue: new Decimal(position.marketValue.toString()).minus(amount).toNumber(),
        status: newQty.isZero() ? "CLOSED" : "ACTIVE",
      },
    });
  }

  private async applyFee(
    portfolioId: string,
    investmentId: string,
    amount: Decimal,
  ) {
    const position = await this.prisma.position.findUnique({
      where: { portfolioId_investmentId: { portfolioId, investmentId } },
    });

    if (position) {
      await this.prisma.position.update({
        where: { id: position.id },
        data: {
          costBasis: new Decimal(position.costBasis.toString()).minus(amount).toNumber(),
        },
      });
    }
  }
}
