// Daily Position Report Generator - migrated from RPTPOS00.cbl
//
// RPTPOS00.cbl generates daily position reports by:
// 1. 1000-INITIALIZE: Opening POSITION-MASTER, TRANSACTION-HISTORY,
//    and REPORT-FILE; writing report headers with date.
// 2. 2000-PROCESS-REPORT:
//    - 2100-READ-POSITIONS: Sequential read of all position records
//    - 2110-FORMAT-POSITION: Format each position with portfolio ID,
//      description, quantity, current value, and change percentage
//      (COMPUTE (POS-CURRENT-VALUE - POS-PREVIOUS-VALUE) / POS-PREVIOUS-VALUE * 100)
//    - 2200-PROCESS-TRANSACTIONS: Read and summarize transaction activity
//    - 2300-WRITE-SUMMARY: Write totals, exceptions, and metrics
// 3. 3000-CLEANUP: Close all files
//
// Modern implementation outputs JSON (for UI) and optionally CSV.
// Uses Prisma queries instead of sequential VSAM reads.
// Preserves Decimal precision for financial calculations.

import Decimal from "decimal.js";
import { prisma } from "../../lib/db";
import type {
  PositionReport,
  PositionReportEntry,
  PositionReportSummary,
} from "../../types";

export interface PositionReportParams {
  date?: Date;
  portfolioId?: string;
  format?: "json" | "csv";
}

/**
 * Generate daily position report, mirroring RPTPOS00 2000-PROCESS-REPORT.
 *
 * Groups positions by portfolio, calculates subtotals per portfolio
 * and grand totals across all portfolios — matching the COBOL report's
 * 2300-WRITE-SUMMARY / 2310-WRITE-TOTALS paragraphs.
 */
export async function generatePositionReport(
  params: PositionReportParams = {}
): Promise<PositionReport> {
  const reportDate = params.date ?? new Date();
  const dateStr = reportDate.toISOString().split("T")[0];

  // 2100-READ-POSITIONS: Read all positions, optionally filtered by portfolio
  const positions = await prisma.investmentPosition.findMany({
    where: {
      ...(params.portfolioId ? { portfolioId: params.portfolioId } : {}),
      positionDate: { lte: reportDate },
    },
    include: {
      portfolio: true,
    },
    orderBy: [{ portfolioId: "asc" }, { investmentId: "asc" }],
  });

  // Group by portfolio — mirrors sequential processing with control breaks
  const portfolioGroups = new Map<string, typeof positions>();
  for (const pos of positions) {
    const group = portfolioGroups.get(pos.portfolioId) ?? [];
    group.push(pos);
    portfolioGroups.set(pos.portfolioId, group);
  }

  let grandTotalCostBasis = new Decimal(0);
  let grandTotalMarketValue = new Decimal(0);

  const portfolios: PositionReportSummary[] = [];

  for (const [portfolioId, portPositions] of portfolioGroups) {
    let totalCostBasis = new Decimal(0);
    let totalMarketValue = new Decimal(0);

    const entries: PositionReportEntry[] = portPositions.map((pos) => {
      // 2110-FORMAT-POSITION: Format each position record
      const costBasis = new Decimal(pos.costBasis.toString());
      const marketValue = new Decimal(pos.marketValue.toString());
      const gainLoss = marketValue.minus(costBasis);
      // COMPUTE WS-POS-CHANGE-PCT = (CURRENT - PREVIOUS) / PREVIOUS * 100
      const gainLossPct = costBasis.isZero()
        ? new Decimal(0)
        : gainLoss.dividedBy(costBasis).times(100);

      totalCostBasis = totalCostBasis.plus(costBasis);
      totalMarketValue = totalMarketValue.plus(marketValue);

      return {
        portfolioId: pos.portfolioId.trim(),
        portfolioName: pos.portfolio.portfolioName.trim(),
        investmentId: pos.investmentId.trim(),
        quantity: new Decimal(pos.quantity.toString()).toFixed(4),
        costBasis: costBasis.toFixed(2),
        marketValue: marketValue.toFixed(2),
        gainLoss: gainLoss.toFixed(2),
        gainLossPct: gainLossPct.toFixed(2),
        currencyCode: pos.currencyCode.trim(),
      };
    });

    const totalGainLoss = totalMarketValue.minus(totalCostBasis);
    const totalGainLossPct = totalCostBasis.isZero()
      ? new Decimal(0)
      : totalGainLoss.dividedBy(totalCostBasis).times(100);

    portfolios.push({
      reportDate: dateStr,
      portfolioId: portfolioId.trim(),
      portfolioName: portPositions[0]?.portfolio.portfolioName.trim() ?? "",
      totalCostBasis: totalCostBasis.toFixed(2),
      totalMarketValue: totalMarketValue.toFixed(2),
      totalGainLoss: totalGainLoss.toFixed(2),
      totalGainLossPct: totalGainLossPct.toFixed(2),
      currencyCode: portPositions[0]?.currencyCode.trim() ?? "USD",
      positions: entries,
    });

    grandTotalCostBasis = grandTotalCostBasis.plus(totalCostBasis);
    grandTotalMarketValue = grandTotalMarketValue.plus(totalMarketValue);
  }

  const grandTotalGainLoss = grandTotalMarketValue.minus(grandTotalCostBasis);

  return {
    reportDate: dateStr,
    generatedAt: new Date().toISOString(),
    portfolios,
    grandTotalCostBasis: grandTotalCostBasis.toFixed(2),
    grandTotalMarketValue: grandTotalMarketValue.toFixed(2),
    grandTotalGainLoss: grandTotalGainLoss.toFixed(2),
  };
}

/**
 * Convert position report to CSV format.
 * Mirrors the fixed-width REPORT-FILE output from RPTPOS00 but uses CSV.
 */
export function positionReportToCsv(report: PositionReport): string {
  const lines: string[] = [];

  // Header — mirrors WS-HEADER1, WS-HEADER2, WS-HEADER3
  lines.push("DAILY POSITION REPORT");
  lines.push(`Report Date: ${report.reportDate}`);
  lines.push(`Generated: ${report.generatedAt}`);
  lines.push("");
  lines.push(
    "Portfolio ID,Portfolio Name,Investment ID,Quantity,Cost Basis,Market Value,Gain/Loss,Gain/Loss %,Currency"
  );

  for (const portfolio of report.portfolios) {
    for (const entry of portfolio.positions) {
      lines.push(
        [
          entry.portfolioId,
          `"${entry.portfolioName}"`,
          entry.investmentId,
          entry.quantity,
          entry.costBasis,
          entry.marketValue,
          entry.gainLoss,
          entry.gainLossPct,
          entry.currencyCode,
        ].join(",")
      );
    }
    // Subtotal line — mirrors 2310-WRITE-TOTALS
    lines.push(
      `,${portfolio.portfolioName} SUBTOTAL,,,${portfolio.totalCostBasis},${portfolio.totalMarketValue},${portfolio.totalGainLoss},${portfolio.totalGainLossPct},${portfolio.currencyCode}`
    );
    lines.push("");
  }

  // Grand total — mirrors end of 2300-WRITE-SUMMARY
  lines.push(
    `,GRAND TOTAL,,,${report.grandTotalCostBasis},${report.grandTotalMarketValue},${report.grandTotalGainLoss},,`
  );

  return lines.join("\n");
}
