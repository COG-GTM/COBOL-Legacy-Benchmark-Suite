// Portfolio routes (replaces PORTMSTR.cbl, PORTADD, PORTDEL, PORTREAD, PORTUPDT, PORTVALD)
import { Router, Response, NextFunction } from 'express';
import { prisma } from '../index';
import { authenticate, authorize, AuthRequest } from '../middleware/auth';

export const portfolioRouter = Router();

// All portfolio routes require authentication
portfolioRouter.use(authenticate);

// GET /api/portfolios — list portfolios (with filters)
portfolioRouter.get('/', async (req: AuthRequest, res: Response, next: NextFunction) => {
  try {
    const { status, clientType, search, page = '1', pageSize = '20' } = req.query;
    const skip = (Number(page) - 1) * Number(pageSize);
    const take = Number(pageSize);

    const where: Record<string, unknown> = {};
    if (status) where.status = status;
    if (clientType) where.clientType = clientType;
    if (search) {
      where.OR = [
        { portfolioId: { contains: String(search), mode: 'insensitive' } },
        { clientName: { contains: String(search), mode: 'insensitive' } },
        { accountNo: { contains: String(search), mode: 'insensitive' } },
      ];
    }

    const [data, total] = await Promise.all([
      prisma.portfolio.findMany({ where, skip, take, orderBy: { updatedAt: 'desc' } }),
      prisma.portfolio.count({ where }),
    ]);

    res.json({
      success: true,
      data: {
        data,
        total,
        page: Number(page),
        pageSize: take,
        totalPages: Math.ceil(total / take),
      },
    });
  } catch (error) {
    next(error);
  }
});

// GET /api/portfolios/:id — get portfolio details
portfolioRouter.get('/:id', async (req: AuthRequest, res: Response, next: NextFunction) => {
  try {
    const portfolio = await prisma.portfolio.findFirst({
      where: { OR: [{ id: req.params.id }, { portfolioId: req.params.id }] },
      include: {
        positions: { where: { status: 'ACTIVE' }, orderBy: { updatedAt: 'desc' } },
        transactions: { take: 10, orderBy: { transactionDate: 'desc' } },
      },
    });

    if (!portfolio) {
      res.status(404).json({
        success: false,
        error: { code: 'VL02', message: 'Portfolio not found', category: 'VL', severity: 4 },
      });
      return;
    }

    res.json({ success: true, data: portfolio });
  } catch (error) {
    next(error);
  }
});

// POST /api/portfolios — create portfolio (with validation from PORTVALD logic)
portfolioRouter.post('/', authorize('UPDATE', 'ADMIN'), async (req: AuthRequest, res: Response, next: NextFunction) => {
  try {
    const { portfolioId, accountNo, clientName, clientType, status, totalValue, cashBalance, currencyCode, riskLevel, branchId } = req.body;

    // Validation from PORTMSTR.cbl lines 142-147: ID must start with 'PORT' + 5 digits
    if (!portfolioId || !/^PORT\d{5}$/.test(portfolioId)) {
      res.status(400).json({
        success: false,
        error: { code: 'VL01', message: "Invalid Portfolio ID format. Must be 'PORT' followed by 5 numeric digits", category: 'VL', severity: 8 },
      });
      return;
    }

    // Validation from PORTMSTR.cbl lines 149-153: name required
    if (!clientName || clientName.trim().length === 0) {
      res.status(400).json({
        success: false,
        error: { code: 'VL01', message: 'Portfolio Name is required', category: 'VL', severity: 8 },
      });
      return;
    }

    // Validation from PORTMSTR.cbl lines 155-160: valid status
    if (status && !['ACTIVE', 'CLOSED', 'SUSPENDED'].includes(status)) {
      res.status(400).json({
        success: false,
        error: { code: 'VL01', message: 'Invalid Portfolio Status', category: 'VL', severity: 8 },
      });
      return;
    }

    // Check for duplicate (VSAM status 22)
    const existing = await prisma.portfolio.findUnique({ where: { portfolioId } });
    if (existing) {
      res.status(409).json({
        success: false,
        error: { code: 'VL03', message: 'Portfolio ID already exists', category: 'VL', severity: 4 },
      });
      return;
    }

    const portfolio = await prisma.portfolio.create({
      data: {
        portfolioId,
        accountNo: accountNo || portfolioId.replace('PORT', '') + '00000',
        clientName,
        clientType: clientType || 'INDIVIDUAL',
        status: status || 'ACTIVE',
        totalValue: totalValue || 0,
        cashBalance: cashBalance || 0,
        currencyCode: currencyCode || 'USD',
        riskLevel: riskLevel || 'M',
        branchId: branchId || '01',
        lastUser: req.userId?.substring(0, 8) || 'SYSTEM',
      },
    });

    // Audit log
    await prisma.auditLog.create({
      data: {
        portfolioId: portfolio.id,
        recordType: 'PORTFOLIO',
        action: 'ADD',
        afterImage: portfolio as unknown as Record<string, unknown>,
        userId: req.userId?.substring(0, 8) || 'SYSTEM',
        programId: 'PORTADD',
        message: `Portfolio ${portfolioId} created`,
      },
    });

    res.status(201).json({ success: true, data: portfolio });
  } catch (error) {
    next(error);
  }
});

// PUT /api/portfolios/:id — update portfolio
portfolioRouter.put('/:id', authorize('UPDATE', 'ADMIN'), async (req: AuthRequest, res: Response, next: NextFunction) => {
  try {
    const portfolio = await prisma.portfolio.findFirst({
      where: { OR: [{ id: req.params.id }, { portfolioId: req.params.id }] },
    });

    if (!portfolio) {
      res.status(404).json({
        success: false,
        error: { code: 'VL02', message: 'Portfolio not found for update', category: 'VL', severity: 4 },
      });
      return;
    }

    const { clientName, clientType, status, totalValue, cashBalance, currencyCode, riskLevel } = req.body;

    // Status transition validation
    if (status && status !== portfolio.status) {
      const transitions: Record<string, string[]> = {
        ACTIVE: ['CLOSED', 'SUSPENDED'],
        SUSPENDED: ['ACTIVE', 'CLOSED'],
        CLOSED: [],
      };
      const allowed = transitions[portfolio.status];
      if (!allowed || !allowed.includes(status)) {
        res.status(400).json({
          success: false,
          error: { code: 'VL01', message: `Invalid status transition from '${portfolio.status}' to '${status}'`, category: 'VL', severity: 8 },
        });
        return;
      }
    }

    const beforeImage = { ...portfolio };
    const updated = await prisma.portfolio.update({
      where: { id: portfolio.id },
      data: {
        ...(clientName !== undefined && { clientName }),
        ...(clientType !== undefined && { clientType }),
        ...(status !== undefined && { status }),
        ...(totalValue !== undefined && { totalValue }),
        ...(cashBalance !== undefined && { cashBalance }),
        ...(currencyCode !== undefined && { currencyCode }),
        ...(riskLevel !== undefined && { riskLevel }),
        lastUser: req.userId?.substring(0, 8) || 'SYSTEM',
        closeDate: status === 'CLOSED' ? new Date() : undefined,
      },
    });

    // Audit log with before/after images
    await prisma.auditLog.create({
      data: {
        portfolioId: portfolio.id,
        recordType: 'PORTFOLIO',
        action: 'CHANGE',
        beforeImage: beforeImage as unknown as Record<string, unknown>,
        afterImage: updated as unknown as Record<string, unknown>,
        userId: req.userId?.substring(0, 8) || 'SYSTEM',
        programId: 'PORTUPDT',
        message: `Portfolio ${portfolio.portfolioId} updated`,
      },
    });

    res.json({ success: true, data: updated });
  } catch (error) {
    next(error);
  }
});

// DELETE /api/portfolios/:id — delete portfolio
portfolioRouter.delete('/:id', authorize('ADMIN'), async (req: AuthRequest, res: Response, next: NextFunction) => {
  try {
    const portfolio = await prisma.portfolio.findFirst({
      where: { OR: [{ id: req.params.id }, { portfolioId: req.params.id }] },
    });

    if (!portfolio) {
      res.status(404).json({
        success: false,
        error: { code: 'VL02', message: 'Portfolio not found for deletion', category: 'VL', severity: 4 },
      });
      return;
    }

    // Audit log before deletion
    await prisma.auditLog.create({
      data: {
        portfolioId: portfolio.id,
        recordType: 'PORTFOLIO',
        action: 'DELETE',
        beforeImage: portfolio as unknown as Record<string, unknown>,
        userId: req.userId?.substring(0, 8) || 'SYSTEM',
        programId: 'PORTDEL',
        message: `Portfolio ${portfolio.portfolioId} deleted`,
      },
    });

    await prisma.portfolio.delete({ where: { id: portfolio.id } });

    res.json({ success: true, data: { message: `Portfolio ${portfolio.portfolioId} deleted` } });
  } catch (error) {
    next(error);
  }
});
