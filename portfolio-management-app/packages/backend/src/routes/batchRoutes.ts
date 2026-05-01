import { Router, Request, Response, NextFunction } from 'express';
import * as batchService from '../services/batchService.js';
import { batchRunSchema } from '../utils/validation.js';
import { authenticate, authorize } from '../middleware/auth.js';
import { WSEvent } from '../types/index.js';

const router = Router();

// POST /api/batch/run — BCHCTL00.cbl logic: trigger batch processing cycle
router.post('/run', authenticate, authorize('admin', 'user'), async (req: Request, res: Response, next: NextFunction) => {
  try {
    const { jobName, processDate } = batchRunSchema.parse(req.body);
    const io = req.app.get('io');

    // Run batch with progress reporting via WebSocket
    const result = await batchService.runBatchCycle(
      jobName,
      processDate,
      (step, progress) => {
        if (io) {
          io.emit(WSEvent.BatchProgress, { step, progress, jobName });
        }
      }
    );

    // Emit completion event
    if (io) {
      io.emit(WSEvent.BatchCompleted, result);
    }

    res.json({ success: true, data: result });
  } catch (err) {
    const io = req.app.get('io');
    if (io) {
      io.emit(WSEvent.BatchFailed, {
        error: err instanceof Error ? err.message : 'Unknown error',
      });
    }
    next(err);
  }
});

// GET /api/batch/status — Get batch job status
router.get('/status', authenticate, async (_req: Request, res: Response, next: NextFunction) => {
  try {
    const jobs = await batchService.getBatchStatus();
    res.json({ success: true, data: jobs });
  } catch (err) {
    next(err);
  }
});

export default router;
