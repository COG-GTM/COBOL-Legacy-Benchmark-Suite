/**
 * POST /api/batch/run — Trigger batch pipeline
 *
 * Triggers the batch processing pipeline: TRNVAL00 -> POSUPD00 -> HISTLD00 -> Reports.
 * Maps to BCHCTL00.cbl's batch orchestration logic.
 *
 * Accepts a list of transactions to process through the pipeline.
 * Implements RC-gating between steps (only proceeds if previous step RC <= 4).
 */

import { NextRequest, NextResponse } from "next/server";
import { Decimal } from "decimal.js";
import { runBatchPipeline } from "../../../../services/batch/batchController";
import { TransactionType, type TransactionInput } from "../../../../types";

interface BatchTransactionBody {
  portfolioId: string;
  investmentId: string;
  type: string;
  quantity: string | number;
  price: string | number;
  amount: string | number;
  currency?: string;
  targetPortfolioId?: string;
}

interface BatchRunRequestBody {
  transactions: BatchTransactionBody[];
}

export async function POST(request: NextRequest) {
  try {
    const body = (await request.json()) as BatchRunRequestBody;

    if (
      !body.transactions ||
      !Array.isArray(body.transactions) ||
      body.transactions.length === 0
    ) {
      return NextResponse.json(
        { error: "Request must include a non-empty 'transactions' array" },
        { status: 400 }
      );
    }

    const transactions: TransactionInput[] = body.transactions.map((t) => ({
      portfolioId: t.portfolioId,
      investmentId: t.investmentId,
      type: t.type as TransactionType,
      quantity: new Decimal(t.quantity.toString()),
      price: new Decimal(t.price.toString()),
      amount: new Decimal(t.amount.toString()),
      currency: t.currency,
      targetPortfolioId: t.targetPortfolioId,
    }));

    const result = await runBatchPipeline(transactions);

    return NextResponse.json({
      jobId: result.jobId,
      status: result.status,
      steps: result.steps.map((s) => ({
        stepName: s.stepName,
        programName: s.programName,
        returnCode: s.returnCode,
        recordsProcessed: s.recordsProcessed,
        errorsEncountered: s.errorsEncountered,
        startTime: s.startTime.toISOString(),
        endTime: s.endTime?.toISOString() ?? null,
      })),
      startTime: result.startTime.toISOString(),
      endTime: result.endTime?.toISOString() ?? null,
    });
  } catch (error) {
    const message =
      error instanceof Error ? error.message : "Internal server error";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
