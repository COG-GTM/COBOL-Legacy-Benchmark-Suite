import type { Portfolio, Position, AuditRecord, HistoryEntry, Transaction } from '../types';

// ======================================================================
// PORTFOLIOS - Maps to PORTFLIO.cpy PORT-RECORD (8 portfolios minimum)
// ======================================================================
export const mockPortfolios: Portfolio[] = [
  {
    portfolioId: 'PORT0001',
    accountNumber: '1000000001',
    clientName: 'JOHN SMITH',
    clientType: 'I',
    createDate: '20230115',
    lastMaintDate: '20240301',
    status: 'A',
    totalValue: 250000.00,
    cashBalance: 15000.00,
    lastUser: 'ADMIN01',
    lastTransDate: '20240301',
  },
  {
    portfolioId: 'PORT0002',
    accountNumber: '1000000001',
    clientName: 'JOHN SMITH',
    clientType: 'I',
    createDate: '20230220',
    lastMaintDate: '20240215',
    status: 'A',
    totalValue: 180000.00,
    cashBalance: 8500.00,
    lastUser: 'ADMIN01',
    lastTransDate: '20240215',
  },
  {
    portfolioId: 'PORT0003',
    accountNumber: '2000000002',
    clientName: 'ACME CORPORATION',
    clientType: 'C',
    createDate: '20220601',
    lastMaintDate: '20240310',
    status: 'A',
    totalValue: 1500000.00,
    cashBalance: 75000.00,
    lastUser: 'ADMIN02',
    lastTransDate: '20240310',
  },
  {
    portfolioId: 'PORT0004',
    accountNumber: '3000000003',
    clientName: 'FAMILY TRUST ALPHA',
    clientType: 'T',
    createDate: '20210301',
    lastMaintDate: '20240101',
    status: 'A',
    totalValue: 520000.00,
    cashBalance: 22000.00,
    lastUser: 'ADMIN01',
    lastTransDate: '20240101',
  },
  {
    portfolioId: 'PORT0005',
    accountNumber: '4000000004',
    clientName: 'JANE DOE',
    clientType: 'I',
    createDate: '20230801',
    lastMaintDate: '20240220',
    status: 'S',
    totalValue: 95000.00,
    cashBalance: 5000.00,
    lastUser: 'ADMIN02',
    lastTransDate: '20240220',
  },
  {
    portfolioId: 'PORT0006',
    accountNumber: '5000000005',
    clientName: 'GLOBAL VENTURES INC',
    clientType: 'C',
    createDate: '20200915',
    lastMaintDate: '20231215',
    status: 'C',
    totalValue: 0.00,
    cashBalance: 0.00,
    lastUser: 'ADMIN01',
    lastTransDate: '20231215',
  },
  {
    portfolioId: 'PORT0007',
    accountNumber: '6000000006',
    clientName: 'WILLIAMS ESTATE TRUST',
    clientType: 'T',
    createDate: '20220110',
    lastMaintDate: '20240305',
    status: 'A',
    totalValue: 850000.00,
    cashBalance: 45000.00,
    lastUser: 'ADMIN03',
    lastTransDate: '20240305',
  },
  {
    portfolioId: 'PORT0008',
    accountNumber: '7000000007',
    clientName: 'ROBERT JOHNSON',
    clientType: 'I',
    createDate: '20240101',
    lastMaintDate: '20240315',
    status: 'A',
    totalValue: 125000.00,
    cashBalance: 10000.00,
    lastUser: 'ADMIN01',
    lastTransDate: '20240315',
  },
];

// ======================================================================
// POSITIONS - Maps to POSREC.cpy POSITION-RECORD
// 5+ accounts with multiple positions each
// ======================================================================
export const mockPositions: Position[] = [
  // Account 1000000001 / PORT0001
  { portfolioId: 'PORT0001', date: '20240301', investmentId: 'AAPL', quantity: 150, costBasis: 22500.00, marketValue: 26250.00, currency: 'USD', status: 'A', lastMaintDate: '2024-03-01T10:30:00Z', lastMaintUser: 'ADMIN01', fundName: 'APPLE INC' },
  { portfolioId: 'PORT0001', date: '20240301', investmentId: 'MSFT', quantity: 100, costBasis: 35000.00, marketValue: 41200.00, currency: 'USD', status: 'A', lastMaintDate: '2024-03-01T10:30:00Z', lastMaintUser: 'ADMIN01', fundName: 'MICROSOFT CORP' },
  { portfolioId: 'PORT0001', date: '20240301', investmentId: 'VBOND1', quantity: 500, costBasis: 50000.00, marketValue: 51500.00, currency: 'USD', status: 'A', lastMaintDate: '2024-02-15T09:00:00Z', lastMaintUser: 'ADMIN01', fundName: 'VANGUARD BOND INDEX' },
  { portfolioId: 'PORT0001', date: '20240301', investmentId: 'MMFUND', quantity: 10000, costBasis: 100000.00, marketValue: 100250.00, currency: 'USD', status: 'A', lastMaintDate: '2024-03-01T10:30:00Z', lastMaintUser: 'ADMIN01', fundName: 'FIDELITY MONEY MARKET' },
  // Account 1000000001 / PORT0002
  { portfolioId: 'PORT0002', date: '20240215', investmentId: 'GOOGL', quantity: 75, costBasis: 10500.00, marketValue: 10725.00, currency: 'USD', status: 'A', lastMaintDate: '2024-02-15T14:00:00Z', lastMaintUser: 'ADMIN01', fundName: 'ALPHABET INC' },
  { portfolioId: 'PORT0002', date: '20240215', investmentId: 'SPY', quantity: 200, costBasis: 90000.00, marketValue: 95000.00, currency: 'USD', status: 'A', lastMaintDate: '2024-02-15T14:00:00Z', lastMaintUser: 'ADMIN01', fundName: 'SPDR S&P 500 ETF' },
  { portfolioId: 'PORT0002', date: '20240215', investmentId: 'TSLA', quantity: 50, costBasis: 12500.00, marketValue: 9750.00, currency: 'USD', status: 'P', lastMaintDate: '2024-02-15T14:00:00Z', lastMaintUser: 'ADMIN01', fundName: 'TESLA INC' },
  // Account 2000000002 / PORT0003
  { portfolioId: 'PORT0003', date: '20240310', investmentId: 'AAPL', quantity: 1000, costBasis: 150000.00, marketValue: 175000.00, currency: 'USD', status: 'A', lastMaintDate: '2024-03-10T08:00:00Z', lastMaintUser: 'ADMIN02', fundName: 'APPLE INC' },
  { portfolioId: 'PORT0003', date: '20240310', investmentId: 'AMZN', quantity: 500, costBasis: 87500.00, marketValue: 92500.00, currency: 'USD', status: 'A', lastMaintDate: '2024-03-10T08:00:00Z', lastMaintUser: 'ADMIN02', fundName: 'AMAZON.COM INC' },
  { portfolioId: 'PORT0003', date: '20240310', investmentId: 'VBOND1', quantity: 5000, costBasis: 500000.00, marketValue: 515000.00, currency: 'USD', status: 'A', lastMaintDate: '2024-03-10T08:00:00Z', lastMaintUser: 'ADMIN02', fundName: 'VANGUARD BOND INDEX' },
  { portfolioId: 'PORT0003', date: '20240310', investmentId: 'MMFUND', quantity: 50000, costBasis: 500000.00, marketValue: 500500.00, currency: 'USD', status: 'A', lastMaintDate: '2024-03-10T08:00:00Z', lastMaintUser: 'ADMIN02', fundName: 'FIDELITY MONEY MARKET' },
  // Account 3000000003 / PORT0004
  { portfolioId: 'PORT0004', date: '20240101', investmentId: 'BRK.B', quantity: 300, costBasis: 108000.00, marketValue: 120000.00, currency: 'USD', status: 'A', lastMaintDate: '2024-01-01T12:00:00Z', lastMaintUser: 'ADMIN01', fundName: 'BERKSHIRE HATHAWAY B' },
  { portfolioId: 'PORT0004', date: '20240101', investmentId: 'QQQ', quantity: 400, costBasis: 160000.00, marketValue: 172000.00, currency: 'USD', status: 'A', lastMaintDate: '2024-01-01T12:00:00Z', lastMaintUser: 'ADMIN01', fundName: 'INVESCO QQQ TRUST' },
  { portfolioId: 'PORT0004', date: '20240101', investmentId: 'VBOND1', quantity: 1500, costBasis: 150000.00, marketValue: 152000.00, currency: 'USD', status: 'A', lastMaintDate: '2024-01-01T12:00:00Z', lastMaintUser: 'ADMIN01', fundName: 'VANGUARD BOND INDEX' },
  // Account 4000000004 / PORT0005
  { portfolioId: 'PORT0005', date: '20240220', investmentId: 'NVDA', quantity: 200, costBasis: 60000.00, marketValue: 70000.00, currency: 'USD', status: 'A', lastMaintDate: '2024-02-20T15:00:00Z', lastMaintUser: 'ADMIN02', fundName: 'NVIDIA CORP' },
  { portfolioId: 'PORT0005', date: '20240220', investmentId: 'AMD', quantity: 300, costBasis: 24000.00, marketValue: 25000.00, currency: 'USD', status: 'P', lastMaintDate: '2024-02-20T15:00:00Z', lastMaintUser: 'ADMIN02', fundName: 'ADV MICRO DEVICES' },
  // Account 6000000006 / PORT0007
  { portfolioId: 'PORT0007', date: '20240305', investmentId: 'AAPL', quantity: 500, costBasis: 75000.00, marketValue: 87500.00, currency: 'USD', status: 'A', lastMaintDate: '2024-03-05T09:30:00Z', lastMaintUser: 'ADMIN03', fundName: 'APPLE INC' },
  { portfolioId: 'PORT0007', date: '20240305', investmentId: 'JNJ', quantity: 600, costBasis: 96000.00, marketValue: 99000.00, currency: 'USD', status: 'A', lastMaintDate: '2024-03-05T09:30:00Z', lastMaintUser: 'ADMIN03', fundName: 'JOHNSON & JOHNSON' },
  { portfolioId: 'PORT0007', date: '20240305', investmentId: 'VBOND1', quantity: 3000, costBasis: 300000.00, marketValue: 309000.00, currency: 'USD', status: 'A', lastMaintDate: '2024-03-05T09:30:00Z', lastMaintUser: 'ADMIN03', fundName: 'VANGUARD BOND INDEX' },
  { portfolioId: 'PORT0007', date: '20240305', investmentId: 'MMFUND', quantity: 25000, costBasis: 250000.00, marketValue: 250250.00, currency: 'USD', status: 'A', lastMaintDate: '2024-03-05T09:30:00Z', lastMaintUser: 'ADMIN03', fundName: 'FIDELITY MONEY MARKET' },
  // Account 7000000007 / PORT0008
  { portfolioId: 'PORT0008', date: '20240315', investmentId: 'SPY', quantity: 100, costBasis: 45000.00, marketValue: 47500.00, currency: 'USD', status: 'A', lastMaintDate: '2024-03-15T11:00:00Z', lastMaintUser: 'ADMIN01', fundName: 'SPDR S&P 500 ETF' },
  { portfolioId: 'PORT0008', date: '20240315', investmentId: 'VBOND1', quantity: 500, costBasis: 50000.00, marketValue: 51500.00, currency: 'USD', status: 'A', lastMaintDate: '2024-03-15T11:00:00Z', lastMaintUser: 'ADMIN01', fundName: 'VANGUARD BOND INDEX' },
];

// ======================================================================
// HISTORY / TRANSACTIONS - Maps to INQHIST.cbl WS-HISTORY-TABLE
// 30+ transaction records across multiple accounts
// ======================================================================
export const mockHistoryByAccount: Record<string, HistoryEntry[]> = {
  '1000000001': [
    { date: '2024-03-01', type: 'Buy', units: 50, price: 175.00, amount: 8750.00 },
    { date: '2024-02-28', type: 'Sell', units: 20, price: 410.00, amount: 8200.00 },
    { date: '2024-02-15', type: 'Buy', units: 100, price: 143.00, amount: 14300.00 },
    { date: '2024-02-10', type: 'Buy', units: 200, price: 475.00, amount: 95000.00 },
    { date: '2024-02-01', type: 'Fee', units: 0, price: 0, amount: 25.00 },
    { date: '2024-01-25', type: 'Buy', units: 100, price: 172.00, amount: 17200.00 },
    { date: '2024-01-20', type: 'Sell', units: 30, price: 395.00, amount: 11850.00 },
    { date: '2024-01-15', type: 'Transfer', units: 500, price: 100.00, amount: 50000.00 },
    { date: '2024-01-10', type: 'Buy', units: 5000, price: 10.00, amount: 50000.00 },
    { date: '2024-01-05', type: 'Fee', units: 0, price: 0, amount: 50.00 },
    { date: '2023-12-20', type: 'Buy', units: 150, price: 170.00, amount: 25500.00 },
    { date: '2023-12-15', type: 'Sell', units: 100, price: 370.00, amount: 37000.00 },
  ],
  '2000000002': [
    { date: '2024-03-10', type: 'Buy', units: 200, price: 175.00, amount: 35000.00 },
    { date: '2024-03-05', type: 'Buy', units: 100, price: 185.00, amount: 18500.00 },
    { date: '2024-02-28', type: 'Sell', units: 50, price: 178.00, amount: 8900.00 },
    { date: '2024-02-20', type: 'Transfer', units: 1000, price: 100.00, amount: 100000.00 },
    { date: '2024-02-15', type: 'Buy', units: 500, price: 100.00, amount: 50000.00 },
    { date: '2024-02-01', type: 'Fee', units: 0, price: 0, amount: 150.00 },
    { date: '2024-01-20', type: 'Buy', units: 300, price: 172.00, amount: 51600.00 },
    { date: '2024-01-15', type: 'Sell', units: 200, price: 175.00, amount: 35000.00 },
  ],
  '3000000003': [
    { date: '2024-01-01', type: 'Buy', units: 300, price: 360.00, amount: 108000.00 },
    { date: '2023-12-15', type: 'Buy', units: 400, price: 400.00, amount: 160000.00 },
    { date: '2023-12-01', type: 'Transfer', units: 1500, price: 100.00, amount: 150000.00 },
    { date: '2023-11-20', type: 'Fee', units: 0, price: 0, amount: 75.00 },
  ],
  '4000000004': [
    { date: '2024-02-20', type: 'Buy', units: 200, price: 300.00, amount: 60000.00 },
    { date: '2024-02-15', type: 'Buy', units: 300, price: 80.00, amount: 24000.00 },
    { date: '2024-02-01', type: 'Fee', units: 0, price: 0, amount: 35.00 },
  ],
  '6000000006': [
    { date: '2024-03-05', type: 'Buy', units: 500, price: 175.00, amount: 87500.00 },
    { date: '2024-03-01', type: 'Buy', units: 600, price: 160.00, amount: 96000.00 },
    { date: '2024-02-20', type: 'Transfer', units: 3000, price: 100.00, amount: 300000.00 },
    { date: '2024-02-15', type: 'Transfer', units: 25000, price: 10.00, amount: 250000.00 },
    { date: '2024-02-01', type: 'Fee', units: 0, price: 0, amount: 100.00 },
  ],
  '7000000007': [
    { date: '2024-03-15', type: 'Buy', units: 100, price: 475.00, amount: 47500.00 },
    { date: '2024-03-10', type: 'Buy', units: 500, price: 100.00, amount: 50000.00 },
    { date: '2024-03-01', type: 'Fee', units: 0, price: 0, amount: 25.00 },
  ],
};

// ======================================================================
// FULL TRANSACTIONS - Maps to TRNREC.cpy TRANSACTION-RECORD
// ======================================================================
export const mockTransactions: Transaction[] = [
  { date: '20240301', time: '103000', portfolioId: 'PORT0001', sequenceNo: '000001', investmentId: 'AAPL', type: 'BU', quantity: 50, price: 175.00, amount: 8750.00, currency: 'USD', status: 'D', processDate: '2024-03-01T10:30:00Z', processUser: 'ADMIN01' },
  { date: '20240228', time: '143000', portfolioId: 'PORT0001', sequenceNo: '000002', investmentId: 'MSFT', type: 'SL', quantity: 20, price: 410.00, amount: 8200.00, currency: 'USD', status: 'D', processDate: '2024-02-28T14:30:00Z', processUser: 'ADMIN01' },
  { date: '20240215', time: '090000', portfolioId: 'PORT0002', sequenceNo: '000003', investmentId: 'GOOGL', type: 'BU', quantity: 75, price: 143.00, amount: 10725.00, currency: 'USD', status: 'D', processDate: '2024-02-15T09:00:00Z', processUser: 'ADMIN01' },
  { date: '20240310', time: '080000', portfolioId: 'PORT0003', sequenceNo: '000004', investmentId: 'AAPL', type: 'BU', quantity: 200, price: 175.00, amount: 35000.00, currency: 'USD', status: 'D', processDate: '2024-03-10T08:00:00Z', processUser: 'ADMIN02' },
  { date: '20240305', time: '093000', portfolioId: 'PORT0007', sequenceNo: '000005', investmentId: 'AAPL', type: 'BU', quantity: 500, price: 175.00, amount: 87500.00, currency: 'USD', status: 'D', processDate: '2024-03-05T09:30:00Z', processUser: 'ADMIN03' },
  { date: '20240220', time: '150000', portfolioId: 'PORT0005', sequenceNo: '000006', investmentId: 'NVDA', type: 'BU', quantity: 200, price: 300.00, amount: 60000.00, currency: 'USD', status: 'D', processDate: '2024-02-20T15:00:00Z', processUser: 'ADMIN02' },
  { date: '20240315', time: '110000', portfolioId: 'PORT0008', sequenceNo: '000007', investmentId: 'SPY', type: 'BU', quantity: 100, price: 475.00, amount: 47500.00, currency: 'USD', status: 'D', processDate: '2024-03-15T11:00:00Z', processUser: 'ADMIN01' },
  { date: '20240220', time: '153000', portfolioId: 'PORT0005', sequenceNo: '000008', investmentId: 'AMD', type: 'BU', quantity: 300, price: 80.00, amount: 24000.00, currency: 'USD', status: 'P', processDate: '2024-02-20T15:30:00Z', processUser: 'ADMIN02' },
  { date: '20240101', time: '120000', portfolioId: 'PORT0004', sequenceNo: '000009', investmentId: 'BRK.B', type: 'BU', quantity: 300, price: 360.00, amount: 108000.00, currency: 'USD', status: 'D', processDate: '2024-01-01T12:00:00Z', processUser: 'ADMIN01' },
  { date: '20240115', time: '093000', portfolioId: 'PORT0001', sequenceNo: '000010', investmentId: 'MMFUND', type: 'TR', quantity: 500, price: 100.00, amount: 50000.00, currency: 'USD', status: 'D', processDate: '2024-01-15T09:30:00Z', processUser: 'ADMIN01' },
];

// ======================================================================
// AUDIT LOG - Maps to AUDITLOG.cpy AUDIT-RECORD
// ======================================================================
export const mockAuditRecords: AuditRecord[] = [
  { timestamp: '2024-03-15T11:00:00Z', systemId: 'PROD01', userId: 'ADMIN01', program: 'PORTTRAN', terminal: 'TERM001', type: 'TRAN', action: 'CREATE', status: 'SUCC', portfolioId: 'PORT0008', accountNumber: '7000000007', beforeImage: '', afterImage: 'Buy 100 SPY @ 475.00', message: 'Transaction processed successfully' },
  { timestamp: '2024-03-10T08:00:00Z', systemId: 'PROD01', userId: 'ADMIN02', program: 'PORTTRAN', terminal: 'TERM002', type: 'TRAN', action: 'CREATE', status: 'SUCC', portfolioId: 'PORT0003', accountNumber: '2000000002', beforeImage: '', afterImage: 'Buy 200 AAPL @ 175.00', message: 'Transaction processed successfully' },
  { timestamp: '2024-03-05T09:30:00Z', systemId: 'PROD01', userId: 'ADMIN03', program: 'PORTMSTR', terminal: 'TERM003', type: 'TRAN', action: 'UPDATE', status: 'SUCC', portfolioId: 'PORT0007', accountNumber: '6000000006', beforeImage: 'Value: 800000.00', afterImage: 'Value: 850000.00', message: 'Portfolio value updated' },
  { timestamp: '2024-03-01T10:30:00Z', systemId: 'PROD01', userId: 'ADMIN01', program: 'PORTTRAN', terminal: 'TERM001', type: 'TRAN', action: 'CREATE', status: 'SUCC', portfolioId: 'PORT0001', accountNumber: '1000000001', beforeImage: '', afterImage: 'Buy 50 AAPL @ 175.00', message: 'Transaction processed successfully' },
  { timestamp: '2024-02-28T14:30:00Z', systemId: 'PROD01', userId: 'ADMIN01', program: 'PORTTRAN', terminal: 'TERM001', type: 'TRAN', action: 'CREATE', status: 'SUCC', portfolioId: 'PORT0001', accountNumber: '1000000001', beforeImage: '', afterImage: 'Sell 20 MSFT @ 410.00', message: 'Transaction processed successfully' },
  { timestamp: '2024-02-20T15:00:00Z', systemId: 'PROD01', userId: 'ADMIN02', program: 'SECMGR', terminal: 'TERM002', type: 'USER', action: 'LOGIN', status: 'SUCC', portfolioId: '', accountNumber: '', beforeImage: '', afterImage: '', message: 'User login successful' },
  { timestamp: '2024-02-20T14:50:00Z', systemId: 'PROD01', userId: 'ADMIN02', program: 'SECMGR', terminal: 'TERM002', type: 'USER', action: 'LOGIN', status: 'FAIL', portfolioId: '', accountNumber: '', beforeImage: '', afterImage: '', message: 'Invalid credentials' },
  { timestamp: '2024-02-15T09:00:00Z', systemId: 'PROD01', userId: 'ADMIN01', program: 'INQONLN', terminal: 'TERM001', type: 'USER', action: 'INQUIRE', status: 'SUCC', portfolioId: 'PORT0002', accountNumber: '1000000001', beforeImage: '', afterImage: '', message: 'Position inquiry completed' },
  { timestamp: '2024-02-01T00:00:00Z', systemId: 'BATCH01', userId: 'BATCHUSR', program: 'BCHCTL00', terminal: 'BATCH', type: 'SYST', action: 'STARTUP', status: 'SUCC', portfolioId: '', accountNumber: '', beforeImage: '', afterImage: '', message: 'Batch processing cycle started' },
  { timestamp: '2024-02-01T04:00:00Z', systemId: 'BATCH01', userId: 'BATCHUSR', program: 'BCHCTL00', terminal: 'BATCH', type: 'SYST', action: 'SHUTDOWN', status: 'SUCC', portfolioId: '', accountNumber: '', beforeImage: '', afterImage: '', message: 'Batch processing cycle completed' },
  { timestamp: '2024-01-25T16:00:00Z', systemId: 'PROD01', userId: 'ADMIN01', program: 'PORTDEL', terminal: 'TERM001', type: 'TRAN', action: 'DELETE', status: 'SUCC', portfolioId: 'PORT0006', accountNumber: '5000000005', beforeImage: 'Status: A, Value: 350000.00', afterImage: 'Status: C, Value: 0.00', message: 'Portfolio closed and deleted' },
  { timestamp: '2024-01-20T11:00:00Z', systemId: 'PROD01', userId: 'ADMIN03', program: 'PORTMSTR', terminal: 'TERM003', type: 'TRAN', action: 'CREATE', status: 'SUCC', portfolioId: 'PORT0008', accountNumber: '7000000007', beforeImage: '', afterImage: 'New portfolio created', message: 'Portfolio created successfully' },
  { timestamp: '2024-01-15T08:00:00Z', systemId: 'PROD01', userId: 'ADMIN01', program: 'PORTUPDT', terminal: 'TERM001', type: 'TRAN', action: 'UPDATE', status: 'FAIL', portfolioId: 'PORT0009', accountNumber: '9999999999', beforeImage: '', afterImage: '', message: 'Record not found - VSAM status 23' },
  { timestamp: '2024-01-10T06:00:00Z', systemId: 'BATCH01', userId: 'BATCHUSR', program: 'RPTPOS00', terminal: 'BATCH', type: 'SYST', action: 'STARTUP', status: 'SUCC', portfolioId: '', accountNumber: '', beforeImage: '', afterImage: '', message: 'Position valuation report started' },
  { timestamp: '2024-01-10T06:30:00Z', systemId: 'BATCH01', userId: 'BATCHUSR', program: 'RPTPOS00', terminal: 'BATCH', type: 'SYST', action: 'SHUTDOWN', status: 'SUCC', portfolioId: '', accountNumber: '', beforeImage: '', afterImage: '', message: 'Position valuation report completed - 156 records' },
];

// ======================================================================
// VALUATION REPORT DATA - Maps to RPTPOS00.cbl
// ======================================================================
export interface ValuationReportRow {
  portfolioId: string;
  description: string;
  quantity: number;
  currentValue: number;
  previousValue: number;
  changePercent: number;
}

export const mockValuationReport: ValuationReportRow[] = [
  { portfolioId: 'PORT0001', description: 'JOHN SMITH - Individual', quantity: 10750, currentValue: 219200.00, previousValue: 207500.00, changePercent: 5.64 },
  { portfolioId: 'PORT0002', description: 'JOHN SMITH - Growth', quantity: 325, currentValue: 115475.00, previousValue: 113000.00, changePercent: 2.19 },
  { portfolioId: 'PORT0003', description: 'ACME CORPORATION', quantity: 56500, currentValue: 1283000.00, previousValue: 1237500.00, changePercent: 3.68 },
  { portfolioId: 'PORT0004', description: 'FAMILY TRUST ALPHA', quantity: 2200, currentValue: 444000.00, previousValue: 418000.00, changePercent: 6.22 },
  { portfolioId: 'PORT0005', description: 'JANE DOE', quantity: 500, currentValue: 95000.00, previousValue: 84000.00, changePercent: 13.10 },
  { portfolioId: 'PORT0007', description: 'WILLIAMS ESTATE TRUST', quantity: 29100, currentValue: 745750.00, previousValue: 721000.00, changePercent: 3.43 },
  { portfolioId: 'PORT0008', description: 'ROBERT JOHNSON', quantity: 600, currentValue: 99000.00, previousValue: 95000.00, changePercent: 4.21 },
];

// ======================================================================
// SYSTEM STATS - Maps to RPTSTA00.cbl DB2/Batch metrics
// ======================================================================
export interface SystemStats {
  db2Metrics: {
    totalQueries: number;
    avgResponseTimeMs: number;
    peakResponseTimeMs: number;
    activeConnections: number;
    deadlockCount: number;
    bufferPoolHitRatio: number;
  };
  batchMetrics: {
    totalJobsRun: number;
    successfulJobs: number;
    failedJobs: number;
    avgDurationMinutes: number;
    recordsProcessed: number;
    lastRunDate: string;
  };
}

export const mockSystemStats: SystemStats = {
  db2Metrics: {
    totalQueries: 15420,
    avgResponseTimeMs: 12,
    peakResponseTimeMs: 245,
    activeConnections: 8,
    deadlockCount: 0,
    bufferPoolHitRatio: 98.5,
  },
  batchMetrics: {
    totalJobsRun: 342,
    successfulJobs: 338,
    failedJobs: 4,
    avgDurationMinutes: 23,
    recordsProcessed: 156780,
    lastRunDate: '2024-03-15',
  },
};
