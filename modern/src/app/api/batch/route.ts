import { NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";

export async function GET() {
  const runs = await prisma.batchRun.findMany({ orderBy: { startedAt: "desc" }, take: 20 });
  return NextResponse.json(runs);
}

export async function POST() {
  const portfolios = await prisma.portfolio.findMany({
    where: { status: "A" },
    include: { positions: true },
  });

  const run = await prisma.batchRun.create({
    data: { status: "RUNNING", totalItems: portfolios.length, processed: 0, errors: 0 },
  });

  let processed = 0;
  let errors = 0;

  for (const p of portfolios) {
    try {
      const totalValue = p.positions.reduce((sum, pos) => sum + pos.marketValue, 0) + p.cashBalance;
      await prisma.portfolio.update({ where: { id: p.id }, data: { totalValue } });
      processed++;
    } catch {
      errors++;
    }
  }

  const completed = await prisma.batchRun.update({
    where: { id: run.id },
    data: {
      status: errors > 0 ? "COMPLETED" : "COMPLETED",
      processed,
      errors,
      completedAt: new Date(),
    },
  });

  return NextResponse.json(completed, { status: 201 });
}
