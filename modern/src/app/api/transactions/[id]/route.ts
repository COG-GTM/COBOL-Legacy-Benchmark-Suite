/**
 * GET /api/transactions/[id] — Get transaction status
 *
 * Retrieves the status and details of a specific transaction by ID.
 */

import { NextRequest, NextResponse } from "next/server";
import { prisma } from "../../../../lib/prisma";

export async function GET(
  _request: NextRequest,
  { params }: { params: { id: string } }
) {
  try {
    const transaction = await prisma.transaction.findUnique({
      where: { id: params.id },
      include: {
        portfolio: {
          select: {
            portfolioId: true,
            accountNo: true,
            clientName: true,
          },
        },
      },
    });

    if (!transaction) {
      return NextResponse.json(
        { error: `Transaction not found: ${params.id}` },
        { status: 404 }
      );
    }

    return NextResponse.json({
      id: transaction.id,
      portfolioId: transaction.portfolioId,
      investmentId: transaction.investmentId,
      type: transaction.type,
      quantity: transaction.quantity.toString(),
      price: transaction.price.toString(),
      amount: transaction.amount.toString(),
      currency: transaction.currency,
      status: transaction.status,
      trnDate: transaction.trnDate.toISOString(),
      trnTime: transaction.trnTime,
      sequenceNo: transaction.sequenceNo,
      processDate: transaction.processDate?.toISOString() ?? null,
      processUser: transaction.processUser,
      targetPortfolioId: transaction.targetPortfolioId,
      portfolio: transaction.portfolio,
    });
  } catch (error) {
    const message =
      error instanceof Error ? error.message : "Internal server error";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
