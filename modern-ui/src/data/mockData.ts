import type { Portfolio, Transaction, Position, AuditRecord, BatchJob, HistoryRecord } from '../types';

const INVESTMENT_NAMES: Record<string, string> = {
  'INV-001001': 'Vanguard S&P 500 ETF',
  'INV-002001': 'iShares Core US Aggregate Bond',
  'INV-003001': 'Fidelity Growth Fund',
  'INV-004001': 'Schwab International Equity',
  'INV-005001': 'PIMCO Total Return Fund',
  'INV-006001': 'BlackRock Global Allocation',
  'INV-007001': 'T. Rowe Price Blue Chip Growth',
  'INV-008001': 'JPMorgan Equity Income',
  'INV-009001': 'Goldman Sachs Bond Fund',
  'INV-010001': 'Morgan Stanley Real Estate',
};

export const portfolios: Portfolio[] = [
  {
    id: 'PORT0001', accountNo: 'ACCT100001', clientName: 'Meridian Capital Partners',
    clientType: 'C', createDate: '2023-01-15', lastMaintDate: '2026-04-28',
    status: 'A', totalValue: 12_450_890.75, cashBalance: 1_250_000.00,
    lastUser: 'JSMITH01', lastTransDate: '2026-04-28',
  },
  {
    id: 'PORT0002', accountNo: 'ACCT100002', clientName: 'Eleanor M. Richardson',
    clientType: 'I', createDate: '2022-06-10', lastMaintDate: '2026-04-27',
    status: 'A', totalValue: 3_875_200.50, cashBalance: 425_000.00,
    lastUser: 'RBROWN02', lastTransDate: '2026-04-27',
  },
  {
    id: 'PORT0003', accountNo: 'ACCT100003', clientName: 'Westfield Family Trust',
    clientType: 'T', createDate: '2021-11-20', lastMaintDate: '2026-04-25',
    status: 'A', totalValue: 8_920_000.00, cashBalance: 890_000.00,
    lastUser: 'KDAVIS03', lastTransDate: '2026-04-25',
  },
  {
    id: 'PORT0004', accountNo: 'ACCT100004', clientName: 'Apex Industrial Holdings',
    clientType: 'C', createDate: '2023-03-01', lastMaintDate: '2026-04-29',
    status: 'A', totalValue: 25_100_500.00, cashBalance: 3_200_000.00,
    lastUser: 'MWILSN04', lastTransDate: '2026-04-29',
  },
  {
    id: 'PORT0005', accountNo: 'ACCT100005', clientName: 'Robert J. Hawthorne',
    clientType: 'I', createDate: '2024-01-08', lastMaintDate: '2026-04-20',
    status: 'A', totalValue: 1_520_750.25, cashBalance: 150_000.00,
    lastUser: 'JSMITH01', lastTransDate: '2026-04-20',
  },
  {
    id: 'PORT0006', accountNo: 'ACCT100006', clientName: 'Pinnacle Growth Corp',
    clientType: 'C', createDate: '2022-09-15', lastMaintDate: '2026-03-15',
    status: 'S', totalValue: 6_780_000.00, cashBalance: 500_000.00,
    lastUser: 'ADMIN01', lastTransDate: '2026-03-15',
  },
  {
    id: 'PORT0007', accountNo: 'ACCT100007', clientName: 'Heritage Foundation Trust',
    clientType: 'T', createDate: '2020-05-12', lastMaintDate: '2025-12-31',
    status: 'C', totalValue: 0, cashBalance: 0,
    lastUser: 'SYSTEM', lastTransDate: '2025-12-31',
  },
  {
    id: 'PORT0008', accountNo: 'ACCT100008', clientName: 'Sarah L. Chen',
    clientType: 'I', createDate: '2024-07-22', lastMaintDate: '2026-04-29',
    status: 'A', totalValue: 2_340_600.80, cashBalance: 280_000.00,
    lastUser: 'RBROWN02', lastTransDate: '2026-04-29',
  },
];

export const transactions: Transaction[] = [
  { id: 'TXN-000001', date: '2026-04-29', time: '09:15:30', portfolioId: 'PORT0001', sequenceNo: '000001', investmentId: 'INV-001001', type: 'BU', quantity: 500, price: 485.25, amount: 242_625.00, currency: 'USD', status: 'D', processDate: '2026-04-29', processUser: 'JSMITH01' },
  { id: 'TXN-000002', date: '2026-04-29', time: '09:32:15', portfolioId: 'PORT0004', sequenceNo: '000001', investmentId: 'INV-003001', type: 'BU', quantity: 1200, price: 125.80, amount: 150_960.00, currency: 'USD', status: 'D', processDate: '2026-04-29', processUser: 'MWILSN04' },
  { id: 'TXN-000003', date: '2026-04-29', time: '10:05:00', portfolioId: 'PORT0002', sequenceNo: '000001', investmentId: 'INV-005001', type: 'SL', quantity: 300, price: 98.50, amount: 29_550.00, currency: 'USD', status: 'D', processDate: '2026-04-29', processUser: 'RBROWN02' },
  { id: 'TXN-000004', date: '2026-04-28', time: '14:20:00', portfolioId: 'PORT0001', sequenceNo: '000002', investmentId: 'INV-007001', type: 'BU', quantity: 800, price: 178.90, amount: 143_120.00, currency: 'USD', status: 'D', processDate: '2026-04-28', processUser: 'JSMITH01' },
  { id: 'TXN-000005', date: '2026-04-28', time: '15:10:30', portfolioId: 'PORT0003', sequenceNo: '000001', investmentId: 'INV-004001', type: 'TR', quantity: 450, price: 62.30, amount: 28_035.00, currency: 'USD', status: 'D', processDate: '2026-04-28', processUser: 'KDAVIS03' },
  { id: 'TXN-000006', date: '2026-04-28', time: '16:00:00', portfolioId: 'PORT0004', sequenceNo: '000002', investmentId: 'INV-001001', type: 'FE', quantity: 0, price: 0, amount: 1_250.00, currency: 'USD', status: 'D', processDate: '2026-04-28', processUser: 'SYSTEM' },
  { id: 'TXN-000007', date: '2026-04-27', time: '11:30:00', portfolioId: 'PORT0002', sequenceNo: '000002', investmentId: 'INV-006001', type: 'BU', quantity: 600, price: 215.40, amount: 129_240.00, currency: 'USD', status: 'D', processDate: '2026-04-27', processUser: 'RBROWN02' },
  { id: 'TXN-000008', date: '2026-04-27', time: '13:45:00', portfolioId: 'PORT0008', sequenceNo: '000001', investmentId: 'INV-008001', type: 'BU', quantity: 350, price: 92.15, amount: 32_252.50, currency: 'USD', status: 'D', processDate: '2026-04-27', processUser: 'RBROWN02' },
  { id: 'TXN-000009', date: '2026-04-26', time: '09:00:00', portfolioId: 'PORT0005', sequenceNo: '000001', investmentId: 'INV-002001', type: 'BU', quantity: 1500, price: 104.20, amount: 156_300.00, currency: 'USD', status: 'P', processDate: '', processUser: '' },
  { id: 'TXN-000010', date: '2026-04-25', time: '10:30:00', portfolioId: 'PORT0003', sequenceNo: '000002', investmentId: 'INV-010001', type: 'SL', quantity: 200, price: 45.60, amount: 9_120.00, currency: 'USD', status: 'F', processDate: '2026-04-25', processUser: 'SYSTEM' },
  { id: 'TXN-000011', date: '2026-04-25', time: '14:15:00', portfolioId: 'PORT0001', sequenceNo: '000003', investmentId: 'INV-009001', type: 'BU', quantity: 2000, price: 52.80, amount: 105_600.00, currency: 'USD', status: 'D', processDate: '2026-04-25', processUser: 'JSMITH01' },
  { id: 'TXN-000012', date: '2026-04-24', time: '11:00:00', portfolioId: 'PORT0004', sequenceNo: '000003', investmentId: 'INV-002001', type: 'BU', quantity: 3000, price: 103.90, amount: 311_700.00, currency: 'USD', status: 'D', processDate: '2026-04-24', processUser: 'MWILSN04' },
];

export const positions: Position[] = [
  { portfolioId: 'PORT0001', date: '2026-04-29', investmentId: 'INV-001001', investmentName: INVESTMENT_NAMES['INV-001001'], quantity: 4500, costBasis: 2_025_000.00, marketValue: 2_183_625.00, currency: 'USD', status: 'A', lastMaintDate: '2026-04-29', lastMaintUser: 'JSMITH01' },
  { portfolioId: 'PORT0001', date: '2026-04-29', investmentId: 'INV-003001', investmentName: INVESTMENT_NAMES['INV-003001'], quantity: 3200, costBasis: 384_000.00, marketValue: 402_560.00, currency: 'USD', status: 'A', lastMaintDate: '2026-04-28', lastMaintUser: 'JSMITH01' },
  { portfolioId: 'PORT0001', date: '2026-04-29', investmentId: 'INV-007001', investmentName: INVESTMENT_NAMES['INV-007001'], quantity: 2800, costBasis: 478_800.00, marketValue: 500_920.00, currency: 'USD', status: 'A', lastMaintDate: '2026-04-28', lastMaintUser: 'JSMITH01' },
  { portfolioId: 'PORT0001', date: '2026-04-29', investmentId: 'INV-009001', investmentName: INVESTMENT_NAMES['INV-009001'], quantity: 5000, costBasis: 260_000.00, marketValue: 264_000.00, currency: 'USD', status: 'A', lastMaintDate: '2026-04-25', lastMaintUser: 'JSMITH01' },
  { portfolioId: 'PORT0002', date: '2026-04-29', investmentId: 'INV-005001', investmentName: INVESTMENT_NAMES['INV-005001'], quantity: 1700, costBasis: 164_900.00, marketValue: 167_450.00, currency: 'USD', status: 'A', lastMaintDate: '2026-04-29', lastMaintUser: 'RBROWN02' },
  { portfolioId: 'PORT0002', date: '2026-04-29', investmentId: 'INV-006001', investmentName: INVESTMENT_NAMES['INV-006001'], quantity: 2100, costBasis: 441_000.00, marketValue: 452_340.00, currency: 'USD', status: 'A', lastMaintDate: '2026-04-27', lastMaintUser: 'RBROWN02' },
  { portfolioId: 'PORT0003', date: '2026-04-29', investmentId: 'INV-004001', investmentName: INVESTMENT_NAMES['INV-004001'], quantity: 6200, costBasis: 380_260.00, marketValue: 386_260.00, currency: 'USD', status: 'A', lastMaintDate: '2026-04-28', lastMaintUser: 'KDAVIS03' },
  { portfolioId: 'PORT0003', date: '2026-04-29', investmentId: 'INV-001001', investmentName: INVESTMENT_NAMES['INV-001001'], quantity: 8000, costBasis: 3_600_000.00, marketValue: 3_882_000.00, currency: 'USD', status: 'A', lastMaintDate: '2026-04-20', lastMaintUser: 'KDAVIS03' },
  { portfolioId: 'PORT0004', date: '2026-04-29', investmentId: 'INV-001001', investmentName: INVESTMENT_NAMES['INV-001001'], quantity: 15000, costBasis: 6_750_000.00, marketValue: 7_278_750.00, currency: 'USD', status: 'A', lastMaintDate: '2026-04-29', lastMaintUser: 'MWILSN04' },
  { portfolioId: 'PORT0004', date: '2026-04-29', investmentId: 'INV-003001', investmentName: INVESTMENT_NAMES['INV-003001'], quantity: 5000, costBasis: 600_000.00, marketValue: 629_000.00, currency: 'USD', status: 'A', lastMaintDate: '2026-04-29', lastMaintUser: 'MWILSN04' },
  { portfolioId: 'PORT0004', date: '2026-04-29', investmentId: 'INV-002001', investmentName: INVESTMENT_NAMES['INV-002001'], quantity: 8000, costBasis: 832_000.00, marketValue: 833_600.00, currency: 'USD', status: 'A', lastMaintDate: '2026-04-24', lastMaintUser: 'MWILSN04' },
  { portfolioId: 'PORT0005', date: '2026-04-29', investmentId: 'INV-002001', investmentName: INVESTMENT_NAMES['INV-002001'], quantity: 4500, costBasis: 459_000.00, marketValue: 468_900.00, currency: 'USD', status: 'A', lastMaintDate: '2026-04-20', lastMaintUser: 'JSMITH01' },
  { portfolioId: 'PORT0008', date: '2026-04-29', investmentId: 'INV-008001', investmentName: INVESTMENT_NAMES['INV-008001'], quantity: 3500, costBasis: 315_000.00, marketValue: 322_525.00, currency: 'USD', status: 'A', lastMaintDate: '2026-04-27', lastMaintUser: 'RBROWN02' },
  { portfolioId: 'PORT0008', date: '2026-04-29', investmentId: 'INV-001001', investmentName: INVESTMENT_NAMES['INV-001001'], quantity: 2000, costBasis: 920_000.00, marketValue: 970_500.00, currency: 'USD', status: 'A', lastMaintDate: '2026-04-15', lastMaintUser: 'RBROWN02' },
];

export const auditRecords: AuditRecord[] = [
  { id: 'AUD-001', timestamp: '2026-04-29 09:15:32', systemId: 'PROD-01', userId: 'JSMITH01', program: 'PORTADD', terminal: 'TRM001', type: 'TRAN', action: 'CREATE', status: 'SUCC', portfolioId: 'PORT0001', accountNo: 'ACCT100001', message: 'Buy order executed: 500 units INV-001001 at $485.25' },
  { id: 'AUD-002', timestamp: '2026-04-29 09:32:18', systemId: 'PROD-01', userId: 'MWILSN04', program: 'TRNVAL00', terminal: 'TRM003', type: 'TRAN', action: 'CREATE', status: 'SUCC', portfolioId: 'PORT0004', accountNo: 'ACCT100004', message: 'Buy order executed: 1200 units INV-003001 at $125.80' },
  { id: 'AUD-003', timestamp: '2026-04-29 10:05:02', systemId: 'PROD-01', userId: 'RBROWN02', program: 'TRNVAL00', terminal: 'TRM002', type: 'TRAN', action: 'CREATE', status: 'SUCC', portfolioId: 'PORT0002', accountNo: 'ACCT100002', message: 'Sell order executed: 300 units INV-005001 at $98.50' },
  { id: 'AUD-004', timestamp: '2026-04-29 08:00:00', systemId: 'PROD-01', userId: 'SYSTEM', program: 'BCHCTL00', terminal: 'BATCH', type: 'SYST', action: 'STARTUP', status: 'SUCC', portfolioId: '', accountNo: '', message: 'Batch control system initialized for processing date 2026-04-29' },
  { id: 'AUD-005', timestamp: '2026-04-29 08:00:05', systemId: 'PROD-01', userId: 'SYSTEM', program: 'PRCSEQ00', terminal: 'BATCH', type: 'SYST', action: 'CREATE', status: 'SUCC', portfolioId: '', accountNo: '', message: 'Process sequence established: TRNVAL → POSUPDT → HISTLD → RPT' },
  { id: 'AUD-006', timestamp: '2026-04-28 16:00:01', systemId: 'PROD-01', userId: 'SYSTEM', program: 'POSUPDT', terminal: 'BATCH', type: 'SYST', action: 'UPDATE', status: 'SUCC', portfolioId: '', accountNo: '', message: 'End-of-day position update completed: 847 positions updated' },
  { id: 'AUD-007', timestamp: '2026-04-28 16:30:00', systemId: 'PROD-01', userId: 'SYSTEM', program: 'HISTLD00', terminal: 'BATCH', type: 'SYST', action: 'CREATE', status: 'SUCC', portfolioId: '', accountNo: '', message: 'History load to DB2 completed: 1,247 records loaded' },
  { id: 'AUD-008', timestamp: '2026-04-28 17:00:00', systemId: 'PROD-01', userId: 'SYSTEM', program: 'RPTPOS00', terminal: 'BATCH', type: 'SYST', action: 'CREATE', status: 'SUCC', portfolioId: '', accountNo: '', message: 'Daily position report generated: 42 pages' },
  { id: 'AUD-009', timestamp: '2026-04-25 10:30:05', systemId: 'PROD-01', userId: 'SYSTEM', program: 'TRNVAL00', terminal: 'BATCH', type: 'TRAN', action: 'UPDATE', status: 'FAIL', portfolioId: 'PORT0003', accountNo: 'ACCT100003', message: 'Transaction validation failed: insufficient holdings for sell order' },
  { id: 'AUD-010', timestamp: '2026-04-29 07:55:00', systemId: 'PROD-01', userId: 'ADMIN01', program: 'SECMGR', terminal: 'TRM000', type: 'USER', action: 'LOGIN', status: 'SUCC', portfolioId: '', accountNo: '', message: 'Administrator login from terminal TRM000' },
  { id: 'AUD-011', timestamp: '2026-04-29 06:00:00', systemId: 'PROD-01', userId: 'SYSTEM', program: 'UTLMNT00', terminal: 'BATCH', type: 'SYST', action: 'UPDATE', status: 'SUCC', portfolioId: '', accountNo: '', message: 'VSAM file maintenance completed: 3 files reorganized' },
  { id: 'AUD-012', timestamp: '2026-04-29 06:30:00', systemId: 'PROD-01', userId: 'SYSTEM', program: 'UTLMON00', terminal: 'BATCH', type: 'SYST', action: 'INQUIRE', status: 'WARN', portfolioId: '', accountNo: '', message: 'CPU utilization at 78% — approaching threshold' },
];

export const batchJobs: BatchJob[] = [
  { id: 'BCH-001', jobName: 'TRNVAL', processDate: '2026-04-29', sequenceNo: 1, status: 'D', stepName: 'STEP010', programName: 'TRNVAL00', startTime: '08:00:15', endTime: '08:12:30', prereqCount: 0, prereqs: [], returnCode: 0, errorDesc: '', restartCount: 0, attemptTs: '2026-04-29 08:00:15', completeTs: '2026-04-29 08:12:30' },
  { id: 'BCH-002', jobName: 'POSUPDT', processDate: '2026-04-29', sequenceNo: 2, status: 'D', stepName: 'STEP020', programName: 'POSUPD00', startTime: '08:12:35', endTime: '08:25:10', prereqCount: 1, prereqs: [{ name: 'TRNVAL', sequenceNo: 1, returnCode: 0 }], returnCode: 0, errorDesc: '', restartCount: 0, attemptTs: '2026-04-29 08:12:35', completeTs: '2026-04-29 08:25:10' },
  { id: 'BCH-003', jobName: 'HISTLD', processDate: '2026-04-29', sequenceNo: 3, status: 'D', stepName: 'STEP030', programName: 'HISTLD00', startTime: '08:25:15', endTime: '08:40:00', prereqCount: 1, prereqs: [{ name: 'POSUPDT', sequenceNo: 2, returnCode: 0 }], returnCode: 0, errorDesc: '', restartCount: 0, attemptTs: '2026-04-29 08:25:15', completeTs: '2026-04-29 08:40:00' },
  { id: 'BCH-004', jobName: 'RPTPOS', processDate: '2026-04-29', sequenceNo: 4, status: 'A', stepName: 'STEP040', programName: 'RPTPOS00', startTime: '08:40:05', endTime: '', prereqCount: 1, prereqs: [{ name: 'HISTLD', sequenceNo: 3, returnCode: 0 }], returnCode: -1, errorDesc: '', restartCount: 0, attemptTs: '2026-04-29 08:40:05', completeTs: '' },
  { id: 'BCH-005', jobName: 'RPTAUD', processDate: '2026-04-29', sequenceNo: 5, status: 'W', stepName: 'STEP050', programName: 'RPTAUD00', startTime: '', endTime: '', prereqCount: 1, prereqs: [{ name: 'RPTPOS', sequenceNo: 4, returnCode: -1 }], returnCode: -1, errorDesc: '', restartCount: 0, attemptTs: '', completeTs: '' },
  { id: 'BCH-006', jobName: 'RPTSTA', processDate: '2026-04-29', sequenceNo: 6, status: 'W', stepName: 'STEP060', programName: 'RPTSTA00', startTime: '', endTime: '', prereqCount: 1, prereqs: [{ name: 'RPTAUD', sequenceNo: 5, returnCode: -1 }], returnCode: -1, errorDesc: '', restartCount: 0, attemptTs: '', completeTs: '' },
  { id: 'BCH-007', jobName: 'UTLMNT', processDate: '2026-04-29', sequenceNo: 7, status: 'R', stepName: 'STEP070', programName: 'UTLMNT00', startTime: '', endTime: '', prereqCount: 0, prereqs: [], returnCode: -1, errorDesc: '', restartCount: 0, attemptTs: '', completeTs: '' },
  { id: 'BCH-008', jobName: 'TRNVAL', processDate: '2026-04-28', sequenceNo: 1, status: 'D', stepName: 'STEP010', programName: 'TRNVAL00', startTime: '08:00:10', endTime: '08:11:45', prereqCount: 0, prereqs: [], returnCode: 0, errorDesc: '', restartCount: 0, attemptTs: '2026-04-28 08:00:10', completeTs: '2026-04-28 08:11:45' },
  { id: 'BCH-009', jobName: 'POSUPDT', processDate: '2026-04-28', sequenceNo: 2, status: 'D', stepName: 'STEP020', programName: 'POSUPD00', startTime: '08:11:50', endTime: '08:24:30', prereqCount: 1, prereqs: [{ name: 'TRNVAL', sequenceNo: 1, returnCode: 0 }], returnCode: 0, errorDesc: '', restartCount: 0, attemptTs: '2026-04-28 08:11:50', completeTs: '2026-04-28 08:24:30' },
  { id: 'BCH-010', jobName: 'HISTLD', processDate: '2026-04-28', sequenceNo: 3, status: 'E', stepName: 'STEP030', programName: 'HISTLD00', startTime: '08:24:35', endTime: '08:26:00', prereqCount: 1, prereqs: [{ name: 'POSUPDT', sequenceNo: 2, returnCode: 0 }], returnCode: 8, errorDesc: 'DB2 connection timeout on HISTTBL insert', restartCount: 1, attemptTs: '2026-04-28 08:24:35', completeTs: '2026-04-28 08:26:00' },
];

export const historyRecords: HistoryRecord[] = [
  { portfolioId: 'PORT0001', date: '2026-04-29', time: '09:15:30', seqNo: '0001', recordType: 'TR', actionCode: 'A', reasonCode: 'BUY', processDate: '2026-04-29', processUser: 'JSMITH01', description: 'Buy 500 units Vanguard S&P 500 ETF' },
  { portfolioId: 'PORT0001', date: '2026-04-28', time: '14:20:00', seqNo: '0002', recordType: 'TR', actionCode: 'A', reasonCode: 'BUY', processDate: '2026-04-28', processUser: 'JSMITH01', description: 'Buy 800 units T. Rowe Price Blue Chip Growth' },
  { portfolioId: 'PORT0001', date: '2026-04-28', time: '16:00:00', seqNo: '0003', recordType: 'PS', actionCode: 'C', reasonCode: 'EOD', processDate: '2026-04-28', processUser: 'SYSTEM', description: 'End-of-day position recalculation' },
  { portfolioId: 'PORT0002', date: '2026-04-29', time: '10:05:00', seqNo: '0001', recordType: 'TR', actionCode: 'A', reasonCode: 'SELL', processDate: '2026-04-29', processUser: 'RBROWN02', description: 'Sell 300 units PIMCO Total Return Fund' },
  { portfolioId: 'PORT0003', date: '2026-04-28', time: '15:10:30', seqNo: '0001', recordType: 'TR', actionCode: 'A', reasonCode: 'XFER', processDate: '2026-04-28', processUser: 'KDAVIS03', description: 'Transfer 450 units Schwab International Equity' },
  { portfolioId: 'PORT0004', date: '2026-04-29', time: '09:32:15', seqNo: '0001', recordType: 'TR', actionCode: 'A', reasonCode: 'BUY', processDate: '2026-04-29', processUser: 'MWILSN04', description: 'Buy 1200 units Fidelity Growth Fund' },
];

// Helpers for dashboard aggregations
export function getPortfolioTotalAUM(): number {
  return portfolios.filter(p => p.status === 'A').reduce((sum, p) => sum + p.totalValue, 0);
}

export function getActivePortfolioCount(): number {
  return portfolios.filter(p => p.status === 'A').length;
}

export function getTodayTransactionCount(): number {
  return transactions.filter(t => t.date === '2026-04-29').length;
}

export function getPendingTransactionCount(): number {
  return transactions.filter(t => t.status === 'P').length;
}

export function getPortfolioValueHistory(): { date: string; value: number }[] {
  return [
    { date: 'Jan', value: 52_100_000 },
    { date: 'Feb', value: 53_800_000 },
    { date: 'Mar', value: 51_200_000 },
    { date: 'Apr', value: 54_500_000 },
    { date: 'May', value: 56_200_000 },
    { date: 'Jun', value: 55_000_000 },
    { date: 'Jul', value: 57_800_000 },
    { date: 'Aug', value: 56_500_000 },
    { date: 'Sep', value: 58_100_000 },
    { date: 'Oct', value: 57_200_000 },
    { date: 'Nov', value: 59_400_000 },
    { date: 'Dec', value: 60_987_941 },
  ];
}

export function getTransactionVolumeByType(): { type: string; count: number; amount: number }[] {
  const types = ['BU', 'SL', 'TR', 'FE'] as const;
  return types.map(t => ({
    type: t === 'BU' ? 'Buy' : t === 'SL' ? 'Sell' : t === 'TR' ? 'Transfer' : 'Fee',
    count: transactions.filter(txn => txn.type === t).length,
    amount: transactions.filter(txn => txn.type === t).reduce((s, txn) => s + txn.amount, 0),
  }));
}
