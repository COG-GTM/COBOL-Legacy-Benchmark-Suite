import { PrismaClient } from '@prisma/client';
import Decimal from 'decimal.js';
import {
  TransactionStatus,
  BatchJobStatus,
  TransactionType,
} from '../types/index.js';
import { updatePositionFromTransaction } from './positionService.js';

const prisma = new PrismaClient();

// BCHCTL00.cbl — Batch Control Processor
// 1000-PROCESS-INITIALIZE → 2000-CHECK-PREREQUISITES → 3000-UPDATE-STATUS → 4000-PROCESS-TERMINATE
export async function runBatchCycle(
  jobName: string = 'BATCHRUN',
  processDateStr?: string,
  onProgress?: (step: string, progress: number) => void
) {
  const processDate = processDateStr ? new Date(processDateStr) : new Date();
  const today = new Date(processDate.getFullYear(), processDate.getMonth(), processDate.getDate());
  const userId = 'BATCH';

  // 1000-PROCESS-INITIALIZE: Create batch job record
  const batchJob = await prisma.batchJob.create({
    data: {
      jobName: jobName.substring(0, 8),
      processDate: today,
      sequenceNo: await getNextSequenceNo(jobName, today),
      status: BatchJobStatus.Active,
      stepName: 'INIT',
      programName: 'BCHCTL00',
      startTime: new Date(),
      recordsRead: 0,
      recordsWritten: 0,
      errorCount: 0,
    },
  });

  try {
    // Step 1: TRNVAL00 — Validate pending transactions
    onProgress?.('Validating pending transactions', 10);
    await updateBatchStep(batchJob.id, 'TRNVAL00', 'TRNVAL00');

    const pendingTransactions = await prisma.transaction.findMany({
      where: { status: TransactionStatus.Pending },
      orderBy: { transactionDate: 'asc' },
    });

    let recordsRead = pendingTransactions.length;
    let recordsWritten = 0;
    let errorCount = 0;

    onProgress?.(`Found ${recordsRead} pending transactions`, 20);

    // Step 2: POSUPD00 — Update positions for valid transactions
    onProgress?.('Updating positions', 30);
    await updateBatchStep(batchJob.id, 'POSUPD00', 'POSUPD00');

    for (let i = 0; i < pendingTransactions.length; i++) {
      const txn = pendingTransactions[i];

      try {
        // Validate the portfolio is still active
        const portfolio = await prisma.portfolio.findUnique({
          where: { portfolioId: txn.portfolioId },
        });

        if (!portfolio || portfolio.status !== 'A') {
          await prisma.transaction.update({
            where: { transactionId: txn.transactionId },
            data: { status: TransactionStatus.Failed },
          });
          errorCount++;
          continue;
        }

        // Update position based on transaction type (POSUPD00 logic)
        await updatePositionFromTransaction(
          txn.portfolioId,
          txn.investmentId,
          txn.transactionType,
          Number(txn.quantity),
          Number(txn.price),
          userId
        );

        // Update cash balance on portfolio
        const amount = new Decimal(txn.amount.toString());
        let cashDelta: Decimal;

        if (txn.transactionType === TransactionType.Buy) {
          cashDelta = amount.negated(); // Buy reduces cash
        } else if (txn.transactionType === TransactionType.Sell) {
          cashDelta = amount; // Sell increases cash
        } else if (txn.transactionType === TransactionType.Fee) {
          cashDelta = amount.negated(); // Fee reduces cash
        } else {
          cashDelta = new Decimal(0);
        }

        await prisma.portfolio.update({
          where: { portfolioId: txn.portfolioId },
          data: {
            cashBalance: {
              increment: cashDelta.toNumber(),
            },
            lastMaintDate: new Date(),
            lastMaintUser: userId,
          },
        });

        // Mark transaction as done
        await prisma.transaction.update({
          where: { transactionId: txn.transactionId },
          data: {
            status: TransactionStatus.Done,
            processDate: new Date(),
            processUser: userId,
          },
        });

        recordsWritten++;
      } catch {
        // Mark transaction as failed
        await prisma.transaction.update({
          where: { transactionId: txn.transactionId },
          data: { status: TransactionStatus.Failed },
        });
        errorCount++;
      }

      const progress = 30 + Math.round((i / pendingTransactions.length) * 50);
      onProgress?.(`Processed ${i + 1}/${pendingTransactions.length}`, progress);
    }

    // Step 3: HISTLD00 — Load history (already done via position history in step 2)
    onProgress?.('Loading history', 85);
    await updateBatchStep(batchJob.id, 'HISTLD00', 'HISTLD00');

    // Step 4: Finalize
    onProgress?.('Finalizing batch', 95);
    await prisma.batchJob.update({
      where: { id: batchJob.id },
      data: {
        status: BatchJobStatus.Done,
        stepName: 'COMPLETE',
        endTime: new Date(),
        recordsRead,
        recordsWritten,
        errorCount,
        returnCode: errorCount > 0 ? 4 : 0,
      },
    });

    onProgress?.('Batch complete', 100);

    return {
      jobId: batchJob.id,
      jobName,
      status: BatchJobStatus.Done,
      recordsRead,
      recordsWritten,
      errorCount,
      startTime: batchJob.startTime,
      endTime: new Date(),
    };
  } catch (error) {
    // 9000-ERROR-ROUTINE
    await prisma.batchJob.update({
      where: { id: batchJob.id },
      data: {
        status: BatchJobStatus.Error,
        endTime: new Date(),
        errorDesc: error instanceof Error ? error.message.substring(0, 255) : 'Unknown error',
        returnCode: 12,
      },
    });

    throw error;
  }
}

// Get batch job status
export async function getBatchStatus() {
  const jobs = await prisma.batchJob.findMany({
    orderBy: { startTime: 'desc' },
    take: 20,
  });
  return jobs;
}

async function getNextSequenceNo(jobName: string, processDate: Date): Promise<number> {
  const lastJob = await prisma.batchJob.findFirst({
    where: {
      jobName: jobName.substring(0, 8),
      processDate,
    },
    orderBy: { sequenceNo: 'desc' },
  });
  return (lastJob?.sequenceNo ?? 0) + 1;
}

async function updateBatchStep(jobId: number, stepName: string, programName: string) {
  await prisma.batchJob.update({
    where: { id: jobId },
    data: { stepName, programName },
  });
}
