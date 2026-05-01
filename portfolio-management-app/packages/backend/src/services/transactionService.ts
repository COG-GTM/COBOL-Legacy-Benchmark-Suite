import { PrismaClient } from '@prisma/client';
import Decimal from 'decimal.js';
import {
  TransactionType,
  TransactionStatus,
  PortfolioStatus,
  AuditRecordType,
  AuditActionCode,
  type CreateTransactionRequest,
} from '../types/index.js';
import { ValidationError, NotFoundError } from '../utils/errors.js';
import {
  generateTransactionId,
  formatTimeSQL,
  formatTimeCOBOL,
  generateAuditSeqNo,
} from '../utils/helpers.js';

const prisma = new PrismaClient();

// TRNVAL00.cbl — Transaction validation logic
async function validateTransaction(data: CreateTransactionRequest) {
  // 1. Verify portfolio exists and is active
  const portfolio = await prisma.portfolio.findUnique({
    where: { portfolioId: data.portfolioId },
  });

  if (!portfolio) {
    throw new NotFoundError('Portfolio', data.portfolioId);
  }

  if (portfolio.status !== PortfolioStatus.Active) {
    throw new ValidationError(
      `Portfolio ${data.portfolioId} is not active (status: ${portfolio.status})`
    );
  }

  // 2. Validate transaction type codes
  const validTypes = Object.values(TransactionType);
  if (!validTypes.includes(data.transactionType as TransactionType)) {
    throw new ValidationError(`Invalid transaction type: ${data.transactionType}`);
  }

  // 3. For sells, check sufficient quantity
  if (data.transactionType === TransactionType.Sell) {
    const positions = await prisma.investmentPosition.findMany({
      where: {
        portfolioId: data.portfolioId,
        investmentId: data.investmentId,
      },
      orderBy: { positionDate: 'desc' },
      take: 1,
    });

    const currentQty = positions.length > 0
      ? new Decimal(positions[0].quantity.toString())
      : new Decimal(0);

    if (currentQty.lessThan(data.quantity)) {
      throw new ValidationError(
        `Insufficient quantity for sell: have ${currentQty}, need ${data.quantity}`
      );
    }
  }

  // 4. Validate amounts
  const amount = new Decimal(data.quantity).times(data.price);
  if (amount.lessThanOrEqualTo(0)) {
    throw new ValidationError('Transaction amount must be positive');
  }

  return { portfolio, amount };
}

// PORTTRAN.cbl + TRNVAL00.cbl — Submit new transaction
export async function createTransaction(
  data: CreateTransactionRequest,
  userId: string
) {
  const { amount } = await validateTransaction(data);
  const now = new Date();
  const transactionId = generateTransactionId();

  // Calculate signed amount based on type
  let signedAmount = amount;
  if (data.transactionType === TransactionType.Sell) {
    signedAmount = amount; // positive for sells (cash inflow)
  } else if (data.transactionType === TransactionType.Buy) {
    signedAmount = amount; // recorded positive, debited from cash
  }

  const transaction = await prisma.transaction.create({
    data: {
      transactionId,
      portfolioId: data.portfolioId,
      transactionDate: now,
      transactionTime: formatTimeSQL(now),
      investmentId: data.investmentId,
      transactionType: data.transactionType,
      quantity: data.quantity,
      price: data.price,
      amount: signedAmount.toNumber(),
      currencyCode: data.currencyCode,
      status: TransactionStatus.Pending,
      processDate: now,
      processUser: userId,
    },
  });

  // Audit trail
  await prisma.auditLog.create({
    data: {
      portfolioId: data.portfolioId,
      date: now,
      time: formatTimeCOBOL(now),
      seqNo: generateAuditSeqNo(),
      recordType: AuditRecordType.Transaction,
      actionCode: AuditActionCode.Add,
      afterImage: JSON.stringify(transaction),
      processDate: now,
      processUser: userId,
    },
  });

  return transaction;
}

// INQHIST.cbl — Get transaction history with pagination
export async function getTransactionHistory(
  portfolioId: string,
  page: number,
  pageSize: number
) {
  // Verify portfolio exists
  const portfolio = await prisma.portfolio.findUnique({
    where: { portfolioId },
  });

  if (!portfolio) {
    throw new NotFoundError('Portfolio', portfolioId);
  }

  const [transactions, total] = await Promise.all([
    prisma.transaction.findMany({
      where: { portfolioId },
      orderBy: { transactionDate: 'desc' },
      skip: (page - 1) * pageSize,
      take: pageSize,
    }),
    prisma.transaction.count({ where: { portfolioId } }),
  ]);

  return { transactions, total };
}

// Get single transaction
export async function getTransaction(transactionId: string) {
  const transaction = await prisma.transaction.findUnique({
    where: { transactionId },
  });

  if (!transaction) {
    throw new NotFoundError('Transaction', transactionId);
  }

  return transaction;
}
