import {
  InvestmentPosition,
  PortfolioMaster,
  TransactionRecord,
} from "../types";

/**
 * Representative sample data matching the DB2 schema
 * (`src/database/db2/db2-definitions.sql`). This lets the API run standalone
 * with no live DB2/z-OS runtime. See web/README.md for pointing at real DB2.
 *
 * Reference lookup for investment (fund) names. The DB2 schema stores only
 * INVESTMENT_ID; the BMS POSMAP screen also shows a "Fund Name", so we keep a
 * small reference table here (in a real system this would be a securities /
 * instrument master table).
 */
export const INVESTMENT_NAMES: Record<string, string> = {
  FUND000123: "Global Equity Growth Fund",
  FUND000456: "US Core Bond Fund",
  FUND000789: "Emerging Markets Fund",
  FUND000999: "Money Market Fund",
  FUND000321: "Technology Sector Fund",
};

export const PORTFOLIO_MASTER: PortfolioMaster[] = [
  {
    portfolioId: "PORT0001",
    accountNo: "1000000001",
    accountType: "IN",
    branchId: "01",
    clientId: "CLNT000100",
    portfolioName: "Anderson Retirement Portfolio",
    currencyCode: "USD",
    riskLevel: "3",
    status: "A",
    openDate: "2019-05-14",
    closeDate: null,
    lastMaintDate: "2024-11-01 09:15:22.000000",
    lastMaintUser: "BATCH01",
  },
  {
    portfolioId: "PORT0002",
    accountNo: "1000000002",
    accountType: "CO",
    branchId: "02",
    clientId: "CLNT000200",
    portfolioName: "Beacon Capital Corporate Fund",
    currencyCode: "USD",
    riskLevel: "4",
    status: "A",
    openDate: "2021-01-08",
    closeDate: null,
    lastMaintDate: "2024-11-01 09:15:22.000000",
    lastMaintUser: "BATCH01",
  },
  {
    portfolioId: "PORT0003",
    accountNo: "1000000003",
    accountType: "TR",
    branchId: "01",
    clientId: "CLNT000300",
    portfolioName: "Carter Family Trust",
    currencyCode: "USD",
    riskLevel: "2",
    status: "S",
    openDate: "2018-09-30",
    closeDate: null,
    lastMaintDate: "2024-10-15 14:02:10.000000",
    lastMaintUser: "OPER005",
  },
];

/**
 * INVESTMENT_POSITIONS rows. The inquiry (INQPORT/POSMAP) shows the current
 * position per portfolio; we keep the latest POSITION_DATE per portfolio here.
 */
export const INVESTMENT_POSITIONS: InvestmentPosition[] = [
  {
    portfolioId: "PORT0001",
    investmentId: "FUND000123",
    investmentName: INVESTMENT_NAMES.FUND000123,
    positionDate: "2024-10-31",
    quantity: 1250.5,
    costBasis: 118000.0,
    marketValue: 142375.75,
    currencyCode: "USD",
  },
  {
    portfolioId: "PORT0002",
    investmentId: "FUND000456",
    investmentName: INVESTMENT_NAMES.FUND000456,
    positionDate: "2024-10-31",
    quantity: 8000.0,
    costBasis: 800000.0,
    marketValue: 812640.0,
    currencyCode: "USD",
  },
  {
    portfolioId: "PORT0003",
    investmentId: "FUND000789",
    investmentName: INVESTMENT_NAMES.FUND000789,
    positionDate: "2024-10-31",
    quantity: 430.25,
    costBasis: 51000.0,
    marketValue: 48912.3,
    currencyCode: "USD",
  },
];

/**
 * TRANSACTION_HISTORY rows. Mirrors INQHIST which selects transactions for an
 * account ordered by date descending.
 */
export const TRANSACTION_HISTORY: TransactionRecord[] = [
  {
    transactionId: "20241031091500000001",
    portfolioId: "PORT0001",
    transactionDate: "2024-10-31",
    transactionTime: "09:15:00",
    investmentId: "FUND000123",
    transactionType: "BU",
    quantity: 100.0,
    price: 113.5,
    amount: 11350.0,
    currencyCode: "USD",
    status: "P",
  },
  {
    transactionId: "20240915103000000002",
    portfolioId: "PORT0001",
    transactionDate: "2024-09-15",
    transactionTime: "10:30:00",
    investmentId: "FUND000123",
    transactionType: "BU",
    quantity: 250.5,
    price: 109.75,
    amount: 27492.38,
    currencyCode: "USD",
    status: "P",
  },
  {
    transactionId: "20240720140500000003",
    portfolioId: "PORT0001",
    transactionDate: "2024-07-20",
    transactionTime: "14:05:00",
    investmentId: "FUND000123",
    transactionType: "SL",
    quantity: 50.0,
    price: 111.2,
    amount: 5560.0,
    currencyCode: "USD",
    status: "P",
  },
  {
    transactionId: "20240630235959000004",
    portfolioId: "PORT0001",
    transactionDate: "2024-06-30",
    transactionTime: "23:59:59",
    investmentId: "FUND000123",
    transactionType: "FE",
    quantity: 0.0,
    price: 0.0,
    amount: 42.15,
    currencyCode: "USD",
    status: "P",
  },
  {
    transactionId: "20241031093000000005",
    portfolioId: "PORT0002",
    transactionDate: "2024-10-31",
    transactionTime: "09:30:00",
    investmentId: "FUND000456",
    transactionType: "BU",
    quantity: 2000.0,
    price: 100.0,
    amount: 200000.0,
    currencyCode: "USD",
    status: "P",
  },
  {
    transactionId: "20240801110000000006",
    portfolioId: "PORT0002",
    transactionDate: "2024-08-01",
    transactionTime: "11:00:00",
    investmentId: "FUND000456",
    transactionType: "BU",
    quantity: 6000.0,
    price: 100.0,
    amount: 600000.0,
    currencyCode: "USD",
    status: "P",
  },
  {
    transactionId: "20240905120000000007",
    portfolioId: "PORT0003",
    transactionDate: "2024-09-05",
    transactionTime: "12:00:00",
    investmentId: "FUND000789",
    transactionType: "BU",
    quantity: 430.25,
    price: 118.5,
    amount: 50984.63,
    currencyCode: "USD",
    status: "R",
  },
];
