// Transaction routes (replaces TRNVAL00 validation + INQHIST from src/programs/online/INQHIST.cbl)
import { Router, Response, NextFunction } from 'express';
import { prisma } from '../index';
import { authenticate, authorize, AuthRequest } from '../middleware/auth';

export const transactionRouter = Router();
transactionRouter.use(authenticate);

// GET /api/transactions — list all transactions
transactionRouter.get('/', async (req: AuthRequest, res: Response, next: NextFunction) => {
  try {
    const { portfolioId, type, status, startDate, endDate, page = '1', pageSize = '10' } = req.query;
    const skip = (Number(page) - 1) * Number(pageSize);
    const take = Number(pageSize);

    const where: Record<string, unknown> = {};
    if (portfolioId) where.portfolioId = String(portfolioId);
    if (type) where.type = String(type);
    if (status) where.status = String(status);
    if (startDate || endDate) {
      where.transactionDate = {
        ...(startDate && { gte: new Date(String(startDate)) }),
        ...(endDate && { lte: new Date(String(endDate)) }),
      };
    }

    const [data, total] = await Promise.all([
      prisma.transaction.findMany({
        where,
        skip,
        take,
        orderBy: { transactionDate: 'desc' },
        include: { portfolio: { select: { portfolioId: true, clientName: true } } },
      }),
      prisma.transaction.count({ where }),
    ]);

    res.json({
      success: true,
      data: { data, total, page: Number(page), pageSize: take, totalPages: Math.ceil(total / take) },
    });
  } catch (error) {
    next(error);
  }
});

// GET /api/portfolios/:id/transactions — transaction history (replaces INQHIST)
transactionRouter.get('/portfolio/:id', async (req: AuthRequest, res: Response, next: NextFunction) => {
  try {
    const portfolio = await prisma.portfolio.findFirst({
      where: { OR: [{ id: req.params.id }, { portfolioId: req.params.id }] },
    });

    if (!portfolio) {
      res.status(404).json({
        success: false,
        error: { code: 'VL02', message: 'Portfolio not found', category: 'VL', severity: 4 },
      });
      return;
    }

    const { page = '1', pageSize = '10', startDate, endDate } = req.query;
    const skip = (Number(page) - 1) * Number(pageSize);
    const take = Number(pageSize);

    const where: Record<string, unknown> = { portfolioId: portfolio.id };
    if (startDate || endDate) {
      where.transactionDate = {
        ...(startDate && { gte: new Date(String(startDate)) }),
        ...(endDate && { lte: new Date(String(endDate)) }),
      };
    }

    const [data, total] = await Promise.all([
      prisma.transaction.findMany({ where, skip, take, orderBy: { transactionDate: 'desc' } }),
      prisma.transaction.count({ where }),
    ]);

    res.json({
      success: true,
      data: { data, total, page: Number(page), pageSize: take, totalPages: Math.ceil(total / take) },
    });
  } catch (error) {
    next(error);
  }
});

// GET /api/transactions/:id — get transaction details
transactionRouter.get('/:id', async (req: AuthRequest, res: Response, next: NextFunction) => {
  try {
    const transaction = await prisma.transaction.findFirst({
      where: { OR: [{ id: req.params.id }, { transactionId: req.params.id }] },
      include: { portfolio: { select: { portfolioId: true, clientName: true } } },
    });

    if (!transaction) {
      res.status(404).json({
        success: false,
        error: { code: 'VL02', message: 'Transaction not found', category: 'VL', severity: 4 },
      });
      return;
    }

    res.json({ success: true, data: transaction });
  } catch (error) {
    next(error);
  }
});

// POST /api/transactions — submit new transaction
transactionRouter.post('/', authorize('UPDATE', 'ADMIN'), async (req: AuthRequest, res: Response, next: NextFunction) => {
  try {
    const { portfolioId, investmentId, type, quantity, price, currency } = req.body;

    // Validation (from TRNVAL00)
    if (!portfolioId || !investmentId || !type || quantity === undefined || price === undefined) {
      res.status(400).json({
        success: false,
        error: { code: 'VL01', message: 'portfolioId, investmentId, type, quantity, and price are required', category: 'VL', severity: 8 },
      });
      return;
    }

    // Transaction type validation (from TRNREC.cpy: BU/SL/TR/FE)
    const validTypes = ['BUY', 'SELL', 'TRANSFER', 'FEE'];
    if (!validTypes.includes(type)) {
      res.status(400).json({
        success: false,
        error: { code: 'VL01', message: `Invalid transaction type '${type}'. Must be one of: BUY, SELL, TRANSFER, FEE`, category: 'VL', severity: 8 },
      });
      return;
    }

    const portfolio = await prisma.portfolio.findFirst({
      where: { OR: [{ id: portfolioId }, { portfolioId }] },
    });

    if (!portfolio) {
      res.status(404).json({
        success: false,
        error: { code: 'VL02', message: 'Portfolio not found', category: 'VL', severity: 4 },
      });
      return;
    }

    if (portfolio.status !== 'ACTIVE') {
      res.status(400).json({
        success: false,
        error: { code: 'VL01', message: 'Cannot create transactions for non-active portfolios', category: 'VL', severity: 8 },
      });
      return;
    }

    const now = new Date();
    const transactionId = now.toISOString().replace(/[-T:.Z]/g, '').substring(0, 14) + String(Math.floor(Math.random() * 999999)).padStart(6, '0');
    const amount = Number(quantity) * Number(price);

    const transaction = await prisma.transaction.create({
      data: {
        transactionId,
        portfolioId: portfolio.id,
        transactionDate: now,
        transactionTime: now.toTimeString().substring(0, 8),
        investmentId,
        type,
        quantity,
        price,
        amount,
        currency: currency || 'USD',
        status: 'PENDING',
        processUser: req.userId?.substring(0, 8) || 'SYSTEM',
      },
    });

    // Audit log
    await prisma.auditLog.create({
      data: {
        portfolioId: portfolio.id,
        recordType: 'TRANSACTION',
        action: 'ADD',
        afterImage: transaction as unknown as Record<string, unknown>,
        userId: req.userId?.substring(0, 8) || 'SYSTEM',
        programId: 'TRNVAL00',
        message: `Transaction ${transactionId} created: ${type} ${quantity} ${investmentId}`,
      },
    });

    res.status(201).json({ success: true, data: transaction });
  } catch (error) {
    next(error);
  }
});
