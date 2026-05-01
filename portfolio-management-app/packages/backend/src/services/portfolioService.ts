import { PrismaClient, Prisma } from '@prisma/client';
import Decimal from 'decimal.js';
import {
  PortfolioStatus,
  AuditRecordType,
  AuditActionCode,
  type CreatePortfolioRequest,
  type UpdatePortfolioRequest,
} from '../types/index.js';
import {
  ValidationError,
  NotFoundError,
  DuplicateError,
} from '../utils/errors.js';
import { generateAuditSeqNo, formatTimeCOBOL } from '../utils/helpers.js';

const prisma = new PrismaClient();

// PORTADD.cbl — 2100-VALIDATE-AND-ADD
export async function createPortfolio(
  data: CreatePortfolioRequest,
  userId: string
) {
  // Validation: PORT-ID and PORT-CLIENT-NAME must not be empty, status must be 'A'
  if (!data.portfolioId || !data.portfolioName) {
    throw new ValidationError('Portfolio ID and name are required');
  }

  const existing = await prisma.portfolio.findUnique({
    where: { portfolioId: data.portfolioId },
  });

  if (existing) {
    throw new DuplicateError('Portfolio', data.portfolioId);
  }

  const now = new Date();
  const portfolio = await prisma.portfolio.create({
    data: {
      portfolioId: data.portfolioId,
      accountType: data.accountType,
      branchId: data.branchId,
      clientId: data.clientId,
      portfolioName: data.portfolioName,
      currencyCode: data.currencyCode,
      riskLevel: data.riskLevel,
      status: PortfolioStatus.Active,
      openDate: now,
      lastMaintDate: now,
      lastMaintUser: userId,
      totalValue: 0,
      cashBalance: 0,
    },
  });

  // HISTREC audit trail — record ADD action
  await prisma.auditLog.create({
    data: {
      portfolioId: data.portfolioId,
      date: now,
      time: formatTimeCOBOL(now),
      seqNo: generateAuditSeqNo(),
      recordType: AuditRecordType.Portfolio,
      actionCode: AuditActionCode.Add,
      afterImage: JSON.stringify(portfolio),
      processDate: now,
      processUser: userId,
    },
  });

  return portfolio;
}

// PORTREAD.cbl — 2100-DISPLAY-RECORD
export async function getPortfolio(portfolioId: string) {
  const portfolio = await prisma.portfolio.findUnique({
    where: { portfolioId },
    include: {
      positions: {
        orderBy: { positionDate: 'desc' },
      },
    },
  });

  if (!portfolio) {
    throw new NotFoundError('Portfolio', portfolioId);
  }

  return portfolio;
}

// PORTUPDT.cbl — 2200-APPLY-UPDATE
export async function updatePortfolio(
  portfolioId: string,
  data: UpdatePortfolioRequest,
  userId: string
) {
  const existing = await prisma.portfolio.findUnique({
    where: { portfolioId },
  });

  if (!existing) {
    throw new NotFoundError('Portfolio', portfolioId);
  }

  const beforeImage = JSON.stringify(existing);

  const updateData: Prisma.PortfolioUpdateInput = {
    lastMaintDate: new Date(),
    lastMaintUser: userId,
  };

  // Matches EVALUATE TRUE in PORTUPDT — UPDT-STATUS, UPDT-NAME, UPDT-VALUE
  if (data.status !== undefined) updateData.status = data.status;
  if (data.portfolioName !== undefined) updateData.portfolioName = data.portfolioName;
  if (data.riskLevel !== undefined) updateData.riskLevel = data.riskLevel;
  if (data.cashBalance !== undefined) updateData.cashBalance = data.cashBalance;

  const portfolio = await prisma.portfolio.update({
    where: { portfolioId },
    data: updateData,
  });

  // Audit trail
  await prisma.auditLog.create({
    data: {
      portfolioId,
      date: new Date(),
      time: formatTimeCOBOL(new Date()),
      seqNo: generateAuditSeqNo(),
      recordType: AuditRecordType.Portfolio,
      actionCode: AuditActionCode.Change,
      beforeImage,
      afterImage: JSON.stringify(portfolio),
      processDate: new Date(),
      processUser: userId,
    },
  });

  return portfolio;
}

// PORTDEL.cbl — 2100-PROCESS-DELETE
// Soft-delete: set status to Closed, record closeDate
export async function deletePortfolio(
  portfolioId: string,
  reasonCode: string,
  userId: string
) {
  const existing = await prisma.portfolio.findUnique({
    where: { portfolioId },
    include: { positions: true },
  });

  if (!existing) {
    throw new NotFoundError('Portfolio', portfolioId);
  }

  // Check for open positions
  const activePositions = existing.positions.filter(
    (p) => new Decimal(p.quantity.toString()).greaterThan(0)
  );
  if (activePositions.length > 0) {
    throw new ValidationError(
      'Cannot close portfolio with active positions',
      `${activePositions.length} positions still open`
    );
  }

  const beforeImage = JSON.stringify(existing);
  const now = new Date();

  const portfolio = await prisma.portfolio.update({
    where: { portfolioId },
    data: {
      status: PortfolioStatus.Closed,
      closeDate: now,
      lastMaintDate: now,
      lastMaintUser: userId,
    },
  });

  // Audit trail with reason code
  await prisma.auditLog.create({
    data: {
      portfolioId,
      date: now,
      time: formatTimeCOBOL(now),
      seqNo: generateAuditSeqNo(),
      recordType: AuditRecordType.Portfolio,
      actionCode: AuditActionCode.Delete,
      beforeImage,
      afterImage: JSON.stringify(portfolio),
      reasonCode,
      processDate: now,
      processUser: userId,
    },
  });

  return portfolio;
}

// List portfolios with filtering
export async function listPortfolios(params: {
  page: number;
  pageSize: number;
  status?: PortfolioStatus;
  clientId?: string;
  search?: string;
}) {
  const where: Prisma.PortfolioWhereInput = {};

  if (params.status) where.status = params.status;
  if (params.clientId) where.clientId = params.clientId;
  if (params.search) {
    where.OR = [
      { portfolioName: { contains: params.search, mode: 'insensitive' } },
      { portfolioId: { contains: params.search, mode: 'insensitive' } },
    ];
  }

  const [portfolios, total] = await Promise.all([
    prisma.portfolio.findMany({
      where,
      skip: (params.page - 1) * params.pageSize,
      take: params.pageSize,
      orderBy: { lastMaintDate: 'desc' },
    }),
    prisma.portfolio.count({ where }),
  ]);

  return { portfolios, total };
}

// PORTVALD.cbl — validate portfolio data
export async function validatePortfolio(portfolioId: string) {
  const portfolio = await prisma.portfolio.findUnique({
    where: { portfolioId },
    include: { positions: true, transactions: true },
  });

  if (!portfolio) {
    throw new NotFoundError('Portfolio', portfolioId);
  }

  const issues: string[] = [];

  // Validate portfolio ID format
  if (!/^PORT\d{4}$/.test(portfolio.portfolioId)) {
    issues.push('Invalid portfolio ID format');
  }

  // Validate status
  if (!['A', 'C', 'S'].includes(portfolio.status)) {
    issues.push(`Invalid status: ${portfolio.status}`);
  }

  // Validate total value matches sum of positions
  const positionTotal = portfolio.positions.reduce(
    (sum, p) => sum.plus(p.marketValue.toString()),
    new Decimal(0)
  );

  if (!positionTotal.equals(portfolio.totalValue.toString())) {
    issues.push(
      `Total value mismatch: recorded=${portfolio.totalValue}, calculated=${positionTotal}`
    );
  }

  return {
    portfolioId,
    valid: issues.length === 0,
    issues,
    positionCount: portfolio.positions.length,
    transactionCount: portfolio.transactions.length,
  };
}

// ACTIVE_PORTFOLIOS view equivalent
export async function getActivePortfolios() {
  return prisma.portfolio.findMany({
    where: {
      status: PortfolioStatus.Active,
      OR: [{ closeDate: null }, { closeDate: { gt: new Date() } }],
    },
  });
}
