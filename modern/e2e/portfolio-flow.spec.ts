/**
 * E2E tests — Portfolio flow
 * Covers the same scenarios as TSTGEN00.cbl and TSTEXEC.cbl:
 *  - Navigate dashboard
 *  - Search portfolio
 *  - View positions
 *  - Submit transaction via API
 */

import { test, expect } from "@playwright/test";

const BASE_URL = process.env.BASE_URL ?? "http://localhost:3000";

test.describe("Portfolio Flow", () => {
  test("health check endpoint returns healthy", async ({ request }) => {
    const response = await request.get(`${BASE_URL}/api/health`);
    expect(response.ok()).toBeTruthy();
    const body = await response.json();
    expect(body.status).toBe("healthy");
    expect(body.database).toBe("connected");
  });

  test("dashboard page loads", async ({ page }) => {
    await page.goto(BASE_URL);
    await expect(page.locator("h1")).toContainText("CLBS Portfolio Manager");
  });

  test("create portfolio via API", async ({ request }) => {
    const response = await request.post(`${BASE_URL}/api/portfolios`, {
      data: {
        id: "PORT1001",
        accountNo: "9000000001",
        clientName: "E2E Test Client",
        clientType: "INDIVIDUAL",
      },
    });

    // May be 201 (created) or 409 (already exists from previous run)
    expect([201, 409]).toContain(response.status());

    if (response.status() === 201) {
      const body = await response.json();
      expect(body.id).toBe("PORT1001");
    }
  });

  test("search portfolio by ID", async ({ request }) => {
    // Ensure portfolio exists
    await request.post(`${BASE_URL}/api/portfolios`, {
      data: {
        id: "PORT1002",
        accountNo: "9000000002",
        clientName: "Search Test Client",
      },
    });

    const response = await request.get(
      `${BASE_URL}/api/portfolios?search=PORT1002`,
    );
    expect(response.ok()).toBeTruthy();
    const portfolios = await response.json();
    expect(portfolios.length).toBeGreaterThanOrEqual(1);
    expect(portfolios[0].id).toBe("PORT1002");
  });

  test("view portfolio detail with positions", async ({ request }) => {
    // Create portfolio and buy position
    await request.post(`${BASE_URL}/api/portfolios`, {
      data: {
        id: "PORT1003",
        accountNo: "9000000003",
        clientName: "Position Test Client",
      },
    });

    await request.post(`${BASE_URL}/api/transactions`, {
      data: {
        portfolioId: "PORT1003",
        investmentId: "AAPL",
        type: "BUY",
        quantity: 50,
        price: 150,
        amount: 7500,
      },
    });

    const response = await request.get(`${BASE_URL}/api/portfolios/PORT1003`);
    expect(response.ok()).toBeTruthy();
    const portfolio = await response.json();
    expect(portfolio.id).toBe("PORT1003");
    expect(portfolio.positions.length).toBeGreaterThanOrEqual(1);
    expect(portfolio.positions[0].investmentId).toBe("AAPL");
  });

  test("submit BUY transaction and verify position", async ({ request }) => {
    await request.post(`${BASE_URL}/api/portfolios`, {
      data: {
        id: "PORT1004",
        accountNo: "9000000004",
        clientName: "Transaction Test Client",
      },
    });

    const txnResponse = await request.post(`${BASE_URL}/api/transactions`, {
      data: {
        portfolioId: "PORT1004",
        investmentId: "MSFT",
        type: "BUY",
        quantity: 25,
        price: 400,
        amount: 10000,
      },
    });

    expect(txnResponse.status()).toBe(201);
    const txnBody = await txnResponse.json();
    expect(txnBody.status).toBe("DONE");
    expect(txnBody.transactionId).toBeDefined();

    // Verify position
    const posResponse = await request.get(
      `${BASE_URL}/api/positions?portfolioId=PORT1004`,
    );
    expect(posResponse.ok()).toBeTruthy();
    const positions = await posResponse.json();
    expect(positions).toHaveLength(1);
    expect(positions[0].investmentId).toBe("MSFT");
    expect(Number(positions[0].quantity)).toBe(25);
  });

  test("submit SELL transaction rejects insufficient units", async ({
    request,
  }) => {
    await request.post(`${BASE_URL}/api/portfolios`, {
      data: {
        id: "PORT1005",
        accountNo: "9000000005",
        clientName: "Sell Fail Client",
      },
    });

    // Buy 10 shares
    await request.post(`${BASE_URL}/api/transactions`, {
      data: {
        portfolioId: "PORT1005",
        investmentId: "TSLA",
        type: "BUY",
        quantity: 10,
        price: 250,
        amount: 2500,
      },
    });

    // Try to sell 50 — should fail
    const sellResponse = await request.post(`${BASE_URL}/api/transactions`, {
      data: {
        portfolioId: "PORT1005",
        investmentId: "TSLA",
        type: "SELL",
        quantity: 50,
        price: 250,
        amount: 12500,
      },
    });

    expect(sellResponse.status()).toBe(400);
    const body = await sellResponse.json();
    expect(body.error).toContain("Insufficient units");
  });

  test("reject transaction with invalid inputs", async ({ request }) => {
    const response = await request.post(`${BASE_URL}/api/transactions`, {
      data: {
        portfolioId: "PORT1005",
        investmentId: "AAPL",
        type: "BUY",
        quantity: -1,
        price: 150,
        amount: 1500,
      },
    });

    expect(response.status()).toBe(400);
  });

  test("view transaction history for a portfolio", async ({ request }) => {
    // Create and submit a transaction first
    await request.post(`${BASE_URL}/api/portfolios`, {
      data: {
        id: "PORT1006",
        accountNo: "9000000006",
        clientName: "History Client",
      },
    });

    await request.post(`${BASE_URL}/api/transactions`, {
      data: {
        portfolioId: "PORT1006",
        investmentId: "AMZN",
        type: "BUY",
        quantity: 20,
        price: 180,
        amount: 3600,
      },
    });

    const response = await request.get(
      `${BASE_URL}/api/transactions?portfolioId=PORT1006`,
    );
    expect(response.ok()).toBeTruthy();
    const transactions = await response.json();
    expect(transactions.length).toBeGreaterThanOrEqual(1);
    expect(transactions[0].type).toBe("BUY");
  });
});
