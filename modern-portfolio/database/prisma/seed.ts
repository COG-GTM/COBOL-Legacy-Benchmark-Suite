// Database seed script (replaces TSTGEN00 from src/programs/test/TSTGEN00.cbl)
import { PrismaClient } from '@prisma/client';
import bcrypt from 'bcryptjs';

const prisma = new PrismaClient();

async function main() {
  console.log('Seeding database...');

  // Create users
  const passwordHash = await bcrypt.hash('password123', 10);
  const users = await Promise.all([
    prisma.user.upsert({
      where: { username: 'admin' },
      update: {},
      create: { username: 'admin', email: 'admin@portfolio.dev', passwordHash, role: 'ADMIN' },
    }),
    prisma.user.upsert({
      where: { username: 'trader' },
      update: {},
      create: { username: 'trader', email: 'trader@portfolio.dev', passwordHash, role: 'UPDATE' },
    }),
    prisma.user.upsert({
      where: { username: 'viewer' },
      update: {},
      create: { username: 'viewer', email: 'viewer@portfolio.dev', passwordHash, role: 'READ' },
    }),
  ]);
  console.log(`Created ${users.length} users`);

  // Create portfolios
  const portfolios = await Promise.all([
    prisma.portfolio.upsert({
      where: { portfolioId: 'PORT10001' },
      update: {},
      create: {
        portfolioId: 'PORT10001',
        accountNo: '1000100000',
        clientName: 'Acme Corporation',
        clientType: 'CORPORATE',
        status: 'ACTIVE',
        totalValue: 1250000.00,
        cashBalance: 50000.00,
        currencyCode: 'USD',
        riskLevel: 'M',
        branchId: '01',
        lastUser: 'SEED',
      },
    }),
    prisma.portfolio.upsert({
      where: { portfolioId: 'PORT10002' },
      update: {},
      create: {
        portfolioId: 'PORT10002',
        accountNo: '1000200000',
        clientName: 'Jane Smith',
        clientType: 'INDIVIDUAL',
        status: 'ACTIVE',
        totalValue: 450000.00,
        cashBalance: 25000.00,
        currencyCode: 'USD',
        riskLevel: 'H',
        branchId: '02',
        lastUser: 'SEED',
      },
    }),
    prisma.portfolio.upsert({
      where: { portfolioId: 'PORT10003' },
      update: {},
      create: {
        portfolioId: 'PORT10003',
        accountNo: '1000300000',
        clientName: 'Smith Family Trust',
        clientType: 'TRUST',
        status: 'ACTIVE',
        totalValue: 2100000.00,
        cashBalance: 100000.00,
        currencyCode: 'USD',
        riskLevel: 'L',
        branchId: '01',
        lastUser: 'SEED',
      },
    }),
    prisma.portfolio.upsert({
      where: { portfolioId: 'PORT10004' },
      update: {},
      create: {
        portfolioId: 'PORT10004',
        accountNo: '1000400000',
        clientName: 'TechVentures Inc',
        clientType: 'CORPORATE',
        status: 'SUSPENDED',
        totalValue: 75000.00,
        cashBalance: 5000.00,
        currencyCode: 'USD',
        riskLevel: 'H',
        branchId: '03',
        lastUser: 'SEED',
      },
    }),
    prisma.portfolio.upsert({
      where: { portfolioId: 'PORT10005' },
      update: {},
      create: {
        portfolioId: 'PORT10005',
        accountNo: '1000500000',
        clientName: 'Robert Johnson',
        clientType: 'INDIVIDUAL',
        status: 'CLOSED',
        totalValue: 0,
        cashBalance: 0,
        currencyCode: 'USD',
        riskLevel: 'M',
        branchId: '02',
        closeDate: new Date('2024-06-15'),
        lastUser: 'SEED',
      },
    }),
  ]);
  console.log(`Created ${portfolios.length} portfolios`);

  // Create positions for active portfolios
  const investments = [
    { id: 'AAPL', name: 'Apple Inc', price: 185.50 },
    { id: 'MSFT', name: 'Microsoft Corp', price: 420.30 },
    { id: 'GOOGL', name: 'Alphabet Inc', price: 175.20 },
    { id: 'AMZN', name: 'Amazon.com Inc', price: 195.80 },
    { id: 'TSLA', name: 'Tesla Inc', price: 245.60 },
    { id: 'BND', name: 'Vanguard Bond ETF', price: 72.45 },
    { id: 'VTI', name: 'Vanguard Total Mkt', price: 265.30 },
    { id: 'SPY', name: 'S&P 500 ETF', price: 580.90 },
  ];

  const positionData = [
    { portfolio: portfolios[0], investments: [0, 1, 2, 5, 7], quantities: [500, 200, 300, 1000, 150] },
    { portfolio: portfolios[1], investments: [0, 3, 4, 6], quantities: [100, 50, 200, 300] },
    { portfolio: portfolios[2], investments: [1, 2, 5, 6, 7], quantities: [400, 250, 2000, 500, 200] },
  ];

  let posCount = 0;
  for (const pd of positionData) {
    for (let i = 0; i < pd.investments.length; i++) {
      const inv = investments[pd.investments[i]];
      const qty = pd.quantities[i];
      const costBasis = qty * inv.price * 0.92;
      const marketValue = qty * inv.price;

      await prisma.position.create({
        data: {
          portfolioId: pd.portfolio.id,
          investmentId: inv.id,
          positionDate: new Date(),
          quantity: qty,
          costBasis,
          marketValue,
          currency: 'USD',
          status: 'ACTIVE',
          lastUser: 'SEED',
        },
      });
      posCount++;
    }
  }
  console.log(`Created ${posCount} positions`);

  // Create transactions
  const transactionTypes = ['BUY', 'SELL', 'BUY', 'BUY', 'FEE', 'BUY', 'SELL', 'TRANSFER'] as const;
  const statuses = ['DONE', 'DONE', 'DONE', 'PENDING', 'DONE', 'DONE', 'PENDING', 'DONE'] as const;

  let txnCount = 0;
  for (const portfolio of portfolios.slice(0, 3)) {
    for (let i = 0; i < 8; i++) {
      const daysAgo = Math.floor(Math.random() * 30);
      const date = new Date();
      date.setDate(date.getDate() - daysAgo);
      const inv = investments[Math.floor(Math.random() * investments.length)];
      const qty = Math.floor(Math.random() * 100) + 10;
      const price = inv.price * (0.95 + Math.random() * 0.1);
      const amount = qty * price;
      const type = transactionTypes[i];
      const status = statuses[i];

      await prisma.transaction.create({
        data: {
          transactionId: `TXN${Date.now()}${String(txnCount).padStart(6, '0')}`,
          portfolioId: portfolio.id,
          transactionDate: date,
          transactionTime: date.toTimeString().substring(0, 8),
          investmentId: inv.id,
          type,
          quantity: qty,
          price,
          amount,
          currency: 'USD',
          status,
          processedAt: status === 'DONE' ? date : null,
          processUser: 'SEED',
        },
      });
      txnCount++;
    }
  }
  console.log(`Created ${txnCount} transactions`);

  // Create audit log entries
  const auditEntries = [
    { portfolioId: portfolios[0].id, recordType: 'PORTFOLIO' as const, action: 'ADD' as const, message: 'Portfolio PORT10001 created' },
    { portfolioId: portfolios[1].id, recordType: 'PORTFOLIO' as const, action: 'ADD' as const, message: 'Portfolio PORT10002 created' },
    { portfolioId: portfolios[2].id, recordType: 'PORTFOLIO' as const, action: 'ADD' as const, message: 'Portfolio PORT10003 created' },
    { portfolioId: portfolios[0].id, recordType: 'POSITION' as const, action: 'ADD' as const, message: 'Positions added to PORT10001' },
    { portfolioId: portfolios[4].id, recordType: 'PORTFOLIO' as const, action: 'CHANGE' as const, message: 'Portfolio PORT10005 closed' },
  ];

  for (const entry of auditEntries) {
    await prisma.auditLog.create({
      data: { ...entry, userId: 'SEED', programId: 'TSTGEN00' },
    });
  }
  console.log(`Created ${auditEntries.length} audit log entries`);

  console.log('Seeding complete!');
}

main()
  .catch((e) => {
    console.error(e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
