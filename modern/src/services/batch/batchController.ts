/**
 * Batch Controller
 * Ported from: src/programs/batch/BCHCTL00.cbl
 *
 * Orchestrates the batch pipeline: TRNVAL00 -> POSUPD00 -> HISTLD00 -> Reports
 * Implements RC-gating: only proceed if previous step returns RC <= 4
 * (see documentation/technical/system-architecture.md lines 469-476).
 *
 * Replaces checkpoint/restart logic (from CKPRST.cpy) with idempotent
 * job processing. Tracks job status, allows re-runs from failure point.
 */

import { prisma } from "../../lib/prisma";
import { validateTransactions } from "./transactionValidator";
import { updatePositions } from "./positionUpdater";
import { loadHistory } from "./historyLoader";
import {
  BatchJobStatus,
  ReturnCode,
  type TransactionInput,
  type BatchPipelineResult,
  type BatchStepResult,
} from "../../types";

const RC_GATE_THRESHOLD = ReturnCode.WARNING; // RC <= 4 to proceed

interface PipelineStep {
  stepName: string;
  programName: string;
  execute: (
    context: PipelineContext
  ) => Promise<{ returnCode: number; recordsProcessed: number; errors: number }>;
}

interface PipelineContext {
  transactions: TransactionInput[];
  runDate: Date;
  jobId: string;
}

/**
 * Run the full batch pipeline.
 * Mirrors BCHCTL00 orchestration with RC-gating between steps.
 *
 * Pipeline flow (from system-architecture.md):
 *   Start of Day -> TRNVAL00 -> (RC<=4) -> POSUPD00 -> (RC<=4) -> HISTLD00 -> (RC<=4) -> Reports -> End of Day
 */
export async function runBatchPipeline(
  transactions: TransactionInput[]
): Promise<BatchPipelineResult> {
  const runDate = new Date();
  const steps: BatchStepResult[] = [];

  // Create or resume batch job
  const job = await getOrCreateJob(runDate);

  await prisma.batchJob.update({
    where: { id: job.id },
    data: {
      status: BatchJobStatus.ACTIVE,
      startTime: job.startTime ?? new Date(),
      attemptTs: new Date(),
    },
  });

  const context: PipelineContext = {
    transactions,
    runDate,
    jobId: job.id,
  };

  const pipelineSteps: PipelineStep[] = [
    {
      stepName: "TRNVAL",
      programName: "TRNVAL00",
      execute: async (ctx) => {
        const result = await validateTransactions(ctx.transactions);
        return {
          returnCode: result.returnCode,
          recordsProcessed: result.validCount + result.errorCount,
          errors: result.errorCount,
        };
      },
    },
    {
      stepName: "POSUPD",
      programName: "POSUPD00",
      execute: async (ctx) => {
        const result = await updatePositions(ctx.transactions, ctx.runDate);
        return {
          returnCode: result.returnCode,
          recordsProcessed: result.recordsProcessed,
          errors: result.errorsEncountered,
        };
      },
    },
    {
      stepName: "HISTLD",
      programName: "HISTLD00",
      execute: async (ctx) => {
        const result = await loadHistory([], ctx.runDate);
        return {
          returnCode: result.returnCode,
          recordsProcessed: result.recordsWritten,
          errors: result.errorsEncountered,
        };
      },
    },
  ];

  // Find the step to resume from (if re-running from failure)
  const resumeStepIndex = await getResumeStepIndex(job.id, pipelineSteps);

  let lastReturnCode: number = ReturnCode.SUCCESS;

  for (let i = resumeStepIndex; i < pipelineSteps.length; i++) {
    const step = pipelineSteps[i];
    const stepStart = new Date();

    await updateJobStep(job.id, step.stepName, step.programName);

    try {
      const result = await step.execute(context);
      const stepEnd = new Date();

      const stepResult: BatchStepResult = {
        stepName: step.stepName,
        programName: step.programName,
        returnCode: result.returnCode,
        recordsProcessed: result.recordsProcessed,
        errorsEncountered: result.errors,
        startTime: stepStart,
        endTime: stepEnd,
      };
      steps.push(stepResult);

      await recordStepCompletion(job.id, step, result.returnCode);
      lastReturnCode = result.returnCode;

      // RC-gating: halt if RC > 4
      if (result.returnCode > RC_GATE_THRESHOLD) {
        await prisma.batchJob.update({
          where: { id: job.id },
          data: {
            status: BatchJobStatus.ERROR,
            returnCode: result.returnCode,
            errorDesc: `Pipeline halted at ${step.stepName}: RC=${result.returnCode}`,
            endTime: new Date(),
          },
        });

        return {
          jobId: job.id,
          status: BatchJobStatus.ERROR,
          steps,
          startTime: job.startTime ?? stepStart,
          endTime: new Date(),
        };
      }
    } catch (error) {
      const stepEnd = new Date();
      const errorMessage =
        error instanceof Error ? error.message : "Unknown error";

      steps.push({
        stepName: step.stepName,
        programName: step.programName,
        returnCode: ReturnCode.SEVERE,
        recordsProcessed: 0,
        errorsEncountered: 1,
        startTime: stepStart,
        endTime: stepEnd,
      });

      await prisma.batchJob.update({
        where: { id: job.id },
        data: {
          status: BatchJobStatus.ERROR,
          returnCode: ReturnCode.SEVERE,
          errorDesc: `${step.stepName} failed: ${errorMessage}`,
          endTime: new Date(),
        },
      });

      return {
        jobId: job.id,
        status: BatchJobStatus.ERROR,
        steps,
        startTime: job.startTime ?? stepStart,
        endTime: new Date(),
      };
    }
  }

  // Pipeline completed successfully
  await prisma.batchJob.update({
    where: { id: job.id },
    data: {
      status: BatchJobStatus.DONE,
      returnCode: lastReturnCode,
      endTime: new Date(),
      completeTs: new Date(),
    },
  });

  return {
    jobId: job.id,
    status: BatchJobStatus.DONE,
    steps,
    startTime: job.startTime ?? new Date(),
    endTime: new Date(),
  };
}

/**
 * Get the status of a batch job.
 */
export async function getBatchJobStatus(
  jobId: string
): Promise<BatchPipelineResult | null> {
  const job = await prisma.batchJob.findUnique({
    where: { id: jobId },
  });

  if (!job) return null;

  return {
    jobId: job.id,
    status: job.status as BatchPipelineResult["status"],
    steps: [],
    startTime: job.startTime ?? new Date(),
    endTime: job.endTime ?? undefined,
  };
}

// --- Internal helpers ---

async function getOrCreateJob(
  runDate: Date
): Promise<{ id: string; startTime: Date | null }> {
  const today = new Date(runDate);
  today.setHours(0, 0, 0, 0);

  const existing = await prisma.batchJob.findFirst({
    where: {
      jobName: "BCHCTL00",
      processDate: today,
      status: { in: [BatchJobStatus.READY, BatchJobStatus.ERROR] },
    },
    orderBy: { sequenceNo: "desc" },
  });

  if (existing) {
    if (existing.status === BatchJobStatus.ERROR) {
      await prisma.batchJob.update({
        where: { id: existing.id },
        data: {
          restartCount: existing.restartCount + 1,
          status: BatchJobStatus.READY,
        },
      });
    }
    return { id: existing.id, startTime: existing.startTime };
  }

  const lastSeq = await prisma.batchJob.findFirst({
    where: { jobName: "BCHCTL00", processDate: today },
    orderBy: { sequenceNo: "desc" },
    select: { sequenceNo: true },
  });

  const newJob = await prisma.batchJob.create({
    data: {
      jobName: "BCHCTL00",
      processDate: today,
      sequenceNo: (lastSeq?.sequenceNo ?? 0) + 1,
      status: BatchJobStatus.READY,
    },
  });

  return { id: newJob.id, startTime: null };
}

async function getResumeStepIndex(
  jobId: string,
  steps: PipelineStep[]
): Promise<number> {
  const job = await prisma.batchJob.findUnique({
    where: { id: jobId },
    select: { stepName: true, returnCode: true },
  });

  if (!job?.stepName) return 0;

  const lastStepIndex = steps.findIndex((s) => s.stepName === job.stepName);
  if (lastStepIndex < 0) return 0;

  // If the last step succeeded (RC <= 4), resume from the next step
  if (job.returnCode !== null && job.returnCode <= RC_GATE_THRESHOLD) {
    return lastStepIndex + 1;
  }

  // Otherwise, re-run the failed step
  return lastStepIndex;
}

async function updateJobStep(
  jobId: string,
  stepName: string,
  programName: string
): Promise<void> {
  await prisma.batchJob.update({
    where: { id: jobId },
    data: { stepName, programName },
  });
}

async function recordStepCompletion(
  jobId: string,
  step: PipelineStep,
  returnCode: number
): Promise<void> {
  await prisma.batchJob.update({
    where: { id: jobId },
    data: {
      returnCode,
      stepName: step.stepName,
      programName: step.programName,
    },
  });
}
