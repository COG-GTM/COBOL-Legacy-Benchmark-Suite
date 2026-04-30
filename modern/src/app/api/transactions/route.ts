import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";

export async function GET(req: NextRequest) {
  const sp = req.nextUrl.searchParams;
  const portfolioId = sp.get("portfolioId");
  const type = sp.get("type");
  const startDate = sp.get("startDate");
  const endDate = sp.get("endDate");

  const where: Record<string, unknown> = {};
  if (portfolioId) where.portfolioId = portfolioId;
  if (type) where.transactionType = type;
  if (startDate || endDate) {
    where.createdAt = {
      ...(startDate && { gte: new Date(startDate) }),
      ...(endDate && { lte: new Date(endDate) }),
    };
  }

  const [transactions, total] = await Promise.all([
    prisma.transaction.findMany({
      where,
      include: { portfolio: { select: { accountNo: true, clientName: true } } },
      orderBy: { createdAt: "desc" },
      take: 200,
    }),
    prisma.transaction.count({ where }),
  ]);

  return NextResponse.json({ transactions, total });
}

export async function POST(req: NextRequest) {
  const body = await req.json();
  const { portfolioId, transactionType, investmentType, units, price } = body;

  if (!portfolioId || !transactionType) {
    return NextResponse.json({ error: "portfolioId and transactionType are required" }, { status: 400 });
  }

  const portfolio = await prisma.portfolio.findUnique({ where: { id: portfolioId } });
  if (!portfolio) {
    return NextResponse.json({ error: "Portfolio not found" }, { status: 404 });
  }

  const amount = Math.round((units ?? 0) * (price ?? 0) * 100) / 100;
  const seqCount = await prisma.transaction.count({ where: { portfolioId } });

  const transaction = await prisma.transaction.create({
    data: {
      transactionType,
      investmentType: investmentType ?? "STK",
      units: units ?? 0,
      price: price ?? 0,
      amount,
      sequenceNo: `SEQ${String(seqCount + 1).padStart(3, "0")}`,
      portfolioId,
    },
  });

  await prisma.auditLog.create({
    data: {
      action: "CREATE",
      key: `${portfolio.accountNo}-${transaction.sequenceNo}`,
      reason: `${transactionType} transaction submitted`,
      status: "SUCC",
      portfolioId,
    },
  });

  return NextResponse.json(
    { success: true, message: "Transaction submitted successfully", transactionId: transaction.id },
    { status: 201 }
  );
}
