import type { SummaryMetrics, Transaction, SystemStatus } from "../types";

export const summaryMetrics: SummaryMetrics = {
  totalAccounts: 1247,
  totalPositions: 8432,
  todayTransactions: 156,
  systemStatus: "Operational",
};

export const recentTransactions: Transaction[] = [
  {
    date: "2024-01-15",
    account: "0000012345",
    type: "BUY",
    fund: "GRWTH1",
    units: 100,
    price: 25.5,
    amount: 2550.0,
  },
  {
    date: "2024-01-15",
    account: "0000067890",
    type: "SELL",
    fund: "BOND02",
    units: 50,
    price: 102.75,
    amount: 5137.5,
  },
  {
    date: "2024-01-14",
    account: "0000054321",
    type: "XFER",
    fund: "IDX500",
    units: 200,
    price: 45.0,
    amount: 9000.0,
  },
  {
    date: "2024-01-14",
    account: "0000012345",
    type: "FEE",
    fund: "GRWTH1",
    units: 0,
    price: 0,
    amount: 12.5,
  },
  {
    date: "2024-01-13",
    account: "0000098765",
    type: "BUY",
    fund: "TECH03",
    units: 75,
    price: 88.25,
    amount: 6618.75,
  },
];

export const systemStatus: SystemStatus = {
  status: "operational",
  lastBatchRun: "2024-01-15 02:00:00",
};
