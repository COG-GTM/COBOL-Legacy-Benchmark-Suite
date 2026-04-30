/**
 * Integration tests — batch pipeline:
 *  submit transactions → run batch → verify positions updated and history loaded
 *
 * Requires: DATABASE_URL pointing to a test database
 */

import { describe, it, expect, beforeAll, afterAll, beforeEach } from "vitest";
import { PrismaClient, TransactionType, TransactionStatus } from "@prisma/client";
import { BatchTransactionValidator } from "@/services/batch/transactionValidator";
import { PositionUpdater } from "@/services/batch/positionUpdater";

const TEST_DB_URL = process.env.DATABASE_URL;
const describeIntegration = TEST_DB_URL ? describe : describe.skip;

describeIntegration("Batch Pipeline Integration", () => {
  let prisma: PrismaClient;

  beforeAll(async () => {
    prisma = new PrismaClient();
    await prisma.$connect();
  });

  afterAll(async () => {
    await prisma.$disconnect();
  });

  beforeEach(async () => {
    await prisma.auditLog.deleteMany({});
    await prisma.transaction.deleteMany({});
    await prisma.position.deleteMany({});
    await prisma.batchJob.deleteMany({});
    await prisma.portfolio.deleteMany({});

    await prisma.portfolio.create({
      data: {
        id: "PORT0001",
        accountNo: "1234567890",
        clientName: "Batch Test Client",
      },
    });
  });

  it("validates, processes, and updates positions for a batch of transactions", async () => {
    // Step 1: Create pending transactions
    await prisma.transaction.createMany({
      data: [
        {
          portfolioId: "PORT0001",
          investmentId: "AAPL",
          type: TransactionType.BUY,
          quantity: 100,
          price: 150,
          amount: 15000,
          status: TransactionStatus.PENDING,
        },
        {
          portfolioId: "PORT0001",
          investmentId: "GOOGL",
          type: TransactionType.BUY,
          quantity: 50,
          price: 2800,
          amount: 140000,
          status: TransactionStatus.PENDING,
        },
      ],
    });

    // Step 2: Validate the batch
    const validator = new BatchTransactionValidator(prisma);
    const validationResult = await validator.validate([
      {
        portfolioId: "PORT0001",
        investmentId: "AAPL",
        type: TransactionType.BUY,
        quantity: 100,
        price: 150,
        amount: 15000,
      },
      {
        portfolioId: "PORT0001",
        investmentId: "GOOGL",
        type: TransactionType.BUY,
        quantity: 50,
        price: 2800,
        amount: 140000,
      },
    ]);
    expect(validationResult.valid).toBe(true);

    // Step 3: Run position updater
    const updater = new PositionUpdater(prisma);
    const result = await updater.updatePositions("PORT0001");

    expect(result.positionsUpdated).toBe(2);
    expect(result.errors).toHaveLength(0);

    // Step 4: Verify positions
    const positions = await prisma.position.findMany({
      where: { portfolioId: "PORT0001" },
      orderBy: { investmentId: "asc" },
    });
    expect(positions).toHaveLength(2);
    expect(Number(positions[0].quantity)).toBe(100); // AAPL
    expect(Number(positions[1].quantity)).toBe(50);  // GOOGL

    // Step 5: Verify transactions marked as DONE
    const doneTransactions = await prisma.transaction.findMany({
      where: { portfolioId: "PORT0001", status: TransactionStatus.DONE },
    });
    expect(doneTransactions).toHaveLength(2);

    // Step 6: Verify portfolio total recalculated
    const portfolio = await prisma.portfolio.findUnique({
      where: { id: "PORT0001" },
    });
    expect(Number(portfolio!.totalValue)).toBe(155000); // 15000 + 140000
  });

  it("handles mixed success and failure in a batch", async () => {
    // Create a position so we can sell
    await prisma.position.create({
      data: {
        portfolioId: "PORT0001",
        investmentId: "AAPL",
        quantity: 50,
        costBasis: 7500,
        marketValue: 8500,
      },
    });

    await prisma.transaction.createMany({
      data: [
        {
          portfolioId: "PORT0001",
          investmentId: "AAPL",
          type: TransactionType.SELL,
          quantity: 30,
          price: 175,
          amount: 5250,
          status: TransactionStatus.PENDING,
        },
        {
          portfolioId: "PORT0001",
          investmentId: "AAPL",
          type: TransactionType.SELL,
          quantity: 100, // more than remaining — should fail
          price: 175,
          amount: 17500,
          status: TransactionStatus.PENDING,
        },
      ],
    });

    const updater = new PositionUpdater(prisma);
    const result = await updater.updatePositions("PORT0001");

    // First sell succeeds, second fails
    expect(result.positionsUpdated).toBe(1);
    expect(result.errors).toHaveLength(1);
    expect(result.errors[0]).toContain("Insufficient units for sale");

    // Verify the first sell was applied
    const position = await prisma.position.findUnique({
      where: {
        portfolioId_investmentId: {
          portfolioId: "PORT0001",
          investmentId: "AAPL",
        },
      },
    });
    expect(Number(position!.quantity)).toBe(20); // 50 - 30
  });

  it("creates batch job record for tracking", async () => {
    const job = await prisma.batchJob.create({
      data: {
        jobName: "BATCHRUN",
        processDate: "20240101",
        status: "ACTIVE",
        programName: "POSUPDT",
        startedAt: new Date(),
      },
    });

    expect(job.status).toBe("ACTIVE");
    expect(job.jobName).toBe("BATCHRUN");

    // Update to DONE
    const updated = await prisma.batchJob.update({
      where: { id: job.id },
      data: {
        status: "DONE",
        returnCode: 0,
        completedAt: new Date(),
      },
    });

    expect(updated.status).toBe("DONE");
    expect(updated.returnCode).toBe(0);
  });
});
