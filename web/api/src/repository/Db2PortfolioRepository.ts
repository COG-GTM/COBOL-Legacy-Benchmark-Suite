import {
  InvestmentPosition,
  PortfolioMaster,
  TransactionRecord,
} from "../types";
import { PortfolioRepository } from "./PortfolioRepository";

/**
 * DB2-backed implementation stub.
 *
 * This is intentionally NOT wired to a live DB2 instance (the benchmark suite
 * ships no z/OS runtime). It documents exactly where and how to connect the API
 * to the real DB2 tables defined in `src/database/db2/db2-definitions.sql`.
 *
 * To make it live:
 *   1. `npm install ibm_db` in web/api (IBM's official DB2 driver).
 *   2. Import and open a connection using the DB2_* settings from `.env`
 *      (see the commented `connect()` sketch below).
 *   3. Replace each `notImplemented()` call with the SQL shown in the comments.
 *      The SQL mirrors the queries the COBOL online programs issue.
 *   4. Set DATA_SOURCE=db2 in `.env`.
 *
 * The account-number lookups assume PORTFOLIO_MASTER has been extended with an
 * ACCOUNT_NO column (or a portfolio/account cross-reference table exists), since
 * the online inquiries are keyed by account while the base schema is keyed by
 * PORTFOLIO_ID. See web/README.md ("Swapping in real DB2").
 */
export interface Db2Config {
  database: string;
  hostname: string;
  port: string;
  uid: string;
  pwd: string;
}

export class Db2PortfolioRepository implements PortfolioRepository {
  constructor(private readonly config: Db2Config) {}

  // Example connection sketch (requires the `ibm_db` package):
  //
  // private async connect() {
  //   const ibmdb = require("ibm_db");
  //   const { database, hostname, port, uid, pwd } = this.config;
  //   const dsn =
  //     `DATABASE=${database};HOSTNAME=${hostname};PORT=${port};` +
  //     `PROTOCOL=TCPIP;UID=${uid};PWD=${pwd};`;
  //   return ibmdb.open(dsn);
  // }

  async findPortfolioByAccountNo(
    _accountNo: string
  ): Promise<PortfolioMaster | null> {
    // SELECT * FROM PORTFOLIO_MASTER WHERE ACCOUNT_NO = ?
    return this.notImplemented();
  }

  async findPortfolioById(
    _portfolioId: string
  ): Promise<PortfolioMaster | null> {
    // SELECT * FROM PORTFOLIO_MASTER WHERE PORTFOLIO_ID = ?
    return this.notImplemented();
  }

  async findCurrentPositionByPortfolioId(
    _portfolioId: string
  ): Promise<InvestmentPosition | null> {
    // SELECT * FROM INVESTMENT_POSITIONS
    //  WHERE PORTFOLIO_ID = ?
    //  ORDER BY POSITION_DATE DESC FETCH FIRST 1 ROW ONLY
    // (mirrors the single READ in INQPORT)
    return this.notImplemented();
  }

  async findHistoryByPortfolioId(
    _portfolioId: string
  ): Promise<TransactionRecord[]> {
    // SELECT TRANSACTION_DATE, TRANSACTION_TYPE, QUANTITY, PRICE, AMOUNT, ...
    //   FROM TRANSACTION_HISTORY
    //  WHERE PORTFOLIO_ID = ?
    //  ORDER BY TRANSACTION_DATE DESC
    // (mirrors the HISTORY_CURSOR SELECT in INQHIST)
    return this.notImplemented();
  }

  private notImplemented(): never {
    throw new Error(
      "Db2PortfolioRepository is a stub. Install `ibm_db`, implement the " +
        "queries in web/api/src/repository/Db2PortfolioRepository.ts, and set " +
        "DATA_SOURCE=db2. See web/README.md."
    );
  }
}
