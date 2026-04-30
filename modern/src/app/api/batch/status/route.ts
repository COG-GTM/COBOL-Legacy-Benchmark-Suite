/**
 * GET /api/batch/status — Get batch job status
 *
 * Retrieves the status of a batch job by ID, or lists recent batch jobs.
 * Maps to BCHCTL00.cbl's FUNC-CHEK (check prerequisites/status) functionality.
 */

import { NextRequest, NextResponse } from "next/server";
import { prisma } from "../../../../lib/prisma";
import { getBatchJobStatus } from "../../../../services/batch/batchController";

export async function GET(request: NextRequest) {
  try {
    const { searchParams } = new URL(request.url);
    const jobId = searchParams.get("jobId");

    if (jobId) {
      const status = await getBatchJobStatus(jobId);

      if (!status) {
        return NextResponse.json(
          { error: `Batch job not found: ${jobId}` },
          { status: 404 }
        );
      }

      return NextResponse.json({
        jobId: status.jobId,
        status: status.status,
        steps: status.steps,
        startTime: status.startTime.toISOString(),
        endTime: status.endTime?.toISOString() ?? null,
      });
    }

    // List recent batch jobs
    const jobs = await prisma.batchJob.findMany({
      orderBy: { processDate: "desc" },
      take: 20,
    });

    return NextResponse.json({
      jobs: jobs.map((job) => ({
        id: job.id,
        jobName: job.jobName,
        processDate: job.processDate.toISOString(),
        sequenceNo: job.sequenceNo,
        status: job.status,
        stepName: job.stepName,
        programName: job.programName,
        returnCode: job.returnCode,
        errorDesc: job.errorDesc,
        startTime: job.startTime?.toISOString() ?? null,
        endTime: job.endTime?.toISOString() ?? null,
        restartCount: job.restartCount,
      })),
    });
  } catch (error) {
    const message =
      error instanceof Error ? error.message : "Internal server error";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
