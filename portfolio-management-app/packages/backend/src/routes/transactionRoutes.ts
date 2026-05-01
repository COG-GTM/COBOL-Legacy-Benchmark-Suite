import { Router, Request, Response, NextFunction } from 'express';
import * as transactionService from '../services/transactionService.js';
import { createTransactionSchema, paginationSchema } from '../utils/validation.js';
import { paginationInfo } from '../utils/helpers.js';
import { authenticate } from '../middleware/auth.js';

const router = Router();

// POST /api/transactions — TRNVAL00.cbl + PORTTRAN.cbl logic
router.post('/', authenticate, async (req: Request, res: Response, next: NextFunction) => {
  try {
    const data = createTransactionSchema.parse(req.body);
    const transaction = await transactionService.createTransaction(data, req.user!.userId);

    // Emit WebSocket event for real-time update
    const io = req.app.get('io');
    if (io) {
      io.to(`portfolio:${data.portfolioId}`).emit('transaction:created', transaction);
      io.emit('transaction:created', transaction);
    }

    res.status(201).json({ success: true, data: transaction });
  } catch (err) {
    next(err);
  }
});

// GET /api/portfolios/:id/transactions — INQHIST.cbl logic
router.get('/portfolio/:id', authenticate, async (req: Request, res: Response, next: NextFunction) => {
  try {
    const { page, pageSize } = paginationSchema.parse(req.query);
    const { transactions, total } = await transactionService.getTransactionHistory(
      req.params.id as string,
      page,
      pageSize
    );
    res.json({
      success: true,
      data: transactions,
      pagination: paginationInfo(total, page, pageSize),
    });
  } catch (err) {
    next(err);
  }
});

// GET /api/transactions/:id
router.get('/:id', authenticate, async (req: Request, res: Response, next: NextFunction) => {
  try {
    const transaction = await transactionService.getTransaction(req.params.id as string);
    res.json({ success: true, data: transaction });
  } catch (err) {
    next(err);
  }
});

export default router;
