/**
 * Mock data seeded from the CLBS sample records in
 * `documentation/operations/test-data-specs.md`.
 *
 * The spec provides three portfolios (PORT00001 GROWTH PORTFOLIO,
 * PORT00002 INCOME PORTFOLIO, PORT00003 BALANCED PORTFOLIO) with total values
 * and statuses, plus sample buy/sell transactions against securities
 * (IBM, MSFT, AAPL). Those values are reproduced here 1:1 and expanded with a
 * few derived positions/transactions so the paginated history view (page size
 * 10) is exercisable end-to-end without the mainframe.
 *
 * Numeric values already have their COBOL implied-decimal scale applied
 * (see src/types/portfolio.ts).
 */
import type { Portfolio, Position, Transaction } from '../types/portfolio';

/**
 * Portfolio master records, keyed by account number.
 *
 * The BMS/COBOL online path keys inquiries on PORT-ACCOUNT-NO (INQCOM-ACCOUNT-NO
 * PIC X(10)). The test-data spec identifies portfolios by PORT-ID (PORT0000n),
 * so each seeded account number is derived as ACCT0000n and paired with its
 * portfolio id.
 */
export const portfolios: Portfolio[] = [
  {
    portfolioId: 'PORT0001',
    accountNo: 'ACCT000001',
    clientName: 'GROWTH PORTFOLIO',
    clientType: 'I',
    createDate: '20240320',
    lastMaintDate: '20240320',
    status: 'A',
    totalValue: 12345678.99,
    cashBalance: 250000.0,
  },
  {
    portfolioId: 'PORT0002',
    accountNo: 'ACCT000002',
    clientName: 'INCOME PORTFOLIO',
    clientType: 'C',
    createDate: '20240320',
    lastMaintDate: '20240320',
    status: 'A',
    totalValue: 98765432.1,
    cashBalance: 1000000.0,
  },
  {
    portfolioId: 'PORT0003',
    accountNo: 'ACCT000003',
    clientName: 'BALANCED PORTFOLIO',
    clientType: 'T',
    createDate: '20240320',
    lastMaintDate: '20240320',
    status: 'S',
    totalValue: 5555555.55,
    cashBalance: 75000.0,
  },
];

/** Positions keyed by account number (mirrors POSFILE reads in INQPORT). */
export const positionsByAccount: Record<string, Position[]> = {
  ACCT000001: [
    {
      portfolioId: 'PORT0001',
      date: '20240320',
      investmentId: 'IBM0000001',
      fundName: 'IBM COMMON STOCK',
      quantity: 5000.0,
      costBasis: 625000.0,
      marketValue: 712500.0,
      currency: 'USD',
      status: 'A',
    },
    {
      portfolioId: 'PORT0001',
      date: '20240320',
      investmentId: 'GRWFND0001',
      fundName: 'GROWTH EQUITY FUND',
      quantity: 12000.5,
      costBasis: 1080000.0,
      marketValue: 1245600.75,
      currency: 'USD',
      status: 'A',
    },
  ],
  ACCT000002: [
    {
      portfolioId: 'PORT0002',
      date: '20240320',
      investmentId: 'MSFT000001',
      fundName: 'MICROSOFT CORP',
      quantity: 15000.0,
      costBasis: 5000000.0,
      marketValue: 6187500.0,
      currency: 'USD',
      status: 'A',
    },
    {
      portfolioId: 'PORT0002',
      date: '20240320',
      investmentId: 'INCFND0001',
      fundName: 'FIXED INCOME BOND FUND',
      quantity: 40000.0,
      costBasis: 4000000.0,
      marketValue: 3960000.0,
      currency: 'USD',
      status: 'A',
    },
  ],
  ACCT000003: [
    {
      portfolioId: 'PORT0003',
      date: '20240320',
      investmentId: 'AAPL000001',
      fundName: 'APPLE INC',
      quantity: 3500.25,
      costBasis: 525000.0,
      marketValue: 612543.75,
      currency: 'USD',
      status: 'P',
    },
  ],
};

/**
 * Transaction history keyed by account number (mirrors the DB2 POSHIST rows
 * returned by INQHIST, ordered by TRANS_DATE DESC). ACCT000001 is seeded with
 * >10 rows so the page-size-10 pagination is exercised.
 */
export const transactionsByAccount: Record<string, Transaction[]> = {
  ACCT000001: [
    { transDate: '2024-03-20', transType: 'BU', transUnits: 500.0, transPrice: 125.0, transAmount: 62500.0 },
    { transDate: '2024-03-19', transType: 'BU', transUnits: 250.0, transPrice: 124.5, transAmount: 31125.0 },
    { transDate: '2024-03-18', transType: 'SL', transUnits: 100.0, transPrice: 126.0, transAmount: 12600.0 },
    { transDate: '2024-03-15', transType: 'BU', transUnits: 1000.0, transPrice: 122.75, transAmount: 122750.0 },
    { transDate: '2024-03-14', transType: 'FE', transUnits: 0.0, transPrice: 0.0, transAmount: 49.99 },
    { transDate: '2024-03-13', transType: 'BU', transUnits: 300.0, transPrice: 121.0, transAmount: 36300.0 },
    { transDate: '2024-03-12', transType: 'SL', transUnits: 150.0, transPrice: 123.25, transAmount: 18487.5 },
    { transDate: '2024-03-11', transType: 'BU', transUnits: 800.0, transPrice: 120.5, transAmount: 96400.0 },
    { transDate: '2024-03-08', transType: 'TR', transUnits: 200.0, transPrice: 119.0, transAmount: 23800.0 },
    { transDate: '2024-03-07', transType: 'BU', transUnits: 450.0, transPrice: 118.75, transAmount: 53437.5 },
    { transDate: '2024-03-06', transType: 'SL', transUnits: 75.0, transPrice: 120.0, transAmount: 9000.0 },
    { transDate: '2024-03-05', transType: 'BU', transUnits: 600.0, transPrice: 117.5, transAmount: 70500.0 },
    { transDate: '2024-03-04', transType: 'FE', transUnits: 0.0, transPrice: 0.0, transAmount: 49.99 },
  ],
  ACCT000002: [
    { transDate: '2024-03-20', transType: 'SL', transUnits: 200.0, transPrice: 412.5, transAmount: 82500.0 },
    { transDate: '2024-03-19', transType: 'BU', transUnits: 500.0, transPrice: 410.0, transAmount: 205000.0 },
    { transDate: '2024-03-18', transType: 'BU', transUnits: 1000.0, transPrice: 408.25, transAmount: 408250.0 },
  ],
  ACCT000003: [
    { transDate: '2024-03-20', transType: 'BU', transUnits: 300.0, transPrice: 175.0, transAmount: 52500.0 },
    { transDate: '2024-03-18', transType: 'BU', transUnits: 250.25, transPrice: 174.5, transAmount: 43668.63 },
  ],
};
