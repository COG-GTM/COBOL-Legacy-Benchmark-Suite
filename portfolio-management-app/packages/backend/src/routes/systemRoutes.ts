import { Router, Request, Response, NextFunction } from 'express';
import * as systemService from '../services/systemService.js';
import { authenticate, authorize } from '../middleware/auth.js';

const router = Router();

// GET /api/system/health — UTLMON00.cbl logic
router.get('/health', async (_req: Request, res: Response, next: NextFunction) => {
  try {
    const health = await systemService.getSystemHealth();
    const statusCode = health.status === 'healthy' ? 200 : 503;
    res.status(statusCode).json({ success: true, data: health });
  } catch (err) {
    next(err);
  }
});

// POST /api/system/validate — UTLVAL00.cbl logic
router.post('/validate', authenticate, authorize('admin'), async (_req: Request, res: Response, next: NextFunction) => {
  try {
    const result = await systemService.validateSystemData();
    res.json({ success: true, data: result });
  } catch (err) {
    next(err);
  }
});

// POST /api/system/maintenance — UTLMNT00.cbl logic
router.post('/maintenance', authenticate, authorize('admin'), async (req: Request, res: Response, next: NextFunction) => {
  try {
    const { operation } = req.body;
    const result = await systemService.runMaintenance(operation || 'ANALYZE');
    res.json({ success: true, data: result });
  } catch (err) {
    next(err);
  }
});

export default router;
