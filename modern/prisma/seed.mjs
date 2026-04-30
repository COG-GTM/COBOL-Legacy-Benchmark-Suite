import Database from "better-sqlite3";
import { randomUUID } from "crypto";
import path from "path";
import { fileURLToPath } from "url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const dbPath = path.join(__dirname, "..", "dev.db");
const db = new Database(dbPath);

function cuid() {
  return randomUUID().replace(/-/g, "").slice(0, 25);
}

function now() {
  return new Date().toISOString();
}

function randomDate(start, end) {
  return new Date(start.getTime() + Math.random() * (end.getTime() - start.getTime())).toISOString();
}

console.log("Seeding database...");

db.exec('DELETE FROM "AuditLog"');
db.exec('DELETE FROM "Transaction"');
db.exec('DELETE FROM "Position"');
db.exec('DELETE FROM "BatchRun"');
db.exec('DELETE FROM "Portfolio"');

const portfolios = [
  { accountNo: "1000000001", clientName: "Acme Corp Retirement Fund", clientType: "C", status: "A", totalValue: 1250000.0, cashBalance: 45000.0 },
  { accountNo: "1000000002", clientName: "Jane Smith IRA", clientType: "I", status: "A", totalValue: 325000.0, cashBalance: 12500.0 },
  { accountNo: "1000000003", clientName: "Smith Family Trust", clientType: "T", status: "A", totalValue: 875000.0, cashBalance: 32000.0 },
  { accountNo: "1000000004", clientName: "Tech Ventures LLC", clientType: "C", status: "A", totalValue: 2100000.0, cashBalance: 95000.0 },
  { accountNo: "1000000005", clientName: "Robert Johnson 401k", clientType: "I", status: "S", totalValue: 158000.0, cashBalance: 5200.0 },
  { accountNo: "1000000006", clientName: "Green Energy Fund", clientType: "C", status: "A", totalValue: 3450000.0, cashBalance: 120000.0 },
  { accountNo: "1000000007", clientName: "Maria Garcia Brokerage", clientType: "I", status: "A", totalValue: 92000.0, cashBalance: 3400.0 },
  { accountNo: "1000000008", clientName: "Pacific Trust Holdings", clientType: "T", status: "C", totalValue: 0.0, cashBalance: 0.0 },
];

const positionData = {
  "1000000001": [
    { fundId: "AAPL", fundName: "Apple Inc.", units: 500, costBasis: 75000, marketValue: 95000 },
    { fundId: "MSFT", fundName: "Microsoft Corp.", units: 300, costBasis: 90000, marketValue: 120000 },
    { fundId: "GOOGL", fundName: "Alphabet Inc.", units: 200, costBasis: 280000, marketValue: 340000 },
    { fundId: "VBTLX", fundName: "Vanguard Total Bond", units: 1500, costBasis: 150000, marketValue: 155000 },
    { fundId: "SPY", fundName: "SPDR S&P 500 ETF", units: 800, costBasis: 360000, marketValue: 540000 },
  ],
  "1000000002": [
    { fundId: "VTI", fundName: "Vanguard Total Stock", units: 400, costBasis: 80000, marketValue: 95000 },
    { fundId: "BND", fundName: "Vanguard Total Bond ETF", units: 600, costBasis: 48000, marketValue: 46500 },
    { fundId: "AMZN", fundName: "Amazon.com Inc.", units: 50, costBasis: 85000, marketValue: 92000 },
    { fundId: "TSLA", fundName: "Tesla Inc.", units: 100, costBasis: 25000, marketValue: 28000 },
  ],
  "1000000003": [
    { fundId: "SPY", fundName: "SPDR S&P 500 ETF", units: 600, costBasis: 270000, marketValue: 405000 },
    { fundId: "QQQ", fundName: "Invesco QQQ Trust", units: 300, costBasis: 120000, marketValue: 145000 },
    { fundId: "VBTLX", fundName: "Vanguard Total Bond", units: 2000, costBasis: 200000, marketValue: 193000 },
  ],
  "1000000004": [
    { fundId: "NVDA", fundName: "NVIDIA Corp.", units: 400, costBasis: 120000, marketValue: 480000 },
    { fundId: "META", fundName: "Meta Platforms Inc.", units: 350, costBasis: 105000, marketValue: 175000 },
    { fundId: "NFLX", fundName: "Netflix Inc.", units: 200, costBasis: 100000, marketValue: 130000 },
    { fundId: "AAPL", fundName: "Apple Inc.", units: 1000, costBasis: 150000, marketValue: 190000 },
    { fundId: "MSFT", fundName: "Microsoft Corp.", units: 600, costBasis: 180000, marketValue: 240000 },
    { fundId: "GOOGL", fundName: "Alphabet Inc.", units: 500, costBasis: 700000, marketValue: 885000 },
  ],
  "1000000006": [
    { fundId: "ICLN", fundName: "iShares Global Clean Energy", units: 5000, costBasis: 100000, marketValue: 115000 },
    { fundId: "TAN", fundName: "Invesco Solar ETF", units: 3000, costBasis: 150000, marketValue: 138000 },
    { fundId: "QCLN", fundName: "First Trust NASDAQ Clean Edge", units: 2000, costBasis: 80000, marketValue: 92000 },
    { fundId: "ENPH", fundName: "Enphase Energy Inc.", units: 1500, costBasis: 225000, marketValue: 345000 },
    { fundId: "FSLR", fundName: "First Solar Inc.", units: 2000, costBasis: 400000, marketValue: 480000 },
    { fundId: "BND", fundName: "Vanguard Total Bond ETF", units: 15000, costBasis: 1200000, marketValue: 1160000 },
    { fundId: "SPY", fundName: "SPDR S&P 500 ETF", units: 1500, costBasis: 675000, marketValue: 1120000 },
  ],
  "1000000007": [
    { fundId: "VTI", fundName: "Vanguard Total Stock", units: 200, costBasis: 40000, marketValue: 47500 },
    { fundId: "BND", fundName: "Vanguard Total Bond ETF", units: 500, costBasis: 40000, marketValue: 41100 },
  ],
};

const transactionTypes = ["BUY", "SELL", "TRANSFER", "FEE"];
const investmentTypes = ["STK", "BND", "MMF", "ETF"];

const insertPortfolio = db.prepare(
  "INSERT INTO \"Portfolio\" (id, accountNo, clientName, clientType, status, totalValue, cashBalance, lastUser, lastTrans, createdAt, updatedAt) VALUES (?, ?, ?, ?, ?, ?, ?, 'SYSTEM', '', ?, ?)"
);
const insertPosition = db.prepare(
  "INSERT INTO \"Position\" (id, fundId, fundName, units, costBasis, marketValue, portfolioId) VALUES (?, ?, ?, ?, ?, ?, ?)"
);
const insertTransaction = db.prepare(
  "INSERT INTO \"Transaction\" (id, transactionType, investmentType, units, price, amount, sequenceNo, portfolioId, createdAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
);
const insertAudit = db.prepare(
  "INSERT INTO \"AuditLog\" (id, action, key, reason, status, portfolioId, createdAt) VALUES (?, ?, ?, ?, ?, ?, ?)"
);
const insertBatch = db.prepare(
  "INSERT INTO \"BatchRun\" (id, status, totalItems, processed, errors, startedAt, completedAt) VALUES (?, ?, ?, ?, ?, ?, ?)"
);

const seedAll = db.transaction(() => {
  for (const p of portfolios) {
    const pid = cuid();
    const ts = now();
    insertPortfolio.run(pid, p.accountNo, p.clientName, p.clientType, p.status, p.totalValue, p.cashBalance, ts, ts);

    const positions = positionData[p.accountNo] || [];
    for (const pos of positions) {
      insertPosition.run(cuid(), pos.fundId, pos.fundName, pos.units, pos.costBasis, pos.marketValue, pid);
    }

    const txCount = 5 + Math.floor(Math.random() * 15);
    for (let i = 0; i < txCount; i++) {
      const txType = transactionTypes[Math.floor(Math.random() * transactionTypes.length)];
      const invType = investmentTypes[Math.floor(Math.random() * investmentTypes.length)];
      const units = Math.round((10 + Math.random() * 500) * 100) / 100;
      const price = Math.round((5 + Math.random() * 500) * 100) / 100;
      const amount = Math.round(units * price * 100) / 100;
      const txDate = randomDate(new Date("2025-01-01"), new Date("2026-04-30"));
      insertTransaction.run(cuid(), txType, invType, units, price, amount, `SEQ${String(i + 1).padStart(3, "0")}`, pid, txDate);
    }

    insertAudit.run(cuid(), "CREATE", p.accountNo, "Portfolio created via seed", "SUCC", pid, ts);
  }

  insertBatch.run(cuid(), "COMPLETED", 150, 150, 0, "2026-04-28T02:00:00.000Z", "2026-04-28T02:15:00.000Z");
  insertBatch.run(cuid(), "COMPLETED", 200, 198, 2, "2026-04-29T02:00:00.000Z", "2026-04-29T02:22:00.000Z");
  insertBatch.run(cuid(), "FAILED", 180, 45, 5, "2026-04-30T02:00:00.000Z", "2026-04-30T02:05:00.000Z");
});

seedAll();
db.close();
console.log("Seed completed successfully.");
