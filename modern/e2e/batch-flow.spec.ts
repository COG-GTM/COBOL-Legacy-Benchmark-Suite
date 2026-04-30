/**
 * E2E tests — Batch flow
 * Covers the same scenarios as TSTGEN00.cbl and TSTEXEC.cbl:
 *  - Trigger batch run
 *  - Verify completion
 *  - Check reports (batch job records)
 */

import { test, expect } from "@playwright/test";

const BASE_URL = process.env.BASE_URL ?? "http://localhost:3000";

test.describe("Batch Flow", () => {
  test.beforeAll(async ({ request }) => {
    // Set up test portfolio with pending transactions
    await request.post(`${BASE_URL}/api/portfolios`, {
      data: {
        id: "PORT2001",
        accountNo: "8000000001",
        clientName: "Batch E2E Client",
      },
    });
  });

  test("trigger batch run and verify completion", async ({ request }) => {
    // Submit some transactions first (they'll be DONE immediately via processor)
    // Then create pending transactions directly for batch
    await request.post(`${BASE_URL}/api/transactions`, {
      data: {
        portfolioId: "PORT2001",
        investmentId: "AAPL",
        type: "BUY",
        quantity: 100,
        price: 150,
        amount: 15000,
      },
    });

    // Trigger batch run
    const batchResponse = await request.post(`${BASE_URL}/api/batch`, {
      data: {
        portfolioId: "PORT2001",
        transactions: [
          {
            portfolioId: "PORT2001",
            investmentId: "AAPL",
            type: "BUY",
            quantity: 100,
            price: 150,
            amount: 15000,
          },
        ],
      },
    });

    expect(batchResponse.ok()).toBeTruthy();
    const batchBody = await batchResponse.json();
    expect(batchBody.jobId).toBeDefined();
    expect(["DONE", "ERROR"]).toContain(batchBody.status);
  });

  test("batch validation rejects invalid transactions", async ({
    request,
  }) => {
    const response = await request.post(`${BASE_URL}/api/batch`, {
      data: {
        portfolioId: "PORT2001",
        transactions: [
          {
            portfolioId: "",
            investmentId: "AAPL",
            type: "BUY",
            quantity: 10,
            price: 150,
            amount: 1500,
          },
        ],
      },
    });

    expect(response.status()).toBe(400);
    const body = await response.json();
    expect(body.status).toBe("ERROR");
    expect(body.validation.valid).toBe(false);
  });

  test("view batch job history", async ({ request }) => {
    const response = await request.get(`${BASE_URL}/api/batch`);
    expect(response.ok()).toBeTruthy();
    const jobs = await response.json();
    expect(Array.isArray(jobs)).toBe(true);
  });

  test("batch requires portfolioId", async ({ request }) => {
    const response = await request.post(`${BASE_URL}/api/batch`, {
      data: {},
    });

    expect(response.status()).toBe(400);
    const body = await response.json();
    expect(body.error).toContain("portfolioId");
  });

  test("verify positions after batch processing", async ({ request }) => {
    // Create fresh portfolio for clean test
    await request.post(`${BASE_URL}/api/portfolios`, {
      data: {
        id: "PORT2002",
        accountNo: "8000000002",
        clientName: "Batch Verify Client",
      },
    });

    // BUY via direct API — creates position
    await request.post(`${BASE_URL}/api/transactions`, {
      data: {
        portfolioId: "PORT2002",
        investmentId: "GOOGL",
        type: "BUY",
        quantity: 30,
        price: 2800,
        amount: 84000,
      },
    });

    // Run batch to recalculate
    await request.post(`${BASE_URL}/api/batch`, {
      data: { portfolioId: "PORT2002" },
    });

    // Verify positions reflect the transactions
    const posResponse = await request.get(
      `${BASE_URL}/api/positions?portfolioId=PORT2002`,
    );
    expect(posResponse.ok()).toBeTruthy();
    const positions = await posResponse.json();
    expect(positions.length).toBeGreaterThanOrEqual(1);

    const googl = positions.find(
      (p: { investmentId: string }) => p.investmentId === "GOOGL",
    );
    expect(googl).toBeDefined();
    expect(Number(googl.quantity)).toBe(30);
  });

  test("batch FEE deducts from cost basis", async ({ request }) => {
    await request.post(`${BASE_URL}/api/portfolios`, {
      data: {
        id: "PORT2003",
        accountNo: "8000000003",
        clientName: "Fee Test Client",
      },
    });

    // First buy
    await request.post(`${BASE_URL}/api/transactions`, {
      data: {
        portfolioId: "PORT2003",
        investmentId: "BND001",
        type: "BUY",
        quantity: 100,
        price: 100,
        amount: 10000,
      },
    });

    // Then charge fee
    const feeResponse = await request.post(`${BASE_URL}/api/transactions`, {
      data: {
        portfolioId: "PORT2003",
        investmentId: "BND001",
        type: "FEE",
        quantity: 1,
        price: 50,
        amount: 50,
      },
    });

    expect(feeResponse.status()).toBe(201);

    // Verify cost basis reduced
    const posResponse = await request.get(
      `${BASE_URL}/api/positions?portfolioId=PORT2003`,
    );
    const positions = await posResponse.json();
    const bond = positions.find(
      (p: { investmentId: string }) => p.investmentId === "BND001",
    );
    expect(Number(bond.costBasis)).toBe(9950); // 10000 - 50
  });
});
