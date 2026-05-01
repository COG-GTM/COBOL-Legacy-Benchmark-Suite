import { PrismaClient } from '@prisma/client';
import Decimal from 'decimal.js';
import {
  TransactionType,
  AuditRecordType,
  AuditActionCode,
} from '../types/index.js';
import { NotFoundError } from '../utils/errors.js';
import { formatTimeCOBOL, generateAuditSeqNo } from '../utils/helpers.js';

const prisma = new PrismaClient();

// INQPORT.cbl — P200-GET-POSITION: retrieve current positions
export async function getPositions(portfolioId: string) {
  const portfolio = await prisma.portfolio.findUnique({
    where: { portfolioId },
  });

  if (!portfolio) {
    throw new NotFoundError('Portfolio', portfolioId);
  }

  // Get latest position for each investment
  const positions = await prisma.investmentPosition.findMany({
    where: { portfolioId },
    orderBy: [{ investmentId: 'asc' }, { positionDate: 'desc' }],
  });

  // Deduplicate: keep only latest position per investment
  const latestPositions = new Map<string, typeof positions[0]>();
  for (const pos of positions) {
    if (!latestPositions.has(pos.investmentId)) {
      latestPositions.set(pos.investmentId, pos);
    }
  }

  return Array.from(latestPositions.values());
}

// POSUPD00.cbl — Position update logic
// Recalculate cost basis on buys, calculate gain/loss on sells
export async function updatePositionFromTransaction(
  portfolioId: string,
  investmentId: string,
  transactionType: string,
  quantity: number,
  price: number,
  userId: string
) {
  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());

  // Get current position
  const existingPositions = await prisma.investmentPosition.findMany({
    where: { portfolioId, investmentId },
    orderBy: { positionDate: 'desc' },
    take: 1,
  });

  const currentPosition = existingPositions[0];
  const currentQty = currentPosition
    ? new Decimal(currentPosition.quantity.toString())
    : new Decimal(0);
  const currentCostBasis = currentPosition
    ? new Decimal(currentPosition.costBasis.toString())
    : new Decimal(0);

  let newQuantity: Decimal;
  let newCostBasis: Decimal;
  let gainLoss = new Decimal(0);

  if (transactionType === TransactionType.Buy) {
    // Buy: add quantity, recalculate cost basis (weighted average)
    newQuantity = currentQty.plus(quantity);
    newCostBasis = currentCostBasis.plus(
      new Decimal(quantity).times(price)
    );
  } else if (transactionType === TransactionType.Sell) {
    // Sell: reduce quantity, calculate gain/loss
    newQuantity = currentQty.minus(quantity);
    const avgCostPerUnit = currentQty.greaterThan(0)
      ? currentCostBasis.dividedBy(currentQty)
      : new Decimal(0);
    const costOfSold = avgCostPerUnit.times(quantity);
    newCostBasis = currentCostBasis.minus(costOfSold);
    gainLoss = new Decimal(quantity).times(price).minus(costOfSold);
  } else if (transactionType === TransactionType.Transfer) {
    newQuantity = currentQty.plus(quantity);
    newCostBasis = currentCostBasis.plus(
      new Decimal(quantity).times(price)
    );
  } else {
    // Fee: no position change
    newQuantity = currentQty;
    newCostBasis = currentCostBasis;
  }

  const newMarketValue = newQuantity.times(price);

  const beforeImage = currentPosition ? JSON.stringify(currentPosition) : null;

  // Create new position snapshot
  const position = await prisma.investmentPosition.upsert({
    where: {
      portfolioId_investmentId_positionDate: {
        portfolioId,
        investmentId,
        positionDate: today,
      },
    },
    update: {
      quantity: newQuantity.toNumber(),
      costBasis: newCostBasis.toNumber(),
      marketValue: newMarketValue.toNumber(),
      lastMaintDate: now,
      lastMaintUser: userId,
    },
    create: {
      portfolioId,
      investmentId,
      positionDate: today,
      quantity: newQuantity.toNumber(),
      costBasis: newCostBasis.toNumber(),
      marketValue: newMarketValue.toNumber(),
      currencyCode: 'USD',
      lastMaintDate: now,
      lastMaintUser: userId,
    },
  });

  // Update portfolio total value
  const allPositions = await prisma.investmentPosition.findMany({
    where: { portfolioId },
    orderBy: [{ investmentId: 'asc' }, { positionDate: 'desc' }],
  });

  // Deduplicate to latest per investment
  const latestMap = new Map<string, Decimal>();
  for (const p of allPositions) {
    if (!latestMap.has(p.investmentId)) {
      latestMap.set(p.investmentId, new Decimal(p.marketValue.toString()));
    }
  }
  const totalValue = Array.from(latestMap.values()).reduce(
    (sum, val) => sum.plus(val),
    new Decimal(0)
  );

  await prisma.portfolio.update({
    where: { portfolioId },
    data: {
      totalValue: totalValue.toNumber(),
      lastMaintDate: now,
      lastMaintUser: userId,
    },
  });

  // Position history record (POSHIST table)
  await prisma.positionHistory.create({
    data: {
      accountNo: portfolioId.substring(0, 8).padEnd(8, ' '),
      portfolioId: portfolioId.padEnd(10, ' '),
      transDate: today,
      transTime: formatTimeCOBOL(now),
      transType: transactionType,
      securityId: investmentId.padEnd(12, ' '),
      quantity: new Decimal(quantity).toNumber(),
      price,
      amount: new Decimal(quantity).times(price).toNumber(),
      fees: 0,
      totalAmount: new Decimal(quantity).times(price).toNumber(),
      costBasis: newCostBasis.toNumber(),
      gainLoss: gainLoss.toNumber(),
      processDate: today,
      processTime: formatTimeCOBOL(now),
      programId: 'POSUPD00',
      userId,
    },
  });

  // Audit trail
  await prisma.auditLog.create({
    data: {
      portfolioId,
      date: now,
      time: formatTimeCOBOL(now),
      seqNo: generateAuditSeqNo(),
      recordType: AuditRecordType.Position,
      actionCode: beforeImage ? AuditActionCode.Change : AuditActionCode.Add,
      beforeImage,
      afterImage: JSON.stringify(position),
      processDate: now,
      processUser: userId,
    },
  });

  return { position, gainLoss: gainLoss.toNumber() };
}

// Batch position update — for multiple positions at once
export async function batchUpdatePositions(
  portfolioId: string,
  updates: Array<{
    investmentId: string;
    marketValue: number;
  }>,
  userId: string
) {
  const results = [];
  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());

  for (const update of updates) {
    const position = await prisma.investmentPosition.upsert({
      where: {
        portfolioId_investmentId_positionDate: {
          portfolioId,
          investmentId: update.investmentId,
          positionDate: today,
        },
      },
      update: {
        marketValue: update.marketValue,
        lastMaintDate: now,
        lastMaintUser: userId,
      },
      create: {
        portfolioId,
        investmentId: update.investmentId,
        positionDate: today,
        quantity: 0,
        costBasis: 0,
        marketValue: update.marketValue,
        currencyCode: 'USD',
        lastMaintDate: now,
        lastMaintUser: userId,
      },
    });
    results.push(position);
  }

  return results;
}
