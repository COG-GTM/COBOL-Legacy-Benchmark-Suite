// Batch job routes (replaces BCHCTL00 batch control + JCL jobs)
import { Router, Response, NextFunction } from 'express';
import { prisma } from '../index';
import { authenticate, authorize, AuthRequest } from '../middleware/auth';

export const jobRouter = Router();
jobRouter.use(authenticate);

interface JobState {
  id: string;
  type: string;
  status: string;
  progress: number;
  result: unknown;
  error: string | null;
  startedAt: string;
  completedAt: string | null;
}

const jobStore = new Map<string, JobState>();

// POST /api/jobs/process-transactions — trigger transaction processing pipeline
// Replaces: TRNVAL00 -> POSUPD00 -> HISTLD00
jobRouter.post('/process-transactions', authorize('ADMIN'), async (req: AuthRequest, res: Response, next: NextFunction) => {
  try {
    const jobId = `job-${Date.now()}`;
    const job: JobState = {
      id: jobId,
      type: 'process-transactions',
      status: 'RUNNING',
      progress: 0,
      result: null,
      error: null,
      startedAt: new Date().toISOString(),
      completedAt: null,
    };
    jobStore.set(jobId, job);

    // Run async processing
    processTransactions(jobId, req.userId || 'SYSTEM').catch((err) => {
      const j = jobStore.get(jobId);
      if (j) {
        j.status = 'FAILED';
        j.error = err instanceof Error ? err.message : String(err);
        j.completedAt = new Date().toISOString();
      }
    });

    res.status(202).json({ success: true, data: job });
  } catch (error) {
    next(error);
  }
});

// POST /api/jobs/generate-reports — trigger report generation
jobRouter.post('/generate-reports', authorize('ADMIN'), async (req: AuthRequest, res: Response, next: NextFunction) => {
  try {
    const jobId = `job-${Date.now()}`;
    const job: JobState = {
      id: jobId,
      type: 'generate-reports',
      status: 'RUNNING',
      progress: 0,
      result: null,
      error: null,
      startedAt: new Date().toISOString(),
      completedAt: null,
    };
    jobStore.set(jobId, job);

    // Simulate report generation
    setTimeout(() => {
      const j = jobStore.get(jobId);
      if (j) {
        j.status = 'COMPLETED';
        j.progress = 100;
        j.result = { reportsGenerated: ['positions', 'audit', 'statistics'] };
        j.completedAt = new Date().toISOString();
      }
    }, 2000);

    res.status(202).json({ success: true, data: job });
  } catch (error) {
    next(error);
  }
});

// GET /api/jobs/status — job status
jobRouter.get('/status', async (_req: AuthRequest, res: Response) => {
  const jobs = Array.from(jobStore.values())
    .sort((a, b) => new Date(b.startedAt).getTime() - new Date(a.startedAt).getTime())
    .slice(0, 20);
  res.json({ success: true, data: jobs });
});

// GET /api/jobs/:id — get specific job status
jobRouter.get('/:id', async (req: AuthRequest, res: Response) => {
  const job = jobStore.get(req.params.id as string);
  if (!job) {
    res.status(404).json({
      success: false,
      error: { code: 'VL02', message: 'Job not found', category: 'VL', severity: 4 },
    });
    return;
  }
  res.json({ success: true, data: job });
});

// Transaction processing pipeline (TRNVAL00 -> POSUPD00 -> HISTLD00)
async function processTransactions(jobId: string, userId: string): Promise<void> {
  const job = jobStore.get(jobId);
  if (!job) return;

  // Step 1: Validate pending transactions (TRNVAL00)
  job.progress = 10;
  const pendingTransactions = await prisma.transaction.findMany({
    where: { status: 'PENDING' },
    include: { portfolio: true },
  });

  if (pendingTransactions.length === 0) {
    job.status = 'COMPLETED';
    job.progress = 100;
    job.result = { processed: 0, message: 'No pending transactions' };
    job.completedAt = new Date().toISOString();
    return;
  }

  let processed = 0;
  let failed = 0;

  for (const txn of pendingTransactions) {
    try {
      // Step 2: Update positions (POSUPD00)
      job.progress = 10 + Math.floor((processed / pendingTransactions.length) * 70);

      const positionDate = txn.transactionDate;
      const existingPosition = await prisma.position.findFirst({
        where: {
          portfolioId: txn.portfolioId,
          investmentId: txn.investmentId,
          status: 'ACTIVE',
        },
      });

      if (txn.type === 'BUY' || txn.type === 'SELL') {
        const qtyChange = txn.type === 'BUY' ? Number(txn.quantity) : -Number(txn.quantity);
        const existingQty = existingPosition ? Number(existingPosition.quantity) : 0;
        const existingCost = existingPosition ? Number(existingPosition.costBasis) : 0;
        const avgCostPerUnit = existingQty > 0 ? existingCost / existingQty : 0;
        const newQty = existingQty + qtyChange;
        const costDeducted = txn.type === 'SELL' ? avgCostPerUnit * Number(txn.quantity) : 0;
        const newCostBasis = txn.type === 'BUY'
          ? existingCost + Number(txn.amount)
          : existingCost - costDeducted;

        await prisma.position.upsert({
          where: existingPosition
            ? { id: existingPosition.id }
            : { portfolioId_investmentId_positionDate: { portfolioId: txn.portfolioId, investmentId: txn.investmentId, positionDate } },
          update: { quantity: newQty, costBasis: newCostBasis, marketValue: newQty * Number(txn.price), lastUser: userId.substring(0, 8) },
          create: {
            portfolioId: txn.portfolioId,
            investmentId: txn.investmentId,
            positionDate,
            quantity: newQty,
            costBasis: newCostBasis,
            marketValue: newQty * Number(txn.price),
            lastUser: userId.substring(0, 8),
          },
        });
      }

      // Step 3: Load history (HISTLD00)
      await prisma.positionHistory.create({
        data: {
          accountNo: txn.portfolio.accountNo.substring(0, 8),
          portfolioId: txn.portfolioId,
          transDate: txn.transactionDate,
          transTime: txn.transactionTime,
          transType: txn.type === 'BUY' ? 'BU' : txn.type === 'SELL' ? 'SL' : txn.type === 'TRANSFER' ? 'TR' : 'FE',
          securityId: txn.investmentId,
          quantity: txn.quantity,
          price: txn.price,
          amount: txn.amount,
          fees: 0,
          totalAmount: txn.amount,
          costBasis: (() => {
            if (!existingPosition) return txn.amount;
            if (txn.type === 'SELL') {
              const avg = Number(existingPosition.quantity) > 0
                ? Number(existingPosition.costBasis) / Number(existingPosition.quantity)
                : 0;
              return avg * Number(txn.quantity);
            }
            return txn.amount;
          })(),
          gainLoss: (() => {
            if (txn.type !== 'SELL' || !existingPosition) return 0;
            const avg = Number(existingPosition.quantity) > 0
              ? Number(existingPosition.costBasis) / Number(existingPosition.quantity)
              : 0;
            const proportionalCost = avg * Number(txn.quantity);
            return Number(txn.amount) - proportionalCost;
          })(),
          processDate: new Date(),
          programId: 'HISTLD00',
          userId: userId.substring(0, 8),
        },
      });

      // Mark transaction as done
      await prisma.transaction.update({
        where: { id: txn.id },
        data: { status: 'DONE', processedAt: new Date(), processUser: userId.substring(0, 8) },
      });

      processed++;
    } catch {
      await prisma.transaction.update({
        where: { id: txn.id },
        data: { status: 'FAILED', processedAt: new Date(), processUser: userId.substring(0, 8) },
      });
      failed++;
    }
  }

  job.status = 'COMPLETED';
  job.progress = 100;
  job.result = { processed, failed, total: pendingTransactions.length };
  job.completedAt = new Date().toISOString();
}
