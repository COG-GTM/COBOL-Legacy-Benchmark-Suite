/**
 * Position Updater (Batch)
 * Ported from: src/programs/batch/POSUPD00.cbl (referenced in system-architecture.md)
 * and position update logic from src/programs/portfolio/PORTTRAN.cbl (2200-2240)
 *
 * Processes validated transactions to update position records.
 * Recalculates cost basis, market value, quantity for each position.
 * Handles position creation (first buy) and position closure (full sell).
 */

import { Prisma } from "@prisma/client";
import { Decimal } from "decimal.js";
import { prisma } from "../../lib/prisma";
import { CheckpointManager } from "../../lib/checkpoint";
import {
  TransactionType,
  TransactionStatus,
  PositionStatus,
  HistoryActionCode,
  CheckpointPhase,
  ReturnCode,
  type TransactionInput,
  type PositionUpdate,
  type CheckpointState,
  type BatchStepResult,
} from "../../types";

export interface PositionUpdaterResult {
  returnCode: number;
  positionUpdates: PositionUpdate[];
  recordsProcessed: number;
  errorsEncountered: number;
}

/**
 * Process validated transactions and update positions.
 * Mirrors POSUPD00 pipeline step.
 */
export async function updatePositions(
  transactions: TransactionInput[],
  runDate: Date
): Promise<PositionUpdaterResult> {
  const checkpoint = new CheckpointManager("POSUPD00", runDate);
  const resumeState = await checkpoint.initialize();

  const startIndex = resumeState?.recordsProcessed ?? 0;
  const positionUpdates: PositionUpdate[] = [];
  let recordsProcessed = resumeState?.recordsProcessed ?? 0;
  let errorsEncountered = resumeState?.recordsError ?? 0;

  const state: CheckpointState = {
    programId: "POSUPD00",
    recordsRead: startIndex,
    recordsProcessed,
    recordsError: errorsEncountered,
    lastKey: resumeState?.lastKey ?? null,
    phase: CheckpointPhase.PROCESS,
  };

  for (let i = startIndex; i < transactions.length; i++) {
    const txn = transactions[i];
    state.recordsRead = i + 1;

    try {
      const update = await processPositionUpdate(txn);
      if (update) {
        positionUpdates.push(update);
      }
      recordsProcessed++;
      state.recordsProcessed = recordsProcessed;
      state.lastKey = `${txn.portfolioId}:${txn.investmentId}`;
    } catch (error) {
      errorsEncountered++;
      state.recordsError = errorsEncountered;
    }

    const shouldContinue = await checkpoint.takeCheckpoint(state);
    if (!shouldContinue) {
      break;
    }
  }

  const returnCode =
    errorsEncountered === 0
      ? ReturnCode.SUCCESS
      : errorsEncountered <= transactions.length * 0.1
        ? ReturnCode.WARNING
        : ReturnCode.ERROR;

  if (returnCode <= ReturnCode.WARNING) {
    await checkpoint.markComplete(state);
  } else {
    await checkpoint.markFailed(state, "Too many errors during position update");
  }

  return {
    returnCode,
    positionUpdates,
    recordsProcessed,
    errorsEncountered,
  };
}

/**
 * Process a single transaction's effect on positions.
 */
async function processPositionUpdate(
  txn: TransactionInput
): Promise<PositionUpdate | null> {
  return await prisma.$transaction(async (tx) => {
    switch (txn.type) {
      case TransactionType.BUY:
        return await handleBuyPosition(tx, txn);
      case TransactionType.SELL:
        return await handleSellPosition(tx, txn);
      case TransactionType.TRANSFER:
        return await handleTransferPosition(tx, txn);
      case TransactionType.FEE:
        return null; // Fees don't affect positions
      default:
        throw new Error(`Unknown transaction type: ${txn.type}`);
    }
  });
}

/**
 * Handle buy: create position (first buy) or increase quantity.
 */
async function handleBuyPosition(
  tx: Prisma.TransactionClient,
  txn: TransactionInput
): Promise<PositionUpdate> {
  const existing = await tx.position.findUnique({
    where: {
      portfolioId_investmentId: {
        portfolioId: txn.portfolioId,
        investmentId: txn.investmentId,
      },
    },
  });

  if (existing && existing.status === PositionStatus.ACTIVE) {
    const oldQty = new Decimal(existing.quantity.toString());
    const oldCost = new Decimal(existing.costBasis.toString());
    const newQty = oldQty.plus(txn.quantity);
    const newCost = oldCost.plus(txn.amount);
    const newMarketValue = newQty.times(txn.price);

    await tx.position.update({
      where: { id: existing.id },
      data: {
        quantity: new Prisma.Decimal(newQty.toFixed(4)),
        costBasis: new Prisma.Decimal(newCost.toFixed(2)),
        marketValue: new Prisma.Decimal(newMarketValue.toFixed(2)),
        lastMaintDate: new Date(),
        lastMaintUser: "BATCH",
      },
    });

    return {
      portfolioId: txn.portfolioId,
      investmentId: txn.investmentId,
      quantityDelta: txn.quantity,
      costBasisDelta: txn.amount,
      marketValueDelta: newMarketValue.minus(
        new Decimal(existing.marketValue.toString())
      ),
      actionCode: HistoryActionCode.CHANGE,
    };
  } else {
    const marketValue = txn.quantity.times(txn.price);

    // Reactivate closed position or create new one
    if (existing) {
      await tx.position.update({
        where: { id: existing.id },
        data: {
          quantity: new Prisma.Decimal(txn.quantity.toFixed(4)),
          costBasis: new Prisma.Decimal(txn.amount.toFixed(2)),
          marketValue: new Prisma.Decimal(marketValue.toFixed(2)),
          status: PositionStatus.ACTIVE,
          posDate: new Date(),
          lastMaintDate: new Date(),
          lastMaintUser: "BATCH",
        },
      });
    } else {
      await tx.position.create({
        data: {
          portfolioId: txn.portfolioId,
          posDate: new Date(),
          investmentId: txn.investmentId,
          quantity: new Prisma.Decimal(txn.quantity.toFixed(4)),
          costBasis: new Prisma.Decimal(txn.amount.toFixed(2)),
          marketValue: new Prisma.Decimal(marketValue.toFixed(2)),
          currency: txn.currency ?? "USD",
          status: PositionStatus.ACTIVE,
          lastMaintDate: new Date(),
          lastMaintUser: "BATCH",
        },
      });
    }

    return {
      portfolioId: txn.portfolioId,
      investmentId: txn.investmentId,
      quantityDelta: txn.quantity,
      costBasisDelta: txn.amount,
      marketValueDelta: marketValue,
      actionCode: HistoryActionCode.ADD,
    };
  }
}

/**
 * Handle sell: reduce quantity, close position if fully sold.
 */
async function handleSellPosition(
  tx: Prisma.TransactionClient,
  txn: TransactionInput
): Promise<PositionUpdate> {
  const position = await tx.position.findUnique({
    where: {
      portfolioId_investmentId: {
        portfolioId: txn.portfolioId,
        investmentId: txn.investmentId,
      },
    },
  });

  if (!position || position.status !== PositionStatus.ACTIVE) {
    throw new Error(
      `No active position for ${txn.investmentId} in ${txn.portfolioId}`
    );
  }

  const currentQty = new Decimal(position.quantity.toString());
  const currentCost = new Decimal(position.costBasis.toString());

  if (currentQty.lessThan(txn.quantity)) {
    throw new Error(
      `Insufficient quantity: have ${currentQty.toFixed(4)}, need ${txn.quantity.toFixed(4)}`
    );
  }

  const avgCost = currentQty.isZero()
    ? new Decimal(0)
    : currentCost.dividedBy(currentQty);
  const costReduction = avgCost.times(txn.quantity);

  const newQty = currentQty.minus(txn.quantity);
  const newCost = currentCost.minus(costReduction);
  const newMarketValue = newQty.times(txn.price);

  const isClosure = newQty.isZero();

  await tx.position.update({
    where: { id: position.id },
    data: {
      quantity: new Prisma.Decimal(newQty.toFixed(4)),
      costBasis: new Prisma.Decimal(newCost.toFixed(2)),
      marketValue: new Prisma.Decimal(newMarketValue.toFixed(2)),
      status: isClosure ? PositionStatus.CLOSED : PositionStatus.ACTIVE,
      lastMaintDate: new Date(),
      lastMaintUser: "BATCH",
    },
  });

  return {
    portfolioId: txn.portfolioId,
    investmentId: txn.investmentId,
    quantityDelta: txn.quantity.negated(),
    costBasisDelta: costReduction.negated(),
    marketValueDelta: newMarketValue.minus(
      new Decimal(position.marketValue.toString())
    ),
    actionCode: isClosure ? HistoryActionCode.DELETE : HistoryActionCode.CHANGE,
  };
}

/**
 * Handle transfer: move position between portfolios.
 */
async function handleTransferPosition(
  tx: Prisma.TransactionClient,
  txn: TransactionInput
): Promise<PositionUpdate> {
  if (!txn.targetPortfolioId) {
    throw new Error("Target portfolio ID required for transfer");
  }

  const sourcePosition = await tx.position.findUnique({
    where: {
      portfolioId_investmentId: {
        portfolioId: txn.portfolioId,
        investmentId: txn.investmentId,
      },
    },
  });

  if (!sourcePosition || sourcePosition.status !== PositionStatus.ACTIVE) {
    throw new Error(
      `No active position for ${txn.investmentId} in source ${txn.portfolioId}`
    );
  }

  const sourceQty = new Decimal(sourcePosition.quantity.toString());
  if (sourceQty.lessThan(txn.quantity)) {
    throw new Error(
      `Insufficient quantity for transfer: have ${sourceQty.toFixed(4)}, need ${txn.quantity.toFixed(4)}`
    );
  }

  const sourceCost = new Decimal(sourcePosition.costBasis.toString());
  const avgCost = sourceQty.isZero()
    ? new Decimal(0)
    : sourceCost.dividedBy(sourceQty);
  const transferCost = avgCost.times(txn.quantity);

  const newSourceQty = sourceQty.minus(txn.quantity);
  const newSourceCost = sourceCost.minus(transferCost);
  const isClosure = newSourceQty.isZero();

  await tx.position.update({
    where: { id: sourcePosition.id },
    data: {
      quantity: new Prisma.Decimal(newSourceQty.toFixed(4)),
      costBasis: new Prisma.Decimal(newSourceCost.toFixed(2)),
      marketValue: new Prisma.Decimal(
        newSourceQty.times(txn.price).toFixed(2)
      ),
      status: isClosure ? PositionStatus.CLOSED : PositionStatus.ACTIVE,
      lastMaintDate: new Date(),
      lastMaintUser: "BATCH",
    },
  });

  const targetPosition = await tx.position.findUnique({
    where: {
      portfolioId_investmentId: {
        portfolioId: txn.targetPortfolioId,
        investmentId: txn.investmentId,
      },
    },
  });

  if (targetPosition) {
    const targetQty = new Decimal(targetPosition.quantity.toString());
    const targetCost = new Decimal(targetPosition.costBasis.toString());
    const newTargetQty = targetQty.plus(txn.quantity);

    await tx.position.update({
      where: { id: targetPosition.id },
      data: {
        quantity: new Prisma.Decimal(newTargetQty.toFixed(4)),
        costBasis: new Prisma.Decimal(
          targetCost.plus(transferCost).toFixed(2)
        ),
        marketValue: new Prisma.Decimal(
          newTargetQty.times(txn.price).toFixed(2)
        ),
        status: PositionStatus.ACTIVE,
        lastMaintDate: new Date(),
        lastMaintUser: "BATCH",
      },
    });
  } else {
    await tx.position.create({
      data: {
        portfolioId: txn.targetPortfolioId,
        posDate: new Date(),
        investmentId: txn.investmentId,
        quantity: new Prisma.Decimal(txn.quantity.toFixed(4)),
        costBasis: new Prisma.Decimal(transferCost.toFixed(2)),
        marketValue: new Prisma.Decimal(
          txn.quantity.times(txn.price).toFixed(2)
        ),
        currency: txn.currency ?? "USD",
        status: PositionStatus.ACTIVE,
        lastMaintDate: new Date(),
        lastMaintUser: "BATCH",
      },
    });
  }

  return {
    portfolioId: txn.portfolioId,
    investmentId: txn.investmentId,
    quantityDelta: txn.quantity.negated(),
    costBasisDelta: transferCost.negated(),
    marketValueDelta: new Decimal(
      sourcePosition.marketValue.toString()
    ).negated(),
    actionCode: isClosure ? HistoryActionCode.DELETE : HistoryActionCode.CHANGE,
  };
}
