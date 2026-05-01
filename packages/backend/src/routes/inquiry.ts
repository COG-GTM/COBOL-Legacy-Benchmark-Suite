import { Router } from 'express';

const router = Router();

// GET /api/inquiry/portfolio/:accountNo — Portfolio positions inquiry
router.get('/portfolio/:accountNo', async (_req, res) => {
  res.status(501).json({ message: 'Not implemented' });
});

// GET /api/inquiry/history/:accountNo — Transaction history inquiry
router.get('/history/:accountNo', async (_req, res) => {
  res.status(501).json({ message: 'Not implemented' });
});

export default router;
