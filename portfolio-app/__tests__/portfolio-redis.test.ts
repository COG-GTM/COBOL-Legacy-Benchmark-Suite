/**
 * Tests for Redis helper functions — verifies VSAM status code equivalents.
 * Uses ioredis-mock to simulate Redis operations.
 */

jest.mock("ioredis", () => require("ioredis-mock"));

process.env.REDIS_URL = "redis://localhost:6379";

import {
  writePortfolio,
  readPortfolio,
  rewritePortfolio,
  deletePortfolioFromCache,
  getPortfolioIdsByStatus,
  getPortfolioByAccount,
  getRedisClient,
  type PortfolioHash,
} from "@/lib/redis";

const samplePortfolio: PortfolioHash = {
  portfolio_id: "PORT0001",
  account_no: "1234567890",
  client_name: "Test Client",
  client_type: "I",
  portfolio_name: "Test Portfolio",
  currency_code: "USD",
  risk_level: "M",
  branch_id: "01",
  total_value: "10000.00",
  cash_balance: "5000.00",
  status: "A",
  open_date: "2024-01-01",
  close_date: "",
  updated_by: "TESTER",
  created_at: "2024-01-01T00:00:00.000Z",
  updated_at: "2024-01-01T00:00:00.000Z",
};

beforeEach(async () => {
  const client = getRedisClient();
  await client.flushall();
});

describe("Redis VSAM operations", () => {
  // Mirrors WRITE PORTFOLIO-RECORD (PORTMSTR.cbl line 126)
  describe("writePortfolio", () => {
    it("returns '00' on successful write", async () => {
      const status = await writePortfolio(samplePortfolio);
      expect(status).toBe("00");
    });

    it("returns '22' on duplicate key (VSAM status 22)", async () => {
      await writePortfolio(samplePortfolio);
      const status = await writePortfolio(samplePortfolio);
      expect(status).toBe("22");
    });

    it("returns '22' when account_no is already in use", async () => {
      await writePortfolio(samplePortfolio);
      const dup = {
        ...samplePortfolio,
        portfolio_id: "PORT0002",
      };
      const status = await writePortfolio(dup);
      expect(status).toBe("22");
    });

    it("maintains status index set", async () => {
      await writePortfolio(samplePortfolio);
      const ids = await getPortfolioIdsByStatus("A");
      expect(ids).toContain("PORT0001");
    });

    it("maintains account index", async () => {
      await writePortfolio(samplePortfolio);
      const id = await getPortfolioByAccount("1234567890");
      expect(id).toBe("PORT0001");
    });
  });

  // Mirrors READ PORTFOLIO-FILE (PORTMSTR.cbl line 169)
  describe("readPortfolio", () => {
    it("returns portfolio data on success", async () => {
      await writePortfolio(samplePortfolio);
      const data = await readPortfolio("PORT0001");
      expect(data).not.toBeNull();
      expect(data!.client_name).toBe("Test Client");
      expect(data!.account_no).toBe("1234567890");
    });

    it("returns null when not found (VSAM status 23)", async () => {
      const data = await readPortfolio("PORT9999");
      expect(data).toBeNull();
    });
  });

  // Mirrors REWRITE PORT-RECORD (PORTMSTR.cbl line 194)
  describe("rewritePortfolio", () => {
    it("returns '00' on successful rewrite", async () => {
      await writePortfolio(samplePortfolio);
      const updated = { ...samplePortfolio, client_name: "Updated Client" };
      const status = await rewritePortfolio("PORT0001", updated);
      expect(status).toBe("00");

      const data = await readPortfolio("PORT0001");
      expect(data!.client_name).toBe("Updated Client");
    });

    it("returns '23' when record not found", async () => {
      const status = await rewritePortfolio("PORT9999", samplePortfolio);
      expect(status).toBe("23");
    });

    it("updates status index when status changes", async () => {
      await writePortfolio(samplePortfolio);
      const updated = { ...samplePortfolio, status: "S" };
      await rewritePortfolio("PORT0001", updated, "A");

      const activeIds = await getPortfolioIdsByStatus("A");
      const suspendedIds = await getPortfolioIdsByStatus("S");
      expect(activeIds).not.toContain("PORT0001");
      expect(suspendedIds).toContain("PORT0001");
    });
  });

  // Mirrors DELETE PORTFOLIO-FILE (PORTMSTR.cbl line 215)
  describe("deletePortfolioFromCache", () => {
    it("returns '00' on successful delete", async () => {
      await writePortfolio(samplePortfolio);
      const status = await deletePortfolioFromCache("PORT0001");
      expect(status).toBe("00");

      const data = await readPortfolio("PORT0001");
      expect(data).toBeNull();
    });

    it("returns '23' when record not found", async () => {
      const status = await deletePortfolioFromCache("PORT9999");
      expect(status).toBe("23");
    });

    it("cleans up status index on delete", async () => {
      await writePortfolio(samplePortfolio);
      await deletePortfolioFromCache("PORT0001");

      const ids = await getPortfolioIdsByStatus("A");
      expect(ids).not.toContain("PORT0001");
    });

    it("cleans up account index on delete", async () => {
      await writePortfolio(samplePortfolio);
      await deletePortfolioFromCache("PORT0001");

      const id = await getPortfolioByAccount("1234567890");
      expect(id).toBeNull();
    });
  });
});
