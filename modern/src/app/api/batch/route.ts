import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { BatchTransactionValidator } from "@/services/batch/transactionValidator";
import { PositionUpdater } from "@/services/batch/positionUpdater";
import { BatchStatus } from "@prisma/client";

export async function GET() {
  const jobs = await prisma.batchJob.findMany({
    orderBy: { createdAt: "desc" },
    take: 50,
  });
  return NextResponse.json(jobs);
}

export async function POST(request: NextRequest) {
  const body = await request.json();
  const { portfolioId, transactions } = body;

  if (!portfolioId) {
    return NextResponse.json(
      { error: "portfolioId is required" },
      { status: 400 },
    );
  }

  const today = new Date().toISOString().slice(0, 10).replace(/-/g, "");

  const job = await prisma.batchJob.create({
    data: {
      jobName: "BATCHRUN",
      processDate: today,
      status: BatchStatus.ACTIVE,
      programName: "POSUPDT",
      startedAt: new Date(),
    },
  });

  try {
    // Validate transactions if provided
    if (transactions && transactions.length > 0) {
      const validator = new BatchTransactionValidator(prisma);
      const validationResult = await validator.validate(transactions);

      if (!validationResult.valid) {
        await prisma.batchJob.update({
          where: { id: job.id },
          data: {
            status: BatchStatus.ERROR,
            errorDesc: `Validation failed: ${validationResult.errorCount} errors`,
            errorCount: validationResult.errorCount,
            completedAt: new Date(),
          },
        });

        return NextResponse.json(
          {
            jobId: job.id,
            status: "ERROR",
            validation: validationResult,
          },
          { status: 400 },
        );
      }
    }

    // Run position updates
    const updater = new PositionUpdater(prisma);
    const result = await updater.updatePositions(portfolioId);

    const finalStatus =
      result.errors.length > 0 ? BatchStatus.ERROR : BatchStatus.DONE;

    await prisma.batchJob.update({
      where: { id: job.id },
      data: {
        status: finalStatus,
        recordsRead: result.positionsUpdated + result.errors.length,
        recordsWritten: result.positionsUpdated,
        errorCount: result.errors.length,
        returnCode: result.errors.length > 0 ? 8 : 0,
        errorDesc:
          result.errors.length > 0 ? result.errors[0] : null,
        completedAt: new Date(),
      },
    });

    return NextResponse.json({
      jobId: job.id,
      status: finalStatus,
      positionsUpdated: result.positionsUpdated,
      totalValue: result.totalValue.toString(),
      errors: result.errors,
    });
  } catch (err) {
    const message = err instanceof Error ? err.message : "Batch failed";
    await prisma.batchJob.update({
      where: { id: job.id },
      data: {
        status: BatchStatus.ERROR,
        returnCode: 16,
        errorDesc: message.substring(0, 80),
        completedAt: new Date(),
      },
    });

    return NextResponse.json(
      { jobId: job.id, status: "ERROR", error: message },
      { status: 500 },
    );
  }
}
