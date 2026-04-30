/**
 * Prisma Seed Script
 * Generates test data matching the logic in src/programs/test/TSTGEN00.cbl
 *
 * TSTGEN00 test data generation categories:
 *   - PORTFOLIO  : Sample portfolio master records
 *   - TRANSACTN  : Transaction test scenarios
 *   - ERROR      : Error condition data (data errors + process errors)
 *   - VOLUME     : Large-scale portfolio & transaction sets
 *
 * This seed creates a representative dataset covering all four categories.
 */

import { PrismaClient, Prisma } from "@prisma/client";

const prisma = new PrismaClient();

// ---------------------------------------------------------------------------
// Helpers — mirrors WS-RANDOM-VALUES logic from TSTGEN00
// ---------------------------------------------------------------------------

let randomSeed = 123456789;

function nextRandom(): number {
  randomSeed = (randomSeed * 1103515245 + 12345) & 0x7fffffff;
  return randomSeed;
}

function randomDecimal(max: number, decimals: number = 2): number {
  const val = (nextRandom() / 0x7fffffff) * max;
  return parseFloat(val.toFixed(decimals));
}

function pick<T>(arr: readonly T[]): T {
  return arr[nextRandom() % arr.length];
}

function padLeft(value: number | string, length: number, char = "0"): string {
  return String(value).padStart(length, char);
}

function formatDate(d: Date): string {
  return [
    d.getFullYear(),
    padLeft(d.getMonth() + 1, 2),
    padLeft(d.getDate(), 2),
  ].join("");
}

function formatTime(d: Date): string {
  return [
    padLeft(d.getHours(), 2),
    padLeft(d.getMinutes(), 2),
    padLeft(d.getSeconds(), 2),
  ].join("");
}


// ---------------------------------------------------------------------------
// Constants — matching WS-TEST-TYPES and WS-PORTFOLIO-DATA patterns
// ---------------------------------------------------------------------------

const PORTFOLIO_STATUSES = ["A", "A", "A", "C", "S"] as const;
const ACCOUNT_TYPES = ["01", "02", "03"] as const;
const BRANCH_IDS = ["01", "02", "03", "04", "05"] as const;
const CURRENCIES = ["USD", "EUR", "GBP", "JPY", "CAD"] as const;
const RISK_LEVELS = ["1", "2", "3", "4", "5"] as const;

const TRANSACTION_TYPES = ["BU", "SL", "TR", "FE"] as const;
const TRANSACTION_STATUSES = ["P", "D", "D", "D", "F", "R"] as const;

const ERROR_TYPES = ["S", "A", "D"] as const;
const ERROR_SEVERITIES = [1, 2, 3, 4] as const;

const INVESTMENTS = [
  "AAPL000001",
  "MSFT000002",
  "GOOGL00003",
  "AMZN000004",
  "TSLA000005",
  "JPM0000006",
  "BAC0000007",
  "WFC0000008",
  "GS00000009",
  "MS00000010",
] as const;

const CLIENT_NAMES = [
  "SMITH INVESTMENT TRUST",
  "DOE FAMILY PORTFOLIO",
  "ACME CORPORATION",
  "GLOBEX HOLDINGS INC",
  "STERLING CAPITAL MGMT",
  "MAPLE LEAF FUND",
  "PACIFIC RIM PARTNERS",
  "ATLANTIC GROWTH FUND",
  "SUMMIT PEAK ADVISORS",
  "VALLEY VISTA CAPITAL",
];

const PROGRAM_IDS = [
  "PORTMSTR",
  "TRNVAL00",
  "INQONLN ",
  "CURSMGR ",
  "TSTGEN00",
];

const ERROR_MESSAGES = [
  "INVALID PORTFOLIO ID FORMAT",
  "DUPLICATE TRANSACTION DETECTED",
  "INSUFFICIENT FUNDS FOR TRANSACTION",
  "CURRENCY MISMATCH IN PORTFOLIO",
  "MARKET DATA UNAVAILABLE",
  "DB2 SQLCODE -805 PLAN NOT FOUND",
  "VSAM FILE STATUS 23 - NOT FOUND",
  "DECIMAL OVERFLOW IN CALCULATION",
  "INVALID DATE RANGE SPECIFIED",
  "UNAUTHORIZED ACCESS ATTEMPT",
];

// ---------------------------------------------------------------------------
// 2200-GEN-PORTFOLIO — Generate portfolio test data
// ---------------------------------------------------------------------------

async function genPortfolios(count: number) {
  console.log(`Generating ${count} portfolio records...`);
  const portfolios: Prisma.PortfolioCreateInput[] = [];

  for (let i = 0; i < count; i++) {
    const id = `PORT${padLeft(i + 1, 4)}`;
    const status = pick(PORTFOLIO_STATUSES);
    const openDate = new Date(2023, nextRandom() % 12, (nextRandom() % 28) + 1);
    const closeDate = status === "C"
      ? new Date(2024, nextRandom() % 12, (nextRandom() % 28) + 1)
      : null;

    portfolios.push({
      portfolioId: id,
      accountType: pick(ACCOUNT_TYPES),
      branchId: pick(BRANCH_IDS),
      clientId: padLeft(i + 1, 10),
      portfolioName: pick(CLIENT_NAMES),
      currencyCode: pick(CURRENCIES),
      riskLevel: pick(RISK_LEVELS),
      status,
      openDate,
      closeDate,
      lastMaintDate: new Date(),
      lastMaintUser: "SEEDGEN ",
    });
  }

  for (const p of portfolios) {
    await prisma.portfolio.create({ data: p });
  }
  console.log(`  ✓ ${portfolios.length} portfolios created`);
  return portfolios.map((p) => p.portfolioId);
}

// ---------------------------------------------------------------------------
// Generate investment positions for each portfolio
// ---------------------------------------------------------------------------

async function genPositions(portfolioIds: string[]) {
  console.log("Generating investment positions...");
  let count = 0;

  for (const portId of portfolioIds) {
    const numPositions = (nextRandom() % 5) + 1;
    for (let j = 0; j < numPositions; j++) {
      const posDate = new Date(2024, nextRandom() % 12, (nextRandom() % 28) + 1);
      const qty = randomDecimal(10000, 4);
      const costBasis = randomDecimal(500000, 2);
      const marketValue = costBasis * (0.8 + randomDecimal(0.4, 4));

      await prisma.investmentPosition.create({
        data: {
          portfolioId: portId,
          investmentId: pick(INVESTMENTS),
          positionDate: posDate,
          quantity: new Prisma.Decimal(qty),
          costBasis: new Prisma.Decimal(costBasis),
          marketValue: new Prisma.Decimal(parseFloat(marketValue.toFixed(2))),
          currencyCode: pick(CURRENCIES),
          lastMaintDate: new Date(),
          lastMaintUser: "SEEDGEN ",
        },
      });
      count++;
    }
  }
  console.log(`  ✓ ${count} positions created`);
}

// ---------------------------------------------------------------------------
// 2300-GEN-TRANSACTION — Generate transaction test scenarios
// ---------------------------------------------------------------------------

async function genTransactions(portfolioIds: string[], count: number) {
  console.log(`Generating ${count} transaction records...`);
  let created = 0;

  for (let i = 0; i < count; i++) {
    const portId = pick(portfolioIds);
    const trnDate = new Date(2024, nextRandom() % 12, (nextRandom() % 28) + 1);
    const trnTime = new Date(0, 0, 0, nextRandom() % 24, nextRandom() % 60, nextRandom() % 60);
    const trnType = pick(TRANSACTION_TYPES);
    const qty = randomDecimal(1000, 4);
    const price = randomDecimal(500, 4);
    const amount = parseFloat((qty * price).toFixed(2));
    const dateStr = formatDate(trnDate);
    const timeStr = formatTime(trnTime);

    const transactionId = `${dateStr}${timeStr}${padLeft(i + 1, 6)}`;

    await prisma.transactionHistory.create({
      data: {
        transactionId,
        portfolioId: portId,
        transactionDate: trnDate,
        transactionTime: trnTime,
        investmentId: pick(INVESTMENTS),
        transactionType: trnType,
        quantity: new Prisma.Decimal(qty),
        price: new Prisma.Decimal(price),
        amount: new Prisma.Decimal(amount),
        currencyCode: pick(CURRENCIES),
        status: pick(TRANSACTION_STATUSES),
        processDate: new Date(),
        processUser: "SEEDGEN ",
      },
    });
    created++;
  }
  console.log(`  ✓ ${created} transactions created`);
}

// ---------------------------------------------------------------------------
// Generate position history records
// ---------------------------------------------------------------------------

async function genPositionHistory(portfolioIds: string[]) {
  console.log("Generating position history...");
  let count = 0;

  for (const portId of portfolioIds.slice(0, 5)) {
    const numRecords = (nextRandom() % 3) + 1;
    for (let j = 0; j < numRecords; j++) {
      const tDate = new Date(2024, nextRandom() % 12, (nextRandom() % 28) + 1);
      const tTime = new Date(0, 0, 0, nextRandom() % 24, nextRandom() % 60, nextRandom() % 60);
      const qty = randomDecimal(1000, 3);
      const price = randomDecimal(500, 3);
      const amount = parseFloat((qty * price).toFixed(2));
      const fees = randomDecimal(50, 2);
      const totalAmount = amount + fees;
      const costBasis = randomDecimal(200000, 2);
      const gainLoss = parseFloat((amount - costBasis).toFixed(2));

      await prisma.positionHistory.create({
        data: {
          accountNo: padLeft(portId.replace("PORT", ""), 8),
          portfolioId: padLeft(portId, 10),
          transDate: tDate,
          transTime: tTime,
          transType: pick(["BU", "SL", "TR"] as const),
          securityId: padLeft(pick(INVESTMENTS), 12),
          quantity: new Prisma.Decimal(qty),
          price: new Prisma.Decimal(price),
          amount: new Prisma.Decimal(amount),
          fees: new Prisma.Decimal(fees),
          totalAmount: new Prisma.Decimal(totalAmount),
          costBasis: new Prisma.Decimal(costBasis),
          gainLoss: new Prisma.Decimal(gainLoss),
          processDate: new Date(),
          processTime: new Date(),
          programId: pick(PROGRAM_IDS),
          userId: "SEEDGEN ",
        },
      });
      count++;
    }
  }
  console.log(`  ✓ ${count} position history records created`);
}

// ---------------------------------------------------------------------------
// 2400-GEN-ERROR-DATA — Generate error scenarios
// ---------------------------------------------------------------------------

async function genErrorData() {
  console.log("Generating error test data...");
  let count = 0;

  // 2410-GEN-DATA-ERRORS: Data-related errors
  for (let i = 0; i < 10; i++) {
    const ts = new Date(2024, nextRandom() % 12, (nextRandom() % 28) + 1,
      nextRandom() % 24, nextRandom() % 60, nextRandom() % 60, i);

    await prisma.errorLog.create({
      data: {
        errorTimestamp: ts,
        programId: pick(PROGRAM_IDS),
        errorType: pick(ERROR_TYPES),
        errorSeverity: pick(ERROR_SEVERITIES),
        errorCode: `ERR${padLeft(i + 1, 5)}`,
        errorMessage: pick(ERROR_MESSAGES),
        processDate: new Date(),
        processTime: new Date(),
        userId: "SEEDGEN ",
        additionalInfo: i % 3 === 0 ? `Additional context for error scenario ${i + 1}` : null,
      },
    });
    count++;
  }

  // 2420-GEN-PROCESS-ERRORS: Process-related errors
  for (let i = 0; i < 5; i++) {
    const ts = new Date(2024, nextRandom() % 12, (nextRandom() % 28) + 1,
      nextRandom() % 24, nextRandom() % 60, nextRandom() % 60, 100 + i);

    await prisma.errorLog.create({
      data: {
        errorTimestamp: ts,
        programId: pick(PROGRAM_IDS),
        errorType: "S",
        errorSeverity: pick([3, 4] as const),
        errorCode: `SYS${padLeft(i + 1, 5)}`,
        errorMessage: `SYSTEM ERROR IN BATCH PROCESS STEP ${i + 1}`,
        processDate: new Date(),
        processTime: new Date(),
        userId: "BATCH   ",
        additionalInfo: `Batch job step ${i + 1} failed during nightly processing`,
      },
    });
    count++;
  }

  console.log(`  ✓ ${count} error log entries created`);
}

// ---------------------------------------------------------------------------
// Generate audit log records
// ---------------------------------------------------------------------------

async function genAuditLogs(portfolioIds: string[]) {
  console.log("Generating audit log records...");
  let count = 0;

  const recordTypes = ["PT", "PS", "TR"] as const;
  const actionCodes = ["A", "C", "D"] as const;

  for (const portId of portfolioIds.slice(0, 8)) {
    const numLogs = (nextRandom() % 4) + 1;
    for (let j = 0; j < numLogs; j++) {
      const d = new Date(2024, nextRandom() % 12, (nextRandom() % 28) + 1,
        nextRandom() % 24, nextRandom() % 60, nextRandom() % 60);
      const recType = pick(recordTypes);
      const action = pick(actionCodes);

      await prisma.auditLog.create({
        data: {
          portfolioId: portId,
          histDate: formatDate(d),
          histTime: formatTime(d),
          seqNo: padLeft(j + 1, 4),
          recordType: recType,
          actionCode: action,
          beforeImage: action !== "A" ? `{"status":"A","value":${randomDecimal(100000)}}` : null,
          afterImage: action !== "D" ? `{"status":"${pick(["A", "C", "S"])}","value":${randomDecimal(100000)}}` : null,
          reasonCode: pick(["MNTC", "CORR", "ADJS", "CLOS"]),
          processDate: d,
          processUser: pick(PROGRAM_IDS),
        },
      });
      count++;
    }
  }
  console.log(`  ✓ ${count} audit log entries created`);
}

// ---------------------------------------------------------------------------
// Main — orchestrates all generation (mirrors 0000-MAIN → 2000-PROCESS)
// ---------------------------------------------------------------------------

async function main() {
  console.log("=== TSTGEN00 Seed — Investment Portfolio Test Data ===\n");

  // PORTFOLIO test type (2200-GEN-PORTFOLIO)
  const portfolioIds = await genPortfolios(10);

  // Generate positions for portfolios
  await genPositions(portfolioIds);

  // TRANSACTN test type (2300-GEN-TRANSACTION)
  await genTransactions(portfolioIds, 25);

  // Position history
  await genPositionHistory(portfolioIds);

  // ERROR test type (2400-GEN-ERROR-DATA)
  await genErrorData();

  // Audit log records
  await genAuditLogs(portfolioIds);

  // VOLUME test type (2500-GEN-VOLUME-DATA) — smaller scale for seed
  console.log("\nGenerating volume test data (scaled down)...");
  const volumePortIds = await genPortfolios(5);
  await genTransactions(volumePortIds, 50);

  console.log("\n=== Seed complete ===");
}

main()
  .then(async () => {
    await prisma.$disconnect();
  })
  .catch(async (e) => {
    console.error(e);
    await prisma.$disconnect();
    process.exit(1);
  });
