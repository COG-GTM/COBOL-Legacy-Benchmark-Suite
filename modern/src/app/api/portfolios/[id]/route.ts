import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";

export async function GET(
  _req: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  const { id } = await params;
  const portfolio = await prisma.portfolio.findUnique({
    where: { id },
    include: { positions: true },
  });

  if (!portfolio) {
    return NextResponse.json({ error: "Portfolio not found" }, { status: 404 });
  }

  return NextResponse.json(portfolio);
}

export async function PATCH(
  req: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  const { id } = await params;
  const body = await req.json();

  const portfolio = await prisma.portfolio.findUnique({ where: { id } });
  if (!portfolio) {
    return NextResponse.json({ error: "Portfolio not found" }, { status: 404 });
  }

  const updated = await prisma.portfolio.update({
    where: { id },
    data: {
      ...(body.clientName && { clientName: body.clientName }),
      ...(body.clientType && { clientType: body.clientType }),
      ...(body.status && { status: body.status }),
      ...(body.cashBalance !== undefined && { cashBalance: body.cashBalance }),
    },
  });

  await prisma.auditLog.create({
    data: { action: "UPDATE", key: portfolio.accountNo, reason: "Portfolio updated", status: "SUCC", portfolioId: id },
  });

  return NextResponse.json(updated);
}

export async function DELETE(
  _req: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  const { id } = await params;
  const portfolio = await prisma.portfolio.findUnique({ where: { id } });
  if (!portfolio) {
    return NextResponse.json({ error: "Portfolio not found" }, { status: 404 });
  }

  await prisma.portfolio.delete({ where: { id } });

  await prisma.auditLog.create({
    data: { action: "DELETE", key: portfolio.accountNo, reason: "Portfolio deleted", status: "SUCC" },
  });

  return NextResponse.json({ success: true });
}
