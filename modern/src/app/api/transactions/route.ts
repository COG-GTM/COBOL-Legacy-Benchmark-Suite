import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { TransactionProcessor } from "@/services/transactions/processor";
import { z } from "zod";

const TransactionSchema = z.object({
  portfolioId: z.string().min(1),
  investmentId: z.string().min(1),
  type: z.enum(["BUY", "SELL", "TRANSFER", "FEE"]),
  quantity: z.number().positive(),
  price: z.number().min(0),
  amount: z.number().positive(),
  currency: z.string().length(3).default("USD"),
});

export async function GET(request: NextRequest) {
  const { searchParams } = new URL(request.url);
  const portfolioId = searchParams.get("portfolioId");
  const status = searchParams.get("status");

  const where: Record<string, unknown> = {};
  if (portfolioId) where.portfolioId = portfolioId;
  if (status) where.status = status;

  const transactions = await prisma.transaction.findMany({
    where,
    orderBy: { createdAt: "desc" },
    include: { portfolio: true },
  });

  return NextResponse.json(transactions);
}

export async function POST(request: NextRequest) {
  const body = await request.json();
  const parsed = TransactionSchema.safeParse(body);

  if (!parsed.success) {
    return NextResponse.json(
      { error: "Validation failed", details: parsed.error.flatten() },
      { status: 400 },
    );
  }

  const processor = new TransactionProcessor(prisma);
  const result = await processor.process(parsed.data);

  if (!result.success) {
    return NextResponse.json({ error: result.error }, { status: 400 });
  }

  return NextResponse.json(
    { transactionId: result.transactionId, status: "DONE" },
    { status: 201 },
  );
}
