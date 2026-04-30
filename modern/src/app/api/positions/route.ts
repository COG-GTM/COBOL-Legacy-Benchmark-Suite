import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";

export async function GET(request: NextRequest) {
  const { searchParams } = new URL(request.url);
  const portfolioId = searchParams.get("portfolioId");

  if (!portfolioId) {
    return NextResponse.json(
      { error: "portfolioId query parameter is required" },
      { status: 400 },
    );
  }

  const positions = await prisma.position.findMany({
    where: { portfolioId },
    orderBy: { investmentId: "asc" },
  });

  return NextResponse.json(positions);
}
