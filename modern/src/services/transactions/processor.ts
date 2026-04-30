/**
 * Transaction Processor
 * Ported from: src/programs/portfolio/PORTTRAN.cbl
 *
 * Processes portfolio transactions: Buy (BU), Sell (SL), Transfer (TR), Fee (FE).
 * Sets transaction status: P=Pending -> D=Done or F=Failed.
 */

import { Prisma } from "@prisma/client";
import { Decimal } from "decimal.js";
import { prisma } from "../../lib/prisma";
import {
  TransactionType,
  TransactionStatus,
  PositionStatus,
  ReturnCode,
  type TransactionInput,
  type TransactionResult,
} from "../../types";

export class TransactionProcessor {
  private counters = {
    read: 0,
    processed: 0,
    errors: 0,
  };

  /**
   * Process a single transaction.
   * Mirrors 2000-PROCESS-TRANSACTIONS in PORTTRAN.cbl.
   */
  async processTransaction(
    input: TransactionInput
  ): Promise<TransactionResult> {
    this.counters.read++;

    const transaction = await prisma.transaction.create({
      data: {
        trnDate: new Date(),
        trnTime: new Date().toISOString().slice(11, 17).replace(/:/g, ""),
        portfolioId: input.portfolioId,
        sequenceNo: this.generateSequenceNo(),
        investmentId: input.investmentId,
        type: input.type,
        quantity: new Prisma.Decimal(input.quantity.toString()),
        price: new Prisma.Decimal(input.price.toString()),
        amount: new Prisma.Decimal(input.amount.toString()),
        currency: input.currency ?? "USD",
        status: TransactionStatus.PENDING,
        targetPortfolioId: input.targetPortfolioId,
      },
    });

    try {
      const result = await prisma.$transaction(async (tx) => {
        const portfolio = await tx.portfolio.findUnique({
          where: { portfolioId: input.portfolioId },
        });

        if (!portfolio) {
          throw new TransactionError(
            `Invalid Portfolio ID: ${input.portfolioId}`,
            ReturnCode.ERROR
          );
        }

        if (portfolio.status !== "A") {
          throw new TransactionError(
            `Portfolio ${input.portfolioId} is not active`,
            ReturnCode.ERROR
          );
        }

        let gainLoss: Decimal | undefined;

        switch (input.type) {
          case TransactionType.BUY:
            await this.processBuy(tx, input, portfolio);
            break;
          case TransactionType.SELL:
            gainLoss = await this.processSell(tx, input, portfolio);
            break;
          case TransactionType.TRANSFER:
            await this.processTransfer(tx, input, portfolio);
            break;
          case TransactionType.FEE:
            await this.processFee(tx, input, portfolio);
            break;
          default:
            throw new TransactionError(
              `Invalid Transaction Type: ${input.type}`,
              ReturnCode.ERROR
            );
        }

        await tx.transaction.update({
          where: { id: transaction.id },
          data: {
            status: TransactionStatus.DONE,
            processDate: new Date(),
            processUser: "SYSTEM",
          },
        });

        this.counters.processed++;
        return { gainLoss };
      });

      return {
        transactionId: transaction.id,
        status: TransactionStatus.DONE,
        returnCode: ReturnCode.SUCCESS,
        message: "Transaction processed successfully",
        gainLoss: result.gainLoss,
      };
    } catch (error) {
      this.counters.errors++;

      await prisma.transaction.update({
        where: { id: transaction.id },
        data: {
          status: TransactionStatus.FAILED,
          processDate: new Date(),
          processUser: "SYSTEM",
        },
      });

      const message =
        error instanceof TransactionError
          ? error.message
          : "Unexpected error processing transaction";
      const returnCode =
        error instanceof TransactionError
          ? error.returnCode
          : ReturnCode.SEVERE;

      return {
        transactionId: transaction.id,
        status: TransactionStatus.FAILED,
        returnCode,
        message,
      };
    }
  }

  /**
   * Buy transaction: validate funds, debit cash balance, create/increase position, calculate cost basis.
   * Mirrors 2210-PROCESS-BUY in PORTTRAN.cbl.
   */
  private async processBuy(
    tx: Prisma.TransactionClient,
    input: TransactionInput,
    portfolio: { id: string; portfolioId: string; cashBalance: Prisma.Decimal }
  ): Promise<void> {
    const totalCost = input.amount;
    const currentCash = new Decimal(portfolio.cashBalance.toString());

    if (currentCash.lessThan(totalCost)) {
      throw new TransactionError(
        `Insufficient funds: available=${currentCash.toFixed(2)}, required=${totalCost.toFixed(2)}`,
        ReturnCode.ERROR
      );
    }

    await tx.portfolio.update({
      where: { portfolioId: input.portfolioId },
      data: {
        cashBalance: new Prisma.Decimal(
          currentCash.minus(totalCost).toFixed(2)
        ),
        lastMaint: new Date(),
        lastUser: "SYSTEM",
        lastTrans: new Date(),
      },
    });

    const existingPosition = await tx.position.findUnique({
      where: {
        portfolioId_investmentId: {
          portfolioId: input.portfolioId,
          investmentId: input.investmentId,
        },
      },
    });

    if (existingPosition) {
      const newQuantity = new Decimal(
        existingPosition.quantity.toString()
      ).plus(input.quantity);
      const newCostBasis = new Decimal(
        existingPosition.costBasis.toString()
      ).plus(totalCost);

      await tx.position.update({
        where: { id: existingPosition.id },
        data: {
          quantity: new Prisma.Decimal(newQuantity.toFixed(4)),
          costBasis: new Prisma.Decimal(newCostBasis.toFixed(2)),
          marketValue: new Prisma.Decimal(
            newQuantity.times(input.price).toFixed(2)
          ),
          status: PositionStatus.ACTIVE,
          lastMaintDate: new Date(),
          lastMaintUser: "SYSTEM",
        },
      });
    } else {
      await tx.position.create({
        data: {
          portfolioId: input.portfolioId,
          posDate: new Date(),
          investmentId: input.investmentId,
          quantity: new Prisma.Decimal(input.quantity.toFixed(4)),
          costBasis: new Prisma.Decimal(totalCost.toFixed(2)),
          marketValue: new Prisma.Decimal(
            input.quantity.times(input.price).toFixed(2)
          ),
          currency: input.currency ?? "USD",
          status: PositionStatus.ACTIVE,
          lastMaintDate: new Date(),
          lastMaintUser: "SYSTEM",
        },
      });
    }
  }

  /**
   * Sell transaction: validate position, credit cash, calculate gain/loss.
   * Mirrors 2220-PROCESS-SELL in PORTTRAN.cbl.
   */
  private async processSell(
    tx: Prisma.TransactionClient,
    input: TransactionInput,
    portfolio: { id: string; portfolioId: string; cashBalance: Prisma.Decimal }
  ): Promise<Decimal> {
    const position = await tx.position.findUnique({
      where: {
        portfolioId_investmentId: {
          portfolioId: input.portfolioId,
          investmentId: input.investmentId,
        },
      },
    });

    if (!position || position.status !== PositionStatus.ACTIVE) {
      throw new TransactionError(
        `No active position found for ${input.investmentId} in portfolio ${input.portfolioId}`,
        ReturnCode.ERROR
      );
    }

    const currentQuantity = new Decimal(position.quantity.toString());
    if (currentQuantity.lessThan(input.quantity)) {
      throw new TransactionError(
        `Insufficient units for sale: available=${currentQuantity.toFixed(4)}, requested=${input.quantity.toFixed(4)}`,
        ReturnCode.ERROR
      );
    }

    const currentCostBasis = new Decimal(position.costBasis.toString());
    const avgCostPerUnit = currentQuantity.isZero()
      ? new Decimal(0)
      : currentCostBasis.dividedBy(currentQuantity);
    const costOfSoldUnits = avgCostPerUnit.times(input.quantity);
    const gainLoss = input.amount.minus(costOfSoldUnits);

    const newQuantity = currentQuantity.minus(input.quantity);
    const newCostBasis = currentCostBasis.minus(costOfSoldUnits);

    if (newQuantity.isZero()) {
      await tx.position.update({
        where: { id: position.id },
        data: {
          quantity: new Prisma.Decimal("0"),
          costBasis: new Prisma.Decimal("0"),
          marketValue: new Prisma.Decimal("0"),
          status: PositionStatus.CLOSED,
          lastMaintDate: new Date(),
          lastMaintUser: "SYSTEM",
        },
      });
    } else {
      await tx.position.update({
        where: { id: position.id },
        data: {
          quantity: new Prisma.Decimal(newQuantity.toFixed(4)),
          costBasis: new Prisma.Decimal(newCostBasis.toFixed(2)),
          marketValue: new Prisma.Decimal(
            newQuantity.times(input.price).toFixed(2)
          ),
          lastMaintDate: new Date(),
          lastMaintUser: "SYSTEM",
        },
      });
    }

    const currentCash = new Decimal(portfolio.cashBalance.toString());
    await tx.portfolio.update({
      where: { portfolioId: input.portfolioId },
      data: {
        cashBalance: new Prisma.Decimal(
          currentCash.plus(input.amount).toFixed(2)
        ),
        lastMaint: new Date(),
        lastUser: "SYSTEM",
        lastTrans: new Date(),
      },
    });

    return gainLoss;
  }

  /**
   * Transfer: move positions between portfolios.
   * Mirrors 2230-PROCESS-TRANSFER in PORTTRAN.cbl (was not implemented in COBOL).
   */
  private async processTransfer(
    tx: Prisma.TransactionClient,
    input: TransactionInput,
    portfolio: { id: string; portfolioId: string }
  ): Promise<void> {
    if (!input.targetPortfolioId) {
      throw new TransactionError(
        "Target portfolio ID required for transfer",
        ReturnCode.ERROR
      );
    }

    const targetPortfolio = await tx.portfolio.findUnique({
      where: { portfolioId: input.targetPortfolioId },
    });

    if (!targetPortfolio) {
      throw new TransactionError(
        `Target portfolio not found: ${input.targetPortfolioId}`,
        ReturnCode.ERROR
      );
    }

    if (targetPortfolio.status !== "A") {
      throw new TransactionError(
        `Target portfolio ${input.targetPortfolioId} is not active`,
        ReturnCode.ERROR
      );
    }

    const sourcePosition = await tx.position.findUnique({
      where: {
        portfolioId_investmentId: {
          portfolioId: input.portfolioId,
          investmentId: input.investmentId,
        },
      },
    });

    if (!sourcePosition || sourcePosition.status !== PositionStatus.ACTIVE) {
      throw new TransactionError(
        `No active position for ${input.investmentId} in source portfolio`,
        ReturnCode.ERROR
      );
    }

    const sourceQty = new Decimal(sourcePosition.quantity.toString());
    if (sourceQty.lessThan(input.quantity)) {
      throw new TransactionError(
        `Insufficient units for transfer: available=${sourceQty.toFixed(4)}, requested=${input.quantity.toFixed(4)}`,
        ReturnCode.ERROR
      );
    }

    const sourceCostBasis = new Decimal(sourcePosition.costBasis.toString());
    const avgCost = sourceQty.isZero()
      ? new Decimal(0)
      : sourceCostBasis.dividedBy(sourceQty);
    const transferCostBasis = avgCost.times(input.quantity);

    const newSourceQty = sourceQty.minus(input.quantity);
    const newSourceCostBasis = sourceCostBasis.minus(transferCostBasis);

    if (newSourceQty.isZero()) {
      await tx.position.update({
        where: { id: sourcePosition.id },
        data: {
          quantity: new Prisma.Decimal("0"),
          costBasis: new Prisma.Decimal("0"),
          marketValue: new Prisma.Decimal("0"),
          status: PositionStatus.CLOSED,
          lastMaintDate: new Date(),
          lastMaintUser: "SYSTEM",
        },
      });
    } else {
      await tx.position.update({
        where: { id: sourcePosition.id },
        data: {
          quantity: new Prisma.Decimal(newSourceQty.toFixed(4)),
          costBasis: new Prisma.Decimal(newSourceCostBasis.toFixed(2)),
          marketValue: new Prisma.Decimal(
            newSourceQty.times(input.price).toFixed(2)
          ),
          lastMaintDate: new Date(),
          lastMaintUser: "SYSTEM",
        },
      });
    }

    const targetPosition = await tx.position.findUnique({
      where: {
        portfolioId_investmentId: {
          portfolioId: input.targetPortfolioId,
          investmentId: input.investmentId,
        },
      },
    });

    if (targetPosition) {
      const targetQty = new Decimal(targetPosition.quantity.toString());
      const targetCost = new Decimal(targetPosition.costBasis.toString());

      await tx.position.update({
        where: { id: targetPosition.id },
        data: {
          quantity: new Prisma.Decimal(
            targetQty.plus(input.quantity).toFixed(4)
          ),
          costBasis: new Prisma.Decimal(
            targetCost.plus(transferCostBasis).toFixed(2)
          ),
          marketValue: new Prisma.Decimal(
            targetQty.plus(input.quantity).times(input.price).toFixed(2)
          ),
          status: PositionStatus.ACTIVE,
          lastMaintDate: new Date(),
          lastMaintUser: "SYSTEM",
        },
      });
    } else {
      await tx.position.create({
        data: {
          portfolioId: input.targetPortfolioId,
          posDate: new Date(),
          investmentId: input.investmentId,
          quantity: new Prisma.Decimal(input.quantity.toFixed(4)),
          costBasis: new Prisma.Decimal(transferCostBasis.toFixed(2)),
          marketValue: new Prisma.Decimal(
            input.quantity.times(input.price).toFixed(2)
          ),
          currency: input.currency ?? "USD",
          status: PositionStatus.ACTIVE,
          lastMaintDate: new Date(),
          lastMaintUser: "SYSTEM",
        },
      });
    }
  }

  /**
   * Fee transaction: debit fee amount from cash balance.
   * Mirrors 2240-PROCESS-FEE in PORTTRAN.cbl.
   */
  private async processFee(
    tx: Prisma.TransactionClient,
    input: TransactionInput,
    portfolio: { id: string; portfolioId: string; cashBalance: Prisma.Decimal }
  ): Promise<void> {
    const currentCash = new Decimal(portfolio.cashBalance.toString());
    const feeAmount = input.amount;

    if (currentCash.lessThan(feeAmount)) {
      throw new TransactionError(
        `Insufficient funds for fee: available=${currentCash.toFixed(2)}, fee=${feeAmount.toFixed(2)}`,
        ReturnCode.ERROR
      );
    }

    await tx.portfolio.update({
      where: { portfolioId: input.portfolioId },
      data: {
        cashBalance: new Prisma.Decimal(
          currentCash.minus(feeAmount).toFixed(2)
        ),
        lastMaint: new Date(),
        lastUser: "SYSTEM",
        lastTrans: new Date(),
      },
    });
  }

  private generateSequenceNo(): string {
    return String(Date.now() % 1000000).padStart(6, "0");
  }

  getCounters() {
    return { ...this.counters };
  }
}

class TransactionError extends Error {
  constructor(
    message: string,
    public readonly returnCode: ReturnCode
  ) {
    super(message);
    this.name = "TransactionError";
  }
}
