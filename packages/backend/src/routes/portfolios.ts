import { Router } from 'express';

const router = Router();

// POST /api/portfolios — Create portfolio
router.post('/', async (_req, res) => {
  res.status(501).json({ message: 'Not implemented' });
});

// GET /api/portfolios — List portfolios
router.get('/', async (_req, res) => {
  res.status(501).json({ message: 'Not implemented' });
});

// GET /api/portfolios/:id — Get portfolio
router.get('/:id', async (_req, res) => {
  res.status(501).json({ message: 'Not implemented' });
});

// PUT /api/portfolios/:id — Update portfolio
router.put('/:id', async (_req, res) => {
  res.status(501).json({ message: 'Not implemented' });
});

// DELETE /api/portfolios/:id — Delete (soft) portfolio
router.delete('/:id', async (_req, res) => {
  res.status(501).json({ message: 'Not implemented' });
});

// GET /api/portfolios/:id/transactions — List transactions
router.get('/:id/transactions', async (_req, res) => {
  res.status(501).json({ message: 'Not implemented' });
});

export default router;
