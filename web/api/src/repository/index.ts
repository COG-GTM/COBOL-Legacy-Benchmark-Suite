import { Db2PortfolioRepository } from "./Db2PortfolioRepository";
import { InMemoryPortfolioRepository } from "./InMemoryPortfolioRepository";
import { PortfolioRepository } from "./PortfolioRepository";

/**
 * Selects the data layer based on the DATA_SOURCE env var.
 * - "memory" (default): seeded in-memory store; runs standalone.
 * - "db2": DB2 driver stub (see Db2PortfolioRepository for wiring).
 */
export function createRepository(): PortfolioRepository {
  const source = (process.env.DATA_SOURCE ?? "memory").toLowerCase();

  if (source === "db2") {
    return new Db2PortfolioRepository({
      database: process.env.DB2_DATABASE ?? "PORTDB",
      hostname: process.env.DB2_HOSTNAME ?? "localhost",
      port: process.env.DB2_PORT ?? "50000",
      uid: process.env.DB2_UID ?? "db2inst1",
      pwd: process.env.DB2_PWD ?? "",
    });
  }

  return new InMemoryPortfolioRepository();
}

export { PortfolioRepository } from "./PortfolioRepository";
