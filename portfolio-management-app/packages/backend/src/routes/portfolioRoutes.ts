import { Router, Request, Response, NextFunction } from 'express';
import * as portfolioService from '../services/portfolioService.js';
import {
  createPortfolioSchema,
  updatePortfolioSchema,
  portfolioListSchema,
} from '../utils/validation.js';
import { paginationInfo } from '../utils/helpers.js';
import { authenticate } from '../middleware/auth.js';

const router = Router();

// POST /api/portfolios — PORTADD.cbl logic
router.post('/', authenticate, async (req: Request, res: Response, next: NextFunction) => {
  try {
    const data = createPortfolioSchema.parse(req.body);
    const portfolio = await portfolioService.createPortfolio(data, req.user!.userId);
    res.status(201).json({ success: true, data: portfolio });
  } catch (err) {
    next(err);
  }
});

// GET /api/portfolios — List portfolios with filtering
router.get('/', authenticate, async (req: Request, res: Response, next: NextFunction) => {
  try {
    const params = portfolioListSchema.parse(req.query);
    const { portfolios, total } = await portfolioService.listPortfolios(params);
    res.json({
      success: true,
      data: portfolios,
      pagination: paginationInfo(total, params.page, params.pageSize),
    });
  } catch (err) {
    next(err);
  }
});

// GET /api/portfolios/:id — PORTREAD.cbl logic
router.get('/:id', authenticate, async (req: Request, res: Response, next: NextFunction) => {
  try {
    const portfolio = await portfolioService.getPortfolio(req.params.id as string);
    res.json({ success: true, data: portfolio });
  } catch (err) {
    next(err);
  }
});

// PUT /api/portfolios/:id — PORTUPDT.cbl logic
router.put('/:id', authenticate, async (req: Request, res: Response, next: NextFunction) => {
  try {
    const data = updatePortfolioSchema.parse(req.body);
    const portfolio = await portfolioService.updatePortfolio(
      req.params.id as string,
      data,
      req.user!.userId
    );
    res.json({ success: true, data: portfolio });
  } catch (err) {
    next(err);
  }
});

// DELETE /api/portfolios/:id — PORTDEL.cbl logic
router.delete('/:id', authenticate, async (req: Request, res: Response, next: NextFunction) => {
  try {
    const reasonCode = (req.query.reason as string) || '03';
    const portfolio = await portfolioService.deletePortfolio(
      req.params.id as string,
      reasonCode,
      req.user!.userId
    );
    res.json({ success: true, data: portfolio });
  } catch (err) {
    next(err);
  }
});

// POST /api/portfolios/:id/validate — PORTVALD.cbl logic
router.post('/:id/validate', authenticate, async (req: Request, res: Response, next: NextFunction) => {
  try {
    const result = await portfolioService.validatePortfolio(req.params.id as string);
    res.json({ success: true, data: result });
  } catch (err) {
    next(err);
  }
});

export default router;
