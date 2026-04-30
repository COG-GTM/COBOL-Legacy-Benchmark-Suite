// Audit Report Generator - migrated from RPTAUD00.cbl
//
// RPTAUD00.cbl generates comprehensive audit reports by:
// 1. 1000-INITIALIZE: Opening AUDIT-FILE, ERROR-FILE, REPORT-FILE;
//    writing headers with report date.
// 2. 2000-PROCESS-REPORT:
//    - 2100-PROCESS-AUDIT-TRAIL:
//      - 2110-READ-AUDIT-RECORDS: Sequential read of audit log entries
//      - 2120-SUMMARIZE-AUDIT: Aggregate audit activity
//    - 2200-PROCESS-ERROR-LOG:
//      - 2210-READ-ERROR-RECORDS: Sequential read of error log entries
//      - 2220-SUMMARIZE-ERRORS: Aggregate error counts
//    - 2300-WRITE-SUMMARY:
//      - 2310-WRITE-AUDIT-SUMMARY: Audit trail counts by type/action
//      - 2320-WRITE-ERROR-SUMMARY: Error counts by severity/program
//      - 2330-WRITE-CONTROL-SUMMARY: Control verification totals
// 3. 3000-CLEANUP: Close all files
//
// Modern implementation: queries AuditLog table with filters,
// produces JSON output with summary aggregations.

import { prisma } from "../../lib/db";
import type { AuditReportFilters, AuditReportOutput } from "../../types";

/**
 * Generate audit trail report, mirroring RPTAUD00 2000-PROCESS-REPORT.
 *
 * Filters by date range, user, portfolio, and action type —
 * extending the COBOL version which read all records sequentially.
 */
export async function generateAuditReport(
  filters: AuditReportFilters = {}
): Promise<AuditReportOutput> {
  const reportDate = new Date().toISOString().split("T")[0];

  // Build filter conditions
  const where: Record<string, unknown> = {};

  if (filters.startDate || filters.endDate) {
    where.timestamp = {
      ...(filters.startDate ? { gte: filters.startDate } : {}),
      ...(filters.endDate ? { lte: filters.endDate } : {}),
    };
  }

  if (filters.userId) {
    where.userId = filters.userId.padEnd(8).substring(0, 8);
  }

  if (filters.portfolioId) {
    where.portfolioId = filters.portfolioId;
  }

  if (filters.action) {
    where.action = filters.action.padEnd(8).substring(0, 8);
  }

  // 2110-READ-AUDIT-RECORDS: Query audit log entries
  const entries = await prisma.auditLog.findMany({
    where,
    orderBy: { timestamp: "desc" },
    take: 10000,
  });

  // 2120-SUMMARIZE-AUDIT: Build summary aggregations
  const byAction: Record<string, number> = {};
  const byStatus: Record<string, number> = {};
  const byUser: Record<string, number> = {};

  const formattedEntries = entries.map((entry) => {
    const actionTrimmed = entry.action.trim();
    const statusTrimmed = entry.status.trim();
    const userTrimmed = entry.userId.trim();

    byAction[actionTrimmed] = (byAction[actionTrimmed] ?? 0) + 1;
    byStatus[statusTrimmed] = (byStatus[statusTrimmed] ?? 0) + 1;
    byUser[userTrimmed] = (byUser[userTrimmed] ?? 0) + 1;

    return {
      timestamp: entry.timestamp.toISOString(),
      userId: userTrimmed,
      programId: entry.programId.trim(),
      action: actionTrimmed,
      status: statusTrimmed,
      portfolioId: entry.portfolioId?.trim() ?? "",
      accountNo: entry.accountNo?.trim() ?? "",
      beforeImage: entry.beforeImage ?? "",
      afterImage: entry.afterImage ?? "",
      message: entry.message ?? "",
    };
  });

  // Build filter description for report metadata
  const appliedFilters: Record<string, string> = {};
  if (filters.startDate) appliedFilters.startDate = filters.startDate.toISOString();
  if (filters.endDate) appliedFilters.endDate = filters.endDate.toISOString();
  if (filters.userId) appliedFilters.userId = filters.userId;
  if (filters.portfolioId) appliedFilters.portfolioId = filters.portfolioId;
  if (filters.action) appliedFilters.action = filters.action;

  // 2300-WRITE-SUMMARY: Compile final report
  return {
    reportDate,
    generatedAt: new Date().toISOString(),
    filters: appliedFilters,
    totalRecords: entries.length,
    entries: formattedEntries,
    summary: {
      byAction,
      byStatus,
      byUser,
    },
  };
}
