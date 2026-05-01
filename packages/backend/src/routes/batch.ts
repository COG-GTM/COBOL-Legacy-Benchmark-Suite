import { Router } from 'express';

const router = Router();

// POST /api/batch/start — Start batch run
router.post('/start', async (_req, res) => {
  res.status(501).json({ message: 'Not implemented' });
});

// GET /api/batch/:runId/status — Batch status
router.get('/:runId/status', async (_req, res) => {
  res.status(501).json({ message: 'Not implemented' });
});

// POST /api/batch/:runId/restart — Restart from checkpoint
router.post('/:runId/restart', async (_req, res) => {
  res.status(501).json({ message: 'Not implemented' });
});

export default router;
