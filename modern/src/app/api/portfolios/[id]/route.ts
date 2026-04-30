import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";

export async function GET(
  _request: NextRequest,
  { params }: { params: Promise<{ id: string }> },
) {
  const { id } = await params;
  const portfolio = await prisma.portfolio.findUnique({
    where: { id },
    include: { positions: true, transactions: { orderBy: { createdAt: "desc" } } },
  });

  if (!portfolio) {
    return NextResponse.json({ error: "Portfolio not found" }, { status: 404 });
  }

  return NextResponse.json(portfolio);
}

export async function PUT(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> },
) {
  const { id } = await params;
  const body = await request.json();

  try {
    const portfolio = await prisma.portfolio.update({
      where: { id },
      data: {
        clientName: body.clientName,
        clientType: body.clientType,
        status: body.status,
      },
    });
    return NextResponse.json(portfolio);
  } catch {
    return NextResponse.json({ error: "Portfolio not found" }, { status: 404 });
  }
}

export async function DELETE(
  _request: NextRequest,
  { params }: { params: Promise<{ id: string }> },
) {
  const { id } = await params;

  try {
    await prisma.portfolio.update({
      where: { id },
      data: { status: "CLOSED" },
    });
    return NextResponse.json({ message: "Portfolio closed" });
  } catch {
    return NextResponse.json({ error: "Portfolio not found" }, { status: 404 });
  }
}
