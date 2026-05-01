import { Router } from 'express';

const router = Router();

// GET /api/reports/positions — Position report
router.get('/positions', async (_req, res) => {
  res.status(501).json({ message: 'Not implemented' });
});

// GET /api/reports/audit — Audit report
router.get('/audit', async (_req, res) => {
  res.status(501).json({ message: 'Not implemented' });
});

// GET /api/reports/statistics — Statistics report
router.get('/statistics', async (_req, res) => {
  res.status(501).json({ message: 'Not implemented' });
});

export default router;
