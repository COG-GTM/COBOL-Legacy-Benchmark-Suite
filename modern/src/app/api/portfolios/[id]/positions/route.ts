import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";

export async function GET(
  _req: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  const { id } = await params;
  const positions = await prisma.position.findMany({
    where: { portfolioId: id },
    orderBy: { marketValue: "desc" },
  });
  return NextResponse.json(positions);
}
