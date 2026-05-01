import { PrismaClient } from '@prisma/client';
import Decimal from 'decimal.js';

const prisma = new PrismaClient();

// TSTGEN00.cbl — Test Data Generator equivalent
// Generates sample portfolios, positions, transactions, and audit records
async function main() {
  console.log('Seeding database...');

  // Create demo users (SECMGR.cbl)
  const users = await Promise.all([
    prisma.user.upsert({
      where: { username: 'admin' },
      update: {},
      create: { userId: 'ADMIN001', username: 'admin', password: 'admin123', role: 'admin' },
    }),
    prisma.user.upsert({
      where: { username: 'trader1' },
      update: {},
      create: { userId: 'TRADER01', username: 'trader1', password: 'trader123', role: 'user' },
    }),
    prisma.user.upsert({
      where: { username: 'viewer1' },
      update: {},
      create: { userId: 'VIEWER01', username: 'viewer1', password: 'viewer123', role: 'viewer' },
    }),
  ]);

  console.log(`Created ${users.length} users`);

  // Create sample portfolios (TSTGEN00 PORTFOLIO test type)
  const portfolioData = [
    {
      portfolioId: 'PORT0001',
      accountType: 'IN',
      branchId: '01',
      clientId: '0000000001',
      portfolioName: 'Growth Equity Fund',
      currencyCode: 'USD',
      riskLevel: '4',
      status: 'A',
      totalValue: new Decimal(250000),
      cashBalance: new Decimal(15000),
    },
    {
      portfolioId: 'PORT0002',
      accountType: 'CO',
      branchId: '01',
      clientId: '0000000002',
      portfolioName: 'Corporate Bond Portfolio',
      currencyCode: 'USD',
      riskLevel: '2',
      status: 'A',
      totalValue: new Decimal(500000),
      cashBalance: new Decimal(25000),
    },
    {
      portfolioId: 'PORT0003',
      accountType: 'TR',
      branchId: '02',
      clientId: '0000000003',
      portfolioName: 'Family Trust Fund',
      currencyCode: 'USD',
      riskLevel: '3',
      status: 'A',
      totalValue: new Decimal(1000000),
      cashBalance: new Decimal(50000),
    },
    {
      portfolioId: 'PORT0004',
      accountType: 'IN',
      branchId: '02',
      clientId: '0000000004',
      portfolioName: 'Conservative Income',
      currencyCode: 'USD',
      riskLevel: '1',
      status: 'A',
      totalValue: new Decimal(175000),
      cashBalance: new Decimal(10000),
    },
    {
      portfolioId: 'PORT0005',
      accountType: 'IN',
      branchId: '01',
      clientId: '0000000005',
      portfolioName: 'Tech Growth Portfolio',
      currencyCode: 'USD',
      riskLevel: '5',
      status: 'A',
      totalValue: new Decimal(320000),
      cashBalance: new Decimal(20000),
    },
    {
      portfolioId: 'PORT0006',
      accountType: 'CO',
      branchId: '03',
      clientId: '0000000006',
      portfolioName: 'Balanced Fund',
      currencyCode: 'EUR',
      riskLevel: '3',
      status: 'A',
      totalValue: new Decimal(450000),
      cashBalance: new Decimal(30000),
    },
    {
      portfolioId: 'PORT0007',
      accountType: 'IN',
      branchId: '01',
      clientId: '0000000007',
      portfolioName: 'Retired Portfolio',
      currencyCode: 'USD',
      riskLevel: '2',
      status: 'C',
      totalValue: new Decimal(0),
      cashBalance: new Decimal(0),
    },
    {
      portfolioId: 'PORT0008',
      accountType: 'TR',
      branchId: '02',
      clientId: '0000000008',
      portfolioName: 'International Equity',
      currencyCode: 'GBP',
      riskLevel: '4',
      status: 'S',
      totalValue: new Decimal(280000),
      cashBalance: new Decimal(12000),
    },
  ];

  const now = new Date();

  for (const p of portfolioData) {
    await prisma.portfolio.upsert({
      where: { portfolioId: p.portfolioId },
      update: {},
      create: {
        ...p,
        openDate: new Date(2024, 0, 15),
        closeDate: p.status === 'C' ? new Date(2025, 5, 30) : null,
        lastMaintDate: now,
        lastMaintUser: 'SEED0000',
      },
    });
  }

  console.log(`Created ${portfolioData.length} portfolios`);

  // Create sample positions
  const positionData = [
    // PORT0001 positions
    { portfolioId: 'PORT0001', investmentId: 'AAPL      ', quantity: 100, costBasis: 15000, marketValue: 17500 },
    { portfolioId: 'PORT0001', investmentId: 'GOOGL     ', quantity: 50, costBasis: 70000, marketValue: 75000 },
    { portfolioId: 'PORT0001', investmentId: 'MSFT      ', quantity: 200, costBasis: 60000, marketValue: 82500 },
    { portfolioId: 'PORT0001', investmentId: 'AMZN      ', quantity: 75, costBasis: 65000, marketValue: 75000 },
    // PORT0002 positions
    { portfolioId: 'PORT0002', investmentId: 'AGG       ', quantity: 1000, costBasis: 100000, marketValue: 102000 },
    { portfolioId: 'PORT0002', investmentId: 'BND       ', quantity: 2000, costBasis: 200000, marketValue: 198000 },
    { portfolioId: 'PORT0002', investmentId: 'LQD       ', quantity: 1500, costBasis: 150000, marketValue: 153000 },
    { portfolioId: 'PORT0002', investmentId: 'TLT       ', quantity: 500, costBasis: 50000, marketValue: 47000 },
    // PORT0003 positions
    { portfolioId: 'PORT0003', investmentId: 'SPY       ', quantity: 500, costBasis: 200000, marketValue: 225000 },
    { portfolioId: 'PORT0003', investmentId: 'QQQ       ', quantity: 300, costBasis: 120000, marketValue: 135000 },
    { portfolioId: 'PORT0003', investmentId: 'VTI       ', quantity: 1000, costBasis: 200000, marketValue: 220000 },
    { portfolioId: 'PORT0003', investmentId: 'VXUS      ', quantity: 2000, costBasis: 100000, marketValue: 95000 },
    { portfolioId: 'PORT0003', investmentId: 'BND       ', quantity: 3000, costBasis: 300000, marketValue: 295000 },
    { portfolioId: 'PORT0003', investmentId: 'GLD       ', quantity: 150, costBasis: 30000, marketValue: 30000 },
    // PORT0004 positions
    { portfolioId: 'PORT0004', investmentId: 'SCHD      ', quantity: 500, costBasis: 35000, marketValue: 37500 },
    { portfolioId: 'PORT0004', investmentId: 'VYM       ', quantity: 400, costBasis: 40000, marketValue: 42000 },
    { portfolioId: 'PORT0004', investmentId: 'AGG       ', quantity: 1000, costBasis: 95000, marketValue: 95500 },
    // PORT0005 positions
    { portfolioId: 'PORT0005', investmentId: 'NVDA      ', quantity: 200, costBasis: 80000, marketValue: 120000 },
    { portfolioId: 'PORT0005', investmentId: 'META      ', quantity: 150, costBasis: 45000, marketValue: 60000 },
    { portfolioId: 'PORT0005', investmentId: 'TSLA      ', quantity: 100, costBasis: 25000, marketValue: 35000 },
    { portfolioId: 'PORT0005', investmentId: 'AMD       ', quantity: 300, costBasis: 45000, marketValue: 55000 },
    { portfolioId: 'PORT0005', investmentId: 'CRM       ', quantity: 100, costBasis: 25000, marketValue: 30000 },
    // PORT0006 positions
    { portfolioId: 'PORT0006', investmentId: 'VWRL      ', quantity: 1000, costBasis: 200000, marketValue: 210000 },
    { portfolioId: 'PORT0006', investmentId: 'IEMG      ', quantity: 500, costBasis: 50000, marketValue: 48000 },
    { portfolioId: 'PORT0006', investmentId: 'AGGG      ', quantity: 2000, costBasis: 200000, marketValue: 192000 },
  ];

  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  for (const pos of positionData) {
    await prisma.investmentPosition.upsert({
      where: {
        portfolioId_investmentId_positionDate: {
          portfolioId: pos.portfolioId,
          investmentId: pos.investmentId,
          positionDate: today,
        },
      },
      update: {},
      create: {
        portfolioId: pos.portfolioId,
        investmentId: pos.investmentId,
        positionDate: today,
        quantity: pos.quantity,
        costBasis: pos.costBasis,
        marketValue: pos.marketValue,
        currencyCode: 'USD',
        lastMaintDate: now,
        lastMaintUser: 'SEED0000',
      },
    });
  }

  console.log(`Created ${positionData.length} positions`);

  // Create sample transactions (TSTGEN00 TRANSACTN test type)
  const transactionData = [
    { portfolioId: 'PORT0001', investmentId: 'AAPL      ', type: 'BU', qty: 50, price: 150, daysAgo: 30, status: 'D' },
    { portfolioId: 'PORT0001', investmentId: 'AAPL      ', type: 'BU', qty: 50, price: 155, daysAgo: 15, status: 'D' },
    { portfolioId: 'PORT0001', investmentId: 'GOOGL     ', type: 'BU', qty: 50, price: 1400, daysAgo: 25, status: 'D' },
    { portfolioId: 'PORT0001', investmentId: 'MSFT      ', type: 'BU', qty: 200, price: 300, daysAgo: 20, status: 'D' },
    { portfolioId: 'PORT0002', investmentId: 'AGG       ', type: 'BU', qty: 1000, price: 100, daysAgo: 45, status: 'D' },
    { portfolioId: 'PORT0002', investmentId: 'BND       ', type: 'BU', qty: 2000, price: 100, daysAgo: 40, status: 'D' },
    { portfolioId: 'PORT0003', investmentId: 'SPY       ', type: 'BU', qty: 500, price: 400, daysAgo: 60, status: 'D' },
    { portfolioId: 'PORT0003', investmentId: 'QQQ       ', type: 'BU', qty: 300, price: 400, daysAgo: 55, status: 'D' },
    { portfolioId: 'PORT0005', investmentId: 'NVDA      ', type: 'BU', qty: 200, price: 400, daysAgo: 10, status: 'D' },
    { portfolioId: 'PORT0005', investmentId: 'META      ', type: 'BU', qty: 150, price: 300, daysAgo: 8, status: 'D' },
    // Pending transactions
    { portfolioId: 'PORT0001', investmentId: 'AAPL      ', type: 'SL', qty: 25, price: 180, daysAgo: 0, status: 'P' },
    { portfolioId: 'PORT0003', investmentId: 'GLD       ', type: 'BU', qty: 50, price: 200, daysAgo: 0, status: 'P' },
    { portfolioId: 'PORT0005', investmentId: 'AMD       ', type: 'BU', qty: 100, price: 185, daysAgo: 0, status: 'P' },
    { portfolioId: 'PORT0006', investmentId: 'VWRL      ', type: 'FE', qty: 1, price: 250, daysAgo: 0, status: 'P' },
  ];

  let txnSeq = 1;
  for (const txn of transactionData) {
    const txnDate = new Date(now);
    txnDate.setDate(txnDate.getDate() - txn.daysAgo);
    const datePart = txnDate.toISOString().replace(/[-T:Z.]/g, '').substring(0, 14);
    const transactionId = (datePart + txnSeq.toString().padStart(6, '0')).substring(0, 20);
    txnSeq++;

    await prisma.transaction.upsert({
      where: { transactionId },
      update: {},
      create: {
        transactionId,
        portfolioId: txn.portfolioId,
        transactionDate: txnDate,
        transactionTime: txnDate.toTimeString().substring(0, 8),
        investmentId: txn.investmentId,
        transactionType: txn.type,
        quantity: txn.qty,
        price: txn.price,
        amount: txn.qty * txn.price,
        currencyCode: 'USD',
        status: txn.status,
        processDate: txn.status === 'D' ? txnDate : now,
        processUser: txn.status === 'D' ? 'BATCH000' : 'SEED0000',
      },
    });
  }

  console.log(`Created ${transactionData.length} transactions`);

  // Create a completed batch job record
  await prisma.batchJob.upsert({
    where: {
      jobName_processDate_sequenceNo: {
        jobName: 'NIGHTLY',
        processDate: today,
        sequenceNo: 1,
      },
    },
    update: {},
    create: {
      jobName: 'NIGHTLY',
      processDate: today,
      sequenceNo: 1,
      status: 'D',
      stepName: 'COMPLETE',
      programName: 'BCHCTL00',
      startTime: new Date(now.getTime() - 3600000),
      endTime: new Date(now.getTime() - 3000000),
      returnCode: 0,
      recordsRead: 10,
      recordsWritten: 10,
      errorCount: 0,
    },
  });

  console.log('Created batch job record');
  console.log('Seeding complete!');
}

main()
  .catch((e) => {
    console.error('Seed error:', e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
