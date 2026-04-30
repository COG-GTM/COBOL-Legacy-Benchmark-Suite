import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";

export async function GET(req: NextRequest) {
  const search = req.nextUrl.searchParams.get("search") ?? "";
  const status = req.nextUrl.searchParams.get("status") ?? "";

  const where: Record<string, unknown> = {};
  if (search) {
    where.OR = [
      { clientName: { contains: search } },
      { accountNo: { contains: search } },
    ];
  }
  if (status) where.status = status;

  const [portfolios, total] = await Promise.all([
    prisma.portfolio.findMany({ where, orderBy: { createdAt: "desc" } }),
    prisma.portfolio.count({ where }),
  ]);

  return NextResponse.json({ portfolios, total });
}

export async function POST(req: NextRequest) {
  const body = await req.json();
  const { accountNo, clientName, clientType, cashBalance } = body;

  if (!accountNo || !clientName) {
    return NextResponse.json({ error: "accountNo and clientName are required" }, { status: 400 });
  }

  const existing = await prisma.portfolio.findUnique({ where: { accountNo } });
  if (existing) {
    return NextResponse.json({ error: "Account number already exists" }, { status: 409 });
  }

  const portfolio = await prisma.portfolio.create({
    data: {
      accountNo,
      clientName,
      clientType: clientType ?? "I",
      cashBalance: cashBalance ?? 0,
      totalValue: cashBalance ?? 0,
    },
  });

  await prisma.auditLog.create({
    data: { action: "CREATE", key: accountNo, reason: "Portfolio created", status: "SUCC", portfolioId: portfolio.id },
  });

  return NextResponse.json(portfolio, { status: 201 });
}
