import type { Portfolio, Position, Transaction, MarketTick, PerformancePoint, BatchJob } from '../types';

export const portfolios: Portfolio[] = [
  {
    portfolioId: 'PORT0001',
    accountNumber: '1000000001',
    clientName: 'Meridian Capital Group',
    clientType: 'Corporate',
    createDate: '2022-01-15',
    lastMaintDate: '2024-11-20',
    status: 'Active',
    totalValue: 2847563.42,
    cashBalance: 125430.18,
    riskLevel: 'High',
    currency: 'USD',
  },
  {
    portfolioId: 'PORT0002',
    accountNumber: '1000000002',
    clientName: 'Sarah J. Mitchell',
    clientType: 'Individual',
    createDate: '2023-03-08',
    lastMaintDate: '2024-11-19',
    status: 'Active',
    totalValue: 458920.75,
    cashBalance: 32100.00,
    riskLevel: 'Medium',
    currency: 'USD',
  },
  {
    portfolioId: 'PORT0003',
    accountNumber: '1000000003',
    clientName: 'Wellington Family Trust',
    clientType: 'Trust',
    createDate: '2021-06-22',
    lastMaintDate: '2024-11-18',
    status: 'Active',
    totalValue: 5234100.90,
    cashBalance: 450000.00,
    riskLevel: 'Low',
    currency: 'USD',
  },
  {
    portfolioId: 'PORT0004',
    accountNumber: '1000000004',
    clientName: 'David R. Chen',
    clientType: 'Individual',
    createDate: '2024-01-10',
    lastMaintDate: '2024-11-15',
    status: 'Active',
    totalValue: 187450.33,
    cashBalance: 15200.00,
    riskLevel: 'High',
    currency: 'USD',
  },
  {
    portfolioId: 'PORT0005',
    accountNumber: '1000000005',
    clientName: 'Apex Industries LLC',
    clientType: 'Corporate',
    createDate: '2020-09-01',
    lastMaintDate: '2024-10-30',
    status: 'Suspended',
    totalValue: 892340.60,
    cashBalance: 0,
    riskLevel: 'Medium',
    currency: 'USD',
  },
];

export const positions: Position[] = [
  { portfolioId: 'PORT0001', investmentId: 'INV-AAPL01', symbol: 'AAPL', name: 'Apple Inc.', quantity: 1500, costBasis: 234750.00, marketValue: 352500.00, currentPrice: 235.00, previousPrice: 233.80, currency: 'USD', status: 'Active', lastUpdated: '2024-11-20T15:30:00Z' },
  { portfolioId: 'PORT0001', investmentId: 'INV-MSFT01', symbol: 'MSFT', name: 'Microsoft Corp.', quantity: 800, costBasis: 248000.00, marketValue: 340000.00, currentPrice: 425.00, previousPrice: 423.50, currency: 'USD', status: 'Active', lastUpdated: '2024-11-20T15:30:00Z' },
  { portfolioId: 'PORT0001', investmentId: 'INV-GOOGL1', symbol: 'GOOGL', name: 'Alphabet Inc.', quantity: 600, costBasis: 96000.00, marketValue: 105000.00, currentPrice: 175.00, previousPrice: 174.20, currency: 'USD', status: 'Active', lastUpdated: '2024-11-20T15:30:00Z' },
  { portfolioId: 'PORT0001', investmentId: 'INV-AMZN01', symbol: 'AMZN', name: 'Amazon.com Inc.', quantity: 1200, costBasis: 187200.00, marketValue: 237600.00, currentPrice: 198.00, previousPrice: 196.50, currency: 'USD', status: 'Active', lastUpdated: '2024-11-20T15:30:00Z' },
  { portfolioId: 'PORT0001', investmentId: 'INV-NVDA01', symbol: 'NVDA', name: 'NVIDIA Corp.', quantity: 2000, costBasis: 580000.00, marketValue: 1080000.00, currentPrice: 540.00, previousPrice: 535.00, currency: 'USD', status: 'Active', lastUpdated: '2024-11-20T15:30:00Z' },
  { portfolioId: 'PORT0001', investmentId: 'INV-TSLA01', symbol: 'TSLA', name: 'Tesla Inc.', quantity: 400, costBasis: 96000.00, marketValue: 106000.00, currentPrice: 265.00, previousPrice: 262.00, currency: 'USD', status: 'Active', lastUpdated: '2024-11-20T15:30:00Z' },
  { portfolioId: 'PORT0002', investmentId: 'INV-VTI001', symbol: 'VTI', name: 'Vanguard Total Stock', quantity: 500, costBasis: 105000.00, marketValue: 132500.00, currentPrice: 265.00, previousPrice: 264.20, currency: 'USD', status: 'Active', lastUpdated: '2024-11-20T15:30:00Z' },
  { portfolioId: 'PORT0002', investmentId: 'INV-BND001', symbol: 'BND', name: 'Vanguard Total Bond', quantity: 1200, costBasis: 92400.00, marketValue: 90000.00, currentPrice: 75.00, previousPrice: 75.10, currency: 'USD', status: 'Active', lastUpdated: '2024-11-20T15:30:00Z' },
  { portfolioId: 'PORT0002', investmentId: 'INV-QQQ001', symbol: 'QQQ', name: 'Invesco QQQ Trust', quantity: 300, costBasis: 114000.00, marketValue: 148500.00, currentPrice: 495.00, previousPrice: 493.50, currency: 'USD', status: 'Active', lastUpdated: '2024-11-20T15:30:00Z' },
  { portfolioId: 'PORT0003', investmentId: 'INV-SPY001', symbol: 'SPY', name: 'SPDR S&P 500 ETF', quantity: 3000, costBasis: 1350000.00, marketValue: 1770000.00, currentPrice: 590.00, previousPrice: 588.50, currency: 'USD', status: 'Active', lastUpdated: '2024-11-20T15:30:00Z' },
  { portfolioId: 'PORT0003', investmentId: 'INV-AGG001', symbol: 'AGG', name: 'iShares Core US Agg', quantity: 5000, costBasis: 525000.00, marketValue: 500000.00, currentPrice: 100.00, previousPrice: 100.05, currency: 'USD', status: 'Active', lastUpdated: '2024-11-20T15:30:00Z' },
  { portfolioId: 'PORT0003', investmentId: 'INV-GLD001', symbol: 'GLD', name: 'SPDR Gold Shares', quantity: 2000, costBasis: 360000.00, marketValue: 464000.00, currentPrice: 232.00, previousPrice: 231.00, currency: 'USD', status: 'Active', lastUpdated: '2024-11-20T15:30:00Z' },
  { portfolioId: 'PORT0004', investmentId: 'INV-COIN01', symbol: 'COIN', name: 'Coinbase Global', quantity: 200, costBasis: 42000.00, marketValue: 58000.00, currentPrice: 290.00, previousPrice: 285.00, currency: 'USD', status: 'Active', lastUpdated: '2024-11-20T15:30:00Z' },
  { portfolioId: 'PORT0004', investmentId: 'INV-SQ0001', symbol: 'SQ', name: 'Block Inc.', quantity: 500, costBasis: 37500.00, marketValue: 42500.00, currentPrice: 85.00, previousPrice: 83.50, currency: 'USD', status: 'Active', lastUpdated: '2024-11-20T15:30:00Z' },
  { portfolioId: 'PORT0004', investmentId: 'INV-ARKK01', symbol: 'ARKK', name: 'ARK Innovation ETF', quantity: 800, costBasis: 40000.00, marketValue: 38400.00, currentPrice: 48.00, previousPrice: 47.20, currency: 'USD', status: 'Active', lastUpdated: '2024-11-20T15:30:00Z' },
];

export const transactions: Transaction[] = [
  { transactionId: '20241120153001000001', date: '2024-11-20', time: '15:30:01', portfolioId: 'PORT0001', sequenceNo: '000001', investmentId: 'INV-NVDA01', symbol: 'NVDA', type: 'BUY', quantity: 100, price: 540.00, amount: 54000.00, fees: 9.99, currency: 'USD', status: 'Done', processUser: 'TRDBOT01' },
  { transactionId: '20241120142500000002', date: '2024-11-20', time: '14:25:00', portfolioId: 'PORT0001', sequenceNo: '000002', investmentId: 'INV-TSLA01', symbol: 'TSLA', type: 'SELL', quantity: 50, price: 265.00, amount: 13250.00, fees: 9.99, currency: 'USD', status: 'Done', processUser: 'TRDBOT01' },
  { transactionId: '20241119100000000003', date: '2024-11-19', time: '10:00:00', portfolioId: 'PORT0002', sequenceNo: '000003', investmentId: 'INV-VTI001', symbol: 'VTI', type: 'BUY', quantity: 50, price: 264.20, amount: 13210.00, fees: 0, currency: 'USD', status: 'Done', processUser: 'SMITCH01' },
  { transactionId: '20241119093000000004', date: '2024-11-19', time: '09:30:00', portfolioId: 'PORT0003', sequenceNo: '000004', investmentId: 'INV-GLD001', symbol: 'GLD', type: 'BUY', quantity: 200, price: 231.00, amount: 46200.00, fees: 14.99, currency: 'USD', status: 'Done', processUser: 'WELFAM01' },
  { transactionId: '20241118160000000005', date: '2024-11-18', time: '16:00:00', portfolioId: 'PORT0001', sequenceNo: '000005', investmentId: 'INV-AAPL01', symbol: 'AAPL', type: 'BUY', quantity: 200, price: 233.80, amount: 46760.00, fees: 9.99, currency: 'USD', status: 'Done', processUser: 'TRDBOT01' },
  { transactionId: '20241118113000000006', date: '2024-11-18', time: '11:30:00', portfolioId: 'PORT0004', sequenceNo: '000006', investmentId: 'INV-COIN01', symbol: 'COIN', type: 'BUY', quantity: 50, price: 285.00, amount: 14250.00, fees: 9.99, currency: 'USD', status: 'Done', processUser: 'DCHEN001' },
  { transactionId: '20241117140000000007', date: '2024-11-17', time: '14:00:00', portfolioId: 'PORT0001', sequenceNo: '000007', investmentId: 'INV-GOOGL1', symbol: 'GOOGL', type: 'SELL', quantity: 100, price: 174.20, amount: 17420.00, fees: 9.99, currency: 'USD', status: 'Done', processUser: 'TRDBOT01' },
  { transactionId: '20241117120000000008', date: '2024-11-17', time: '12:00:00', portfolioId: 'PORT0003', sequenceNo: '000008', investmentId: 'INV-SPY001', symbol: 'SPY', type: 'TRANSFER', quantity: 500, price: 588.50, amount: 294250.00, fees: 0, currency: 'USD', status: 'Done', processUser: 'WELFAM01' },
  { transactionId: '20241116100000000009', date: '2024-11-16', time: '10:00:00', portfolioId: 'PORT0002', sequenceNo: '000009', investmentId: 'INV-QQQ001', symbol: 'QQQ', type: 'BUY', quantity: 30, price: 493.50, amount: 14805.00, fees: 0, currency: 'USD', status: 'Done', processUser: 'SMITCH01' },
  { transactionId: '20241115093000000010', date: '2024-11-15', time: '09:30:00', portfolioId: 'PORT0001', sequenceNo: '000010', investmentId: 'INV-MSFT01', symbol: 'MSFT', type: 'BUY', quantity: 100, price: 423.50, amount: 42350.00, fees: 9.99, currency: 'USD', status: 'Done', processUser: 'TRDBOT01' },
  { transactionId: '20241115110000000011', date: '2024-11-15', time: '11:00:00', portfolioId: 'PORT0004', sequenceNo: '000011', investmentId: 'INV-SQ0001', symbol: 'SQ', type: 'BUY', quantity: 100, price: 83.50, amount: 8350.00, fees: 9.99, currency: 'USD', status: 'Pending', processUser: 'DCHEN001' },
  { transactionId: '20241114160000000012', date: '2024-11-14', time: '16:00:00', portfolioId: 'PORT0001', sequenceNo: '000012', investmentId: 'INV-AMZN01', symbol: 'AMZN', type: 'FEE', quantity: 0, price: 0, amount: 24.99, fees: 24.99, currency: 'USD', status: 'Done', processUser: 'SYSTEM01' },
  { transactionId: '20241113150000000013', date: '2024-11-13', time: '15:00:00', portfolioId: 'PORT0005', sequenceNo: '000013', investmentId: 'INV-AAPL01', symbol: 'AAPL', type: 'SELL', quantity: 500, price: 230.00, amount: 115000.00, fees: 14.99, currency: 'USD', status: 'Failed', processUser: 'SYSTEM01' },
  { transactionId: '20241112093000000014', date: '2024-11-12', time: '09:30:00', portfolioId: 'PORT0003', sequenceNo: '000014', investmentId: 'INV-AGG001', symbol: 'AGG', type: 'BUY', quantity: 500, price: 100.05, amount: 50025.00, fees: 0, currency: 'USD', status: 'Done', processUser: 'WELFAM01' },
  { transactionId: '20241111140000000015', date: '2024-11-11', time: '14:00:00', portfolioId: 'PORT0004', sequenceNo: '000015', investmentId: 'INV-ARKK01', symbol: 'ARKK', type: 'BUY', quantity: 300, price: 47.20, amount: 14160.00, fees: 9.99, currency: 'USD', status: 'Reversed', processUser: 'DCHEN001' },
];

export const initialMarketTicks: MarketTick[] = [
  { symbol: 'AAPL', price: 235.00, change: 1.20, changePercent: 0.51 },
  { symbol: 'MSFT', price: 425.00, change: 1.50, changePercent: 0.35 },
  { symbol: 'GOOGL', price: 175.00, change: 0.80, changePercent: 0.46 },
  { symbol: 'AMZN', price: 198.00, change: 1.50, changePercent: 0.76 },
  { symbol: 'NVDA', price: 540.00, change: 5.00, changePercent: 0.93 },
  { symbol: 'TSLA', price: 265.00, change: 3.00, changePercent: 1.15 },
  { symbol: 'SPY', price: 590.00, change: 1.50, changePercent: 0.25 },
  { symbol: 'QQQ', price: 495.00, change: 1.50, changePercent: 0.30 },
  { symbol: 'GLD', price: 232.00, change: 1.00, changePercent: 0.43 },
  { symbol: 'VTI', price: 265.00, change: 0.80, changePercent: 0.30 },
];

export function generatePerformanceData(): PerformancePoint[] {
  const points: PerformancePoint[] = [];
  let value = 7500000;
  let benchmark = 7500000;
  const now = new Date();

  for (let i = 365; i >= 0; i--) {
    const date = new Date(now);
    date.setDate(date.getDate() - i);

    const dailyReturn = (Math.random() - 0.48) * 0.015;
    const benchmarkReturn = (Math.random() - 0.48) * 0.012;

    value *= (1 + dailyReturn);
    benchmark *= (1 + benchmarkReturn);

    points.push({
      date: date.toISOString().split('T')[0],
      value: Math.round(value * 100) / 100,
      benchmark: Math.round(benchmark * 100) / 100,
    });
  }

  return points;
}

export const batchJobs: BatchJob[] = [
  { jobId: 'TRNVAL-20241120', programName: 'TRNVAL00', description: 'Transaction Validation', status: 'Completed', startTime: '2024-11-20T02:00:00Z', endTime: '2024-11-20T02:15:32Z', recordsProcessed: 15420, totalRecords: 15420, returnCode: 0 },
  { jobId: 'POSUPD-20241120', programName: 'POSUPDT', description: 'Position Update', status: 'Completed', startTime: '2024-11-20T02:16:00Z', endTime: '2024-11-20T02:28:45Z', recordsProcessed: 8934, totalRecords: 8934, returnCode: 0 },
  { jobId: 'HISTLD-20241120', programName: 'HISTLD00', description: 'History Load', status: 'Running', startTime: '2024-11-20T02:30:00Z', recordsProcessed: 6210, totalRecords: 12500, returnCode: 0 },
  { jobId: 'RPTPOS-20241120', programName: 'RPTPOS00', description: 'Position Report', status: 'Scheduled', startTime: '2024-11-20T03:00:00Z', recordsProcessed: 0, totalRecords: 0, returnCode: 0 },
  { jobId: 'RPTAUD-20241120', programName: 'RPTAUD00', description: 'Audit Report', status: 'Scheduled', startTime: '2024-11-20T03:30:00Z', recordsProcessed: 0, totalRecords: 0, returnCode: 0 },
  { jobId: 'UTLMNT-20241119', programName: 'UTLMNT00', description: 'File Maintenance', status: 'Completed', startTime: '2024-11-19T01:00:00Z', endTime: '2024-11-19T01:45:00Z', recordsProcessed: 24500, totalRecords: 24500, returnCode: 0 },
  { jobId: 'TRNVAL-20241119', programName: 'TRNVAL00', description: 'Transaction Validation', status: 'Failed', startTime: '2024-11-19T02:00:00Z', endTime: '2024-11-19T02:05:12Z', recordsProcessed: 1230, totalRecords: 14800, returnCode: 12 },
  { jobId: 'TSTGEN-20241118', programName: 'TSTGEN00', description: 'Test Data Generation', status: 'Completed', startTime: '2024-11-18T22:00:00Z', endTime: '2024-11-18T22:30:00Z', recordsProcessed: 50000, totalRecords: 50000, returnCode: 0 },
];
