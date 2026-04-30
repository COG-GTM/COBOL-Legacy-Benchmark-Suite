import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { validatePortfolioId, validateAccountNumber } from "@/services/portfolio/validation";
import { z } from "zod";

const CreatePortfolioSchema = z.object({
  id: z.string().length(8),
  accountNo: z.string().length(10),
  clientName: z.string().min(1).max(30),
  clientType: z.enum(["INDIVIDUAL", "CORPORATE", "TRUST"]).default("INDIVIDUAL"),
});

export async function GET(request: NextRequest) {
  const { searchParams } = new URL(request.url);
  const status = searchParams.get("status");
  const search = searchParams.get("search");

  const where: Record<string, unknown> = {};
  if (status) where.status = status;
  if (search) {
    where.OR = [
      { id: { contains: search, mode: "insensitive" } },
      { clientName: { contains: search, mode: "insensitive" } },
      { accountNo: { contains: search } },
    ];
  }

  const portfolios = await prisma.portfolio.findMany({
    where,
    include: { positions: true },
    orderBy: { createdAt: "desc" },
  });

  return NextResponse.json(portfolios);
}

export async function POST(request: NextRequest) {
  const body = await request.json();
  const parsed = CreatePortfolioSchema.safeParse(body);

  if (!parsed.success) {
    return NextResponse.json(
      { error: "Validation failed", details: parsed.error.flatten() },
      { status: 400 },
    );
  }

  const { id, accountNo, clientName, clientType } = parsed.data;

  const idResult = validatePortfolioId(id);
  if (idResult.code !== 0) {
    return NextResponse.json({ error: idResult.message }, { status: 400 });
  }

  const acctResult = validateAccountNumber(accountNo);
  if (acctResult.code !== 0) {
    return NextResponse.json({ error: acctResult.message }, { status: 400 });
  }

  try {
    const portfolio = await prisma.portfolio.create({
      data: { id, accountNo, clientName, clientType },
    });
    return NextResponse.json(portfolio, { status: 201 });
  } catch (err) {
    const message = err instanceof Error ? err.message : "Create failed";
    if (message.includes("Unique constraint")) {
      return NextResponse.json(
        { error: "Portfolio ID or Account Number already exists" },
        { status: 409 },
      );
    }
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
