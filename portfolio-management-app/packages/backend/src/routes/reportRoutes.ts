import { Router, Request, Response, NextFunction } from 'express';
import * as reportService from '../services/reportService.js';
import { reportQuerySchema } from '../utils/validation.js';
import { authenticate } from '../middleware/auth.js';

const router = Router();

// GET /api/reports/positions — RPTPOS00.cbl logic
router.get('/positions', authenticate, async (req: Request, res: Response, next: NextFunction) => {
  try {
    const params = reportQuerySchema.parse(req.query);
    const report = await reportService.getPositionReport(params);
    res.json({ success: true, data: report });
  } catch (err) {
    next(err);
  }
});

// GET /api/reports/audit — RPTAUD00.cbl logic
router.get('/audit', authenticate, async (req: Request, res: Response, next: NextFunction) => {
  try {
    const params = reportQuerySchema.parse(req.query);
    const report = await reportService.getAuditReport(params);
    res.json({ success: true, data: report });
  } catch (err) {
    next(err);
  }
});

// GET /api/reports/statistics — RPTSTA00.cbl logic
router.get('/statistics', authenticate, async (_req: Request, res: Response, next: NextFunction) => {
  try {
    const report = await reportService.getStatisticsReport();
    res.json({ success: true, data: report });
  } catch (err) {
    next(err);
  }
});

export default router;
