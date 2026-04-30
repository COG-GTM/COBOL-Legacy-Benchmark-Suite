/**
 * POST /api/transactions — Submit a transaction
 *
 * Accepts a transaction request and processes it through the TransactionProcessor.
 * Maps to PORTTRAN.cbl's transaction processing logic.
 */

import { NextRequest, NextResponse } from "next/server";
import { Decimal } from "decimal.js";
import { TransactionProcessor } from "../../../services/transactions/processor";
import { TransactionType } from "../../../types";

interface TransactionRequestBody {
  portfolioId: string;
  investmentId: string;
  type: string;
  quantity: string | number;
  price: string | number;
  amount: string | number;
  currency?: string;
  targetPortfolioId?: string;
}

export async function POST(request: NextRequest) {
  try {
    const body = (await request.json()) as TransactionRequestBody;

    // Validate required fields
    if (
      !body.portfolioId ||
      !body.investmentId ||
      !body.type ||
      body.quantity === undefined ||
      body.price === undefined ||
      body.amount === undefined
    ) {
      return NextResponse.json(
        { error: "Missing required fields: portfolioId, investmentId, type, quantity, price, amount" },
        { status: 400 }
      );
    }

    const validTypes = new Set(Object.values(TransactionType));
    if (!validTypes.has(body.type as TransactionType)) {
      return NextResponse.json(
        { error: `Invalid transaction type: ${body.type}. Must be one of: BU, SL, TR, FE` },
        { status: 400 }
      );
    }

    const processor = new TransactionProcessor();
    const result = await processor.processTransaction({
      portfolioId: body.portfolioId,
      investmentId: body.investmentId,
      type: body.type as TransactionType,
      quantity: new Decimal(body.quantity.toString()),
      price: new Decimal(body.price.toString()),
      amount: new Decimal(body.amount.toString()),
      currency: body.currency,
      targetPortfolioId: body.targetPortfolioId,
    });

    const statusCode = result.status === "D" ? 200 : 422;

    return NextResponse.json(
      {
        transactionId: result.transactionId,
        status: result.status,
        returnCode: result.returnCode,
        message: result.message,
        gainLoss: result.gainLoss?.toString(),
      },
      { status: statusCode }
    );
  } catch (error) {
    const message =
      error instanceof Error ? error.message : "Internal server error";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
