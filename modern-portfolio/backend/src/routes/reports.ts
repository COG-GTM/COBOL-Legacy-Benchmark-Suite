// Report routes (replaces RPTPOS00, RPTAUD00, RPTSTA00)
import { Router, Response, NextFunction } from 'express';
import { prisma } from '../index';
import { authenticate, AuthRequest } from '../middleware/auth';

export const reportRouter = Router();
reportRouter.use(authenticate);

// GET /api/reports/positions — position report
reportRouter.get('/positions', async (req: AuthRequest, res: Response, next: NextFunction) => {
  try {
    const { portfolioId } = req.query;

    const where: Record<string, unknown> = { status: 'ACTIVE' };
    if (portfolioId) {
      const portfolio = await prisma.portfolio.findFirst({
        where: { OR: [{ id: String(portfolioId) }, { portfolioId: String(portfolioId) }] },
      });
      if (portfolio) where.portfolioId = portfolio.id;
    }

    const portfolios = await prisma.portfolio.findMany({
      where: portfolioId ? { id: where.portfolioId as string } : { status: 'ACTIVE' },
      include: {
        positions: { where: { status: 'ACTIVE' }, orderBy: { investmentId: 'asc' } },
      },
    });

    const report = portfolios.map((p: typeof portfolios[number]) => ({
      portfolioId: p.portfolioId,
      portfolioName: p.clientName,
      positions: p.positions,
      totalCostBasis: p.positions.reduce((sum: number, pos: typeof p.positions[number]) => sum + Number(pos.costBasis), 0),
      totalMarketValue: p.positions.reduce((sum: number, pos: typeof p.positions[number]) => sum + Number(pos.marketValue), 0),
      totalGainLoss: p.positions.reduce((sum: number, pos: typeof p.positions[number]) => sum + (Number(pos.marketValue) - Number(pos.costBasis)), 0),
    }));

    res.json({ success: true, data: report });
  } catch (error) {
    next(error);
  }
});

// GET /api/reports/audit — audit report
reportRouter.get('/audit', async (req: AuthRequest, res: Response, next: NextFunction) => {
  try {
    const { startDate, endDate, recordType, action, page = '1', pageSize = '20' } = req.query;
    const skip = (Number(page) - 1) * Number(pageSize);
    const take = Number(pageSize);

    const where: Record<string, unknown> = {};
    if (recordType) where.recordType = String(recordType);
    if (action) where.action = String(action);
    if (startDate || endDate) {
      where.createdAt = {
        ...(startDate && { gte: new Date(String(startDate)) }),
        ...(endDate && { lte: new Date(String(endDate)) }),
      };
    }

    const [entries, total] = await Promise.all([
      prisma.auditLog.findMany({
        where,
        skip,
        take,
        orderBy: { createdAt: 'desc' },
        include: { portfolio: { select: { portfolioId: true, clientName: true } } },
      }),
      prisma.auditLog.count({ where }),
    ]);

    res.json({
      success: true,
      data: {
        entries,
        total,
        page: Number(page),
        pageSize: take,
        totalPages: Math.ceil(total / take),
        dateRange: {
          from: startDate || 'all',
          to: endDate || 'all',
        },
      },
    });
  } catch (error) {
    next(error);
  }
});

// GET /api/reports/statistics — system statistics
reportRouter.get('/statistics', async (_req: AuthRequest, res: Response, next: NextFunction) => {
  try {
    const [
      totalPortfolios,
      activePortfolios,
      totalPositions,
      totalTransactions,
      pendingTransactions,
      portfolioValues,
    ] = await Promise.all([
      prisma.portfolio.count(),
      prisma.portfolio.count({ where: { status: 'ACTIVE' } }),
      prisma.position.count(),
      prisma.transaction.count(),
      prisma.transaction.count({ where: { status: 'PENDING' } }),
      prisma.portfolio.aggregate({ _sum: { totalValue: true } }),
    ]);

    // Recent activity (last 7 days)
    const sevenDaysAgo = new Date();
    sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7);

    const recentTransactions = await prisma.transaction.findMany({
      where: { transactionDate: { gte: sevenDaysAgo } },
      select: { transactionDate: true },
      orderBy: { transactionDate: 'asc' },
    });

    const activityMap = new Map<string, number>();
    for (let i = 6; i >= 0; i--) {
      const date = new Date();
      date.setDate(date.getDate() - i);
      activityMap.set(date.toISOString().substring(0, 10), 0);
    }
    recentTransactions.forEach((t: { transactionDate: Date }) => {
      const dateKey = t.transactionDate.toISOString().substring(0, 10);
      activityMap.set(dateKey, (activityMap.get(dateKey) || 0) + 1);
    });

    const recentActivity = Array.from(activityMap.entries()).map(([date, count]) => ({ date, count }));

    res.json({
      success: true,
      data: {
        totalPortfolios,
        activePortfolios,
        totalPositions,
        totalTransactions,
        pendingTransactions,
        totalValue: Number(portfolioValues._sum.totalValue || 0),
        recentActivity,
      },
    });
  } catch (error) {
    next(error);
  }
});
