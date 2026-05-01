import { Router, Request, Response, NextFunction } from 'express';
import * as positionService from '../services/positionService.js';
import { authenticate } from '../middleware/auth.js';
import { z } from 'zod';

const router = Router();

// GET /api/portfolios/:id/positions — INQPORT.cbl logic
router.get('/:id/positions', authenticate, async (req: Request, res: Response, next: NextFunction) => {
  try {
    const positions = await positionService.getPositions(req.params.id as string);
    res.json({ success: true, data: positions });
  } catch (err) {
    next(err);
  }
});

// PUT /api/portfolios/:id/positions — Batch position update (POSUPD00 logic)
const updatePositionsSchema = z.object({
  positions: z.array(
    z.object({
      investmentId: z.string().min(1).max(10),
      marketValue: z.number(),
    })
  ),
});

router.put('/:id/positions', authenticate, async (req: Request, res: Response, next: NextFunction) => {
  try {
    const { positions } = updatePositionsSchema.parse(req.body);
    const results = await positionService.batchUpdatePositions(
      req.params.id as string,
      positions,
      req.user!.userId
    );
    res.json({ success: true, data: results });
  } catch (err) {
    next(err);
  }
});

export default router;
