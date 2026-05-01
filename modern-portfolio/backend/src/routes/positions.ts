// Position routes (replaces POSUPD00 batch logic + INQPORT online inquiry)
import { Router, Response, NextFunction } from 'express';
import { prisma } from '../index';
import { authenticate, authorize, AuthRequest } from '../middleware/auth';

export const positionRouter = Router();
positionRouter.use(authenticate);

// GET /api/positions/current — current positions view (replaces CURRENT_POSITIONS DB2 view)
positionRouter.get('/current', async (req: AuthRequest, res: Response, next: NextFunction) => {
  try {
    const { portfolioId, page = '1', pageSize = '20' } = req.query;
    const skip = (Number(page) - 1) * Number(pageSize);
    const take = Number(pageSize);

    const where: Record<string, unknown> = { status: 'ACTIVE' };
    if (portfolioId) where.portfolioId = String(portfolioId);

    const [data, total] = await Promise.all([
      prisma.position.findMany({
        where,
        skip,
        take,
        orderBy: { updatedAt: 'desc' },
        include: { portfolio: { select: { portfolioId: true, clientName: true, accountNo: true } } },
      }),
      prisma.position.count({ where }),
    ]);

    res.json({
      success: true,
      data: { data, total, page: Number(page), pageSize: take, totalPages: Math.ceil(total / take) },
    });
  } catch (error) {
    next(error);
  }
});

// GET /api/portfolios/:id/positions — get positions for a portfolio
positionRouter.get('/portfolio/:id', async (req: AuthRequest, res: Response, next: NextFunction) => {
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

    const positions = await prisma.position.findMany({
      where: { portfolioId: portfolio.id },
      orderBy: { updatedAt: 'desc' },
    });

    res.json({ success: true, data: positions });
  } catch (error) {
    next(error);
  }
});

// POST /api/portfolios/:id/positions — add/update position
positionRouter.post('/portfolio/:id', authorize('UPDATE', 'ADMIN'), async (req: AuthRequest, res: Response, next: NextFunction) => {
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

    const { investmentId, positionDate, quantity, costBasis, marketValue, currency, status } = req.body;

    if (!investmentId || quantity === undefined || costBasis === undefined || marketValue === undefined) {
      res.status(400).json({
        success: false,
        error: { code: 'VL01', message: 'investmentId, quantity, costBasis, and marketValue are required', category: 'VL', severity: 8 },
      });
      return;
    }

    const position = await prisma.position.upsert({
      where: {
        portfolioId_investmentId_positionDate: {
          portfolioId: portfolio.id,
          investmentId,
          positionDate: positionDate ? new Date(positionDate) : new Date(),
        },
      },
      update: {
        quantity,
        costBasis,
        marketValue,
        currency: currency || 'USD',
        status: status || 'ACTIVE',
        lastUser: req.userId?.substring(0, 8) || 'SYSTEM',
      },
      create: {
        portfolioId: portfolio.id,
        investmentId,
        positionDate: positionDate ? new Date(positionDate) : new Date(),
        quantity,
        costBasis,
        marketValue,
        currency: currency || 'USD',
        status: status || 'ACTIVE',
        lastUser: req.userId?.substring(0, 8) || 'SYSTEM',
      },
    });

    // Audit log
    await prisma.auditLog.create({
      data: {
        portfolioId: portfolio.id,
        recordType: 'POSITION',
        action: 'ADD',
        afterImage: position as unknown as Record<string, unknown>,
        userId: req.userId?.substring(0, 8) || 'SYSTEM',
        programId: 'POSUPD00',
        message: `Position ${investmentId} updated for portfolio ${portfolio.portfolioId}`,
      },
    });

    res.status(201).json({ success: true, data: position });
  } catch (error) {
    next(error);
  }
});
