package com.investment.portfolio.reporting;

import com.investment.portfolio.common.ErrorHandler;
import com.investment.portfolio.common.FileHandler;
import com.investment.portfolio.common.ReturnCode;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Audit Report Generator (RPTAUD00) - Java equivalent of RPTAUD00.cbl
 *
 * Original COBOL: src/programs/batch/RPTAUD00.cbl
 *
 * Responsibilities:
 * - Generates audit trail and error summary reports
 * - Processes audit log file for security and activity tracking
 * - Processes error file for error categorization and summary
 * - Produces control summary with totals and statistics
 *
 * Files:
 * - AUDIT-FILE  (input):  Audit trail records
 * - ERROR-FILE  (input):  Error log records
 * - REPORT-FILE (output): Formatted audit/error report
 *
 * Report sections:
 *   Section 1: Audit Trail Detail
 *   Section 2: Error Log Detail
 *   Section 3: Control Summary (counts, categories, severity distribution)
 */
public class AuditReportGenerator {

    private static final Logger LOGGER = Logger.getLogger(AuditReportGenerator.class.getName());
    private static final String PROGRAM_ID = "RPTAUD00";
    private static final DateTimeFormatter REPORT_DATE_FMT = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int PAGE_WIDTH = 132;
    private static final int LINES_PER_PAGE = 60;

    private final Path auditFilePath;
    private final Path errorFilePath;
    private final Path reportFilePath;

    private final ErrorHandler errorHandler;
    private final ReturnCode returnCode;

    /** Report state */
    private int pageNumber;
    private int lineCount;

    /** Audit counters */
    private long auditRecordCount;
    private final Map<String, Integer> auditByType;
    private final Map<String, Integer> auditByAction;
    private final Map<String, Integer> auditByStatus;

    /** Error counters */
    private long errorRecordCount;
    private final Map<String, Integer> errorByCategory;
    private final Map<String, Integer> errorBySeverity;

    public AuditReportGenerator(Path auditFilePath, Path errorFilePath, Path reportFilePath) {
        this.auditFilePath = auditFilePath;
        this.errorFilePath = errorFilePath;
        this.reportFilePath = reportFilePath;
        this.errorHandler = new ErrorHandler(PROGRAM_ID);
        this.returnCode = new ReturnCode(PROGRAM_ID);
        this.auditByType = new LinkedHashMap<>();
        this.auditByAction = new LinkedHashMap<>();
        this.auditByStatus = new LinkedHashMap<>();
        this.errorByCategory = new LinkedHashMap<>();
        this.errorBySeverity = new LinkedHashMap<>();
    }

    /**
     * Main entry point - maps to COBOL 0000-MAIN.
     *
     * PERFORM 1000-INITIALIZE
     * PERFORM 2000-PROCESS-AUDIT-FILE
     * PERFORM 3000-PROCESS-ERROR-FILE
     * PERFORM 4000-PRINT-CONTROL-SUMMARY
     * PERFORM 9000-TERMINATE
     */
    public int execute() {
        LOGGER.info(PROGRAM_ID + " - Audit Report generation starting");

        try (FileHandler reportFile = new FileHandler(reportFilePath)) {
            initialize(reportFile);
            processAuditFile(reportFile);
            processErrorFile(reportFile);
            printControlSummary(reportFile);
            terminate(reportFile);
        } catch (Exception e) {
            errorHandler.handleSystemError("E999", "Unexpected error in " + PROGRAM_ID, e);
            returnCode.setCode(ReturnCode.SEVERE);
        }

        LOGGER.info(PROGRAM_ID + " - Completed with return code: " + returnCode.getCurrentCode());
        return returnCode.getCurrentCode();
    }

    /**
     * 1000-INITIALIZE: Open report file, print header.
     */
    private void initialize(FileHandler reportFile) {
        pageNumber = 0;
        lineCount = LINES_PER_PAGE;
        auditRecordCount = 0;
        errorRecordCount = 0;

        reportFile.openOutput();
        printPageHeader(reportFile, "AUDIT AND ERROR REPORT");
    }

    /**
     * 2000-PROCESS-AUDIT-FILE: Read and report audit trail records.
     *
     * Maps to:
     *   OPEN INPUT AUDIT-FILE
     *   READ AUDIT-FILE AT END SET WS-EOF-AUDIT TO TRUE
     *   PERFORM UNTIL WS-EOF-AUDIT
     *     PERFORM 2100-PRINT-AUDIT-DETAIL
     *     PERFORM 2200-ACCUMULATE-AUDIT-STATS
     *     READ AUDIT-FILE AT END SET WS-EOF-AUDIT TO TRUE
     *   END-PERFORM
     */
    private void processAuditFile(FileHandler reportFile) {
        printSectionHeader(reportFile, "SECTION 1: AUDIT TRAIL");

        printLine(reportFile, String.format("%-26s %-8s %-8s %-8s %-4s %-8s %-4s %s",
                "Timestamp", "System", "User", "Program", "Type", "Action", "Stat", "Message"));
        printLine(reportFile, "-".repeat(PAGE_WIDTH));

        try (FileHandler auditFile = new FileHandler(auditFilePath)) {
            if (!FileHandler.STATUS_SUCCESS.equals(auditFile.openInput())) {
                printLine(reportFile, "*** NO AUDIT DATA AVAILABLE ***");
                return;
            }

            String line;
            while ((line = auditFile.readLine()) != null) {
                auditRecordCount++;

                // Parse audit record fields
                AuditFields fields = parseAuditRecord(line);
                if (fields == null) continue;

                // 2100-PRINT-AUDIT-DETAIL
                checkPageBreak(reportFile, 1);
                printLine(reportFile, String.format("%-26s %-8s %-8s %-8s %-4s %-8s %-4s %s",
                        fields.timestamp, fields.systemId, fields.userId,
                        fields.program, fields.type, fields.action,
                        fields.status, fields.message));

                // 2200-ACCUMULATE-AUDIT-STATS
                auditByType.merge(fields.type, 1, Integer::sum);
                auditByAction.merge(fields.action, 1, Integer::sum);
                auditByStatus.merge(fields.status, 1, Integer::sum);
            }
        } catch (Exception e) {
            errorHandler.handleSystemError("E200", "Error processing audit file", e);
            returnCode.setCode(ReturnCode.ERROR);
        }

        printLine(reportFile, "");
        printLine(reportFile, "Total audit records: " + auditRecordCount);
        printLine(reportFile, "");
    }

    /**
     * 3000-PROCESS-ERROR-FILE: Read and report error log records.
     *
     * Maps to:
     *   OPEN INPUT ERROR-FILE
     *   READ ERROR-FILE AT END SET WS-EOF-ERROR TO TRUE
     *   PERFORM UNTIL WS-EOF-ERROR
     *     PERFORM 3100-PRINT-ERROR-DETAIL
     *     PERFORM 3200-ACCUMULATE-ERROR-STATS
     *     READ ERROR-FILE AT END SET WS-EOF-ERROR TO TRUE
     *   END-PERFORM
     */
    private void processErrorFile(FileHandler reportFile) {
        printSectionHeader(reportFile, "SECTION 2: ERROR LOG");

        printLine(reportFile, String.format("%-26s %-8s %-4s %-8s %4s %s",
                "Timestamp", "Program", "Cat", "Code", "Sev", "Description"));
        printLine(reportFile, "-".repeat(PAGE_WIDTH));

        try (FileHandler errFile = new FileHandler(errorFilePath)) {
            if (!FileHandler.STATUS_SUCCESS.equals(errFile.openInput())) {
                printLine(reportFile, "*** NO ERROR DATA AVAILABLE ***");
                return;
            }

            String line;
            while ((line = errFile.readLine()) != null) {
                errorRecordCount++;

                ErrorFields fields = parseErrorRecord(line);
                if (fields == null) continue;

                // 3100-PRINT-ERROR-DETAIL
                checkPageBreak(reportFile, 1);
                printLine(reportFile, String.format("%-26s %-8s %-4s %-8s %4s %s",
                        fields.timestamp, fields.program, fields.category,
                        fields.errorCode, fields.severity, fields.description));

                // 3200-ACCUMULATE-ERROR-STATS
                errorByCategory.merge(fields.category, 1, Integer::sum);
                errorBySeverity.merge(fields.severity, 1, Integer::sum);
            }
        } catch (Exception e) {
            errorHandler.handleSystemError("E300", "Error processing error file", e);
            returnCode.setCode(ReturnCode.ERROR);
        }

        printLine(reportFile, "");
        printLine(reportFile, "Total error records: " + errorRecordCount);
        printLine(reportFile, "");
    }

    /**
     * 4000-PRINT-CONTROL-SUMMARY: Print summary statistics.
     */
    private void printControlSummary(FileHandler reportFile) {
        printSectionHeader(reportFile, "SECTION 3: CONTROL SUMMARY");

        // Audit summary
        printLine(reportFile, "AUDIT TRAIL SUMMARY");
        printLine(reportFile, String.format("  Total Records: %,d", auditRecordCount));
        printLine(reportFile, "");

        printLine(reportFile, "  By Type:");
        for (Map.Entry<String, Integer> entry : auditByType.entrySet()) {
            printLine(reportFile, String.format("    %-10s: %,d", entry.getKey(), entry.getValue()));
        }

        printLine(reportFile, "  By Action:");
        for (Map.Entry<String, Integer> entry : auditByAction.entrySet()) {
            printLine(reportFile, String.format("    %-10s: %,d", entry.getKey(), entry.getValue()));
        }

        printLine(reportFile, "  By Status:");
        for (Map.Entry<String, Integer> entry : auditByStatus.entrySet()) {
            printLine(reportFile, String.format("    %-10s: %,d", entry.getKey(), entry.getValue()));
        }

        printLine(reportFile, "");

        // Error summary
        printLine(reportFile, "ERROR LOG SUMMARY");
        printLine(reportFile, String.format("  Total Records: %,d", errorRecordCount));
        printLine(reportFile, "");

        printLine(reportFile, "  By Category:");
        for (Map.Entry<String, Integer> entry : errorByCategory.entrySet()) {
            printLine(reportFile, String.format("    %-10s: %,d", entry.getKey(), entry.getValue()));
        }

        printLine(reportFile, "  By Severity:");
        for (Map.Entry<String, Integer> entry : errorBySeverity.entrySet()) {
            printLine(reportFile, String.format("    %-10s: %,d", entry.getKey(), entry.getValue()));
        }

        printLine(reportFile, "");
    }

    /**
     * 9000-TERMINATE: Print report footer.
     */
    private void terminate(FileHandler reportFile) {
        printLine(reportFile, "=".repeat(PAGE_WIDTH));
        printLine(reportFile, centerText("*** END OF AUDIT REPORT ***"));
        printLine(reportFile, centerText("Pages: " + pageNumber));

        if (errorRecordCount > 0 && returnCode.getCurrentCode() < ReturnCode.WARNING) {
            returnCode.setCode(ReturnCode.WARNING);
        }

        LOGGER.info(PROGRAM_ID + " - Report generated: audit=" + auditRecordCount
                + " errors=" + errorRecordCount);
    }

    // --- Parsing helpers ---

    private AuditFields parseAuditRecord(String line) {
        try {
            if (line == null || line.length() < 40) return null;
            AuditFields f = new AuditFields();
            int pos = 0;
            f.timestamp = line.substring(pos, Math.min(pos + 26, line.length())).trim(); pos += 26;
            if (line.length() > pos + 8) { f.systemId = line.substring(pos, pos + 8).trim(); pos += 8; }
            if (line.length() > pos + 8) { f.userId = line.substring(pos, pos + 8).trim(); pos += 8; }
            if (line.length() > pos + 8) { f.program = line.substring(pos, pos + 8).trim(); pos += 8; }
            if (line.length() > pos + 4) { f.type = line.substring(pos, pos + 4).trim(); pos += 4; }
            if (line.length() > pos + 8) { f.action = line.substring(pos, pos + 8).trim(); pos += 8; }
            if (line.length() > pos + 4) { f.status = line.substring(pos, pos + 4).trim(); pos += 4; }
            if (line.length() > pos) { f.message = line.substring(pos).trim(); }
            return f;
        } catch (Exception e) {
            return null;
        }
    }

    private ErrorFields parseErrorRecord(String line) {
        try {
            if (line == null || line.length() < 30) return null;
            ErrorFields f = new ErrorFields();
            int pos = 0;
            f.timestamp = line.substring(pos, Math.min(pos + 26, line.length())).trim(); pos += 26;
            if (line.length() > pos + 8) { f.program = line.substring(pos, pos + 8).trim(); pos += 8; }
            if (line.length() > pos + 4) { f.category = line.substring(pos, pos + 4).trim(); pos += 4; }
            if (line.length() > pos + 8) { f.errorCode = line.substring(pos, pos + 8).trim(); pos += 8; }
            if (line.length() > pos + 4) { f.severity = line.substring(pos, pos + 4).trim(); pos += 4; }
            if (line.length() > pos) { f.description = line.substring(pos).trim(); }
            return f;
        } catch (Exception e) {
            return null;
        }
    }

    // --- Report formatting helpers ---

    private void printPageHeader(FileHandler reportFile, String title) {
        pageNumber++;
        lineCount = 0;
        printLine(reportFile, "=".repeat(PAGE_WIDTH));
        printLine(reportFile, String.format("%-40s %s %40s",
                "DATE: " + LocalDate.now().format(REPORT_DATE_FMT),
                title, "PAGE: " + pageNumber));
        printLine(reportFile, String.format("%-40s %s",
                "PROGRAM: " + PROGRAM_ID,
                "INVESTMENT PORTFOLIO MANAGEMENT SYSTEM"));
        printLine(reportFile, "=".repeat(PAGE_WIDTH));
        printLine(reportFile, "");
    }

    private void printSectionHeader(FileHandler reportFile, String title) {
        checkPageBreak(reportFile, 5);
        printLine(reportFile, "");
        printLine(reportFile, title);
        printLine(reportFile, "-".repeat(title.length()));
        printLine(reportFile, "");
    }

    private void printLine(FileHandler reportFile, String line) {
        reportFile.writeLine(line);
        lineCount++;
    }

    private void checkPageBreak(FileHandler reportFile, int linesNeeded) {
        if (lineCount + linesNeeded > LINES_PER_PAGE) {
            printPageHeader(reportFile, "AUDIT AND ERROR REPORT (CONT.)");
        }
    }

    private String centerText(String text) {
        int pad = (PAGE_WIDTH - text.length()) / 2;
        return " ".repeat(Math.max(0, pad)) + text;
    }

    // --- Inner data classes ---

    private static class AuditFields {
        String timestamp = "";
        String systemId = "";
        String userId = "";
        String program = "";
        String type = "";
        String action = "";
        String status = "";
        String message = "";
    }

    private static class ErrorFields {
        String timestamp = "";
        String program = "";
        String category = "";
        String errorCode = "";
        String severity = "";
        String description = "";
    }
}
