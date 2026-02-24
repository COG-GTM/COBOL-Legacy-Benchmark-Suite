package com.investment.portfolio.reporting;

import com.investment.portfolio.common.ErrorHandler;
import com.investment.portfolio.common.FileHandler;
import com.investment.portfolio.common.ReturnCode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

/**
 * System Statistics Report Generator (RPTSTA00) - Java equivalent of RPTSTA00.cbl
 *
 * Original COBOL: src/programs/batch/RPTSTA00.cbl
 *
 * Responsibilities:
 * - Generates performance and statistics reports
 * - Processes DB2 statistics (calls, elapsed time, CPU, wait time)
 * - Processes batch statistics (jobs, success rate, elapsed time)
 * - Computes averages and totals for all metrics
 *
 * Files:
 * - DB2-STATS   (input):  Database performance statistics
 * - BATCH-STATS (input):  Batch job execution statistics
 * - REPORT-FILE (output): Formatted statistics report
 *
 * Metrics tracked:
 * - DB2: Total calls, elapsed time, CPU time, wait time, deadlocks
 * - Batch: Total jobs, successful, failed, elapsed time, records processed
 */
public class StatisticsReportGenerator {

    private static final Logger LOGGER = Logger.getLogger(StatisticsReportGenerator.class.getName());
    private static final String PROGRAM_ID = "RPTSTA00";
    private static final DateTimeFormatter REPORT_DATE_FMT = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int PAGE_WIDTH = 132;

    private final Path db2StatsPath;
    private final Path batchStatsPath;
    private final Path reportFilePath;

    private final ErrorHandler errorHandler;
    private final ReturnCode returnCode;

    /** DB2 statistics accumulators */
    private long db2TotalCalls;
    private long db2TotalElapsed;    // milliseconds
    private long db2TotalCpu;        // milliseconds
    private long db2TotalWait;       // milliseconds
    private long db2DeadlockCount;
    private long db2TimeoutCount;
    private long db2RecordCount;

    /** Batch statistics accumulators */
    private long batchTotalJobs;
    private long batchSuccessful;
    private long batchFailed;
    private long batchTotalElapsed;   // milliseconds
    private long batchTotalRecords;
    private long batchRecordCount;

    public StatisticsReportGenerator(Path db2StatsPath, Path batchStatsPath,
                                     Path reportFilePath) {
        this.db2StatsPath = db2StatsPath;
        this.batchStatsPath = batchStatsPath;
        this.reportFilePath = reportFilePath;
        this.errorHandler = new ErrorHandler(PROGRAM_ID);
        this.returnCode = new ReturnCode(PROGRAM_ID);
    }

    /**
     * Main entry point - maps to COBOL 0000-MAIN.
     *
     * PERFORM 1000-INITIALIZE
     * PERFORM 2000-PROCESS-DB2-STATS
     * PERFORM 3000-PROCESS-BATCH-STATS
     * PERFORM 4000-CALCULATE-AVERAGES
     * PERFORM 5000-PRINT-REPORT
     * PERFORM 9000-TERMINATE
     */
    public int execute() {
        LOGGER.info(PROGRAM_ID + " - Statistics Report generation starting");

        try {
            initialize();
            processDb2Stats();
            processBatchStats();
            printReport();
            terminate();
        } catch (Exception e) {
            errorHandler.handleSystemError("E999", "Unexpected error in " + PROGRAM_ID, e);
            returnCode.setCode(ReturnCode.SEVERE);
        }

        LOGGER.info(PROGRAM_ID + " - Completed with return code: " + returnCode.getCurrentCode());
        return returnCode.getCurrentCode();
    }

    /**
     * 1000-INITIALIZE: Reset all accumulators.
     */
    private void initialize() {
        db2TotalCalls = 0;
        db2TotalElapsed = 0;
        db2TotalCpu = 0;
        db2TotalWait = 0;
        db2DeadlockCount = 0;
        db2TimeoutCount = 0;
        db2RecordCount = 0;

        batchTotalJobs = 0;
        batchSuccessful = 0;
        batchFailed = 0;
        batchTotalElapsed = 0;
        batchTotalRecords = 0;
        batchRecordCount = 0;

        LOGGER.info(PROGRAM_ID + " - Initialization complete");
    }

    /**
     * 2000-PROCESS-DB2-STATS: Read and accumulate DB2 performance statistics.
     *
     * Maps to:
     *   OPEN INPUT DB2-STATS
     *   READ DB2-STATS AT END SET WS-EOF-DB2 TO TRUE
     *   PERFORM UNTIL WS-EOF-DB2
     *     ADD DB2S-CALLS TO WS-TOTAL-DB2-CALLS
     *     ADD DB2S-ELAPSED TO WS-TOTAL-DB2-ELAPSED
     *     ADD DB2S-CPU TO WS-TOTAL-DB2-CPU
     *     ADD DB2S-WAIT TO WS-TOTAL-DB2-WAIT
     *     READ DB2-STATS AT END SET WS-EOF-DB2 TO TRUE
     *   END-PERFORM
     */
    private void processDb2Stats() {
        try (FileHandler db2File = new FileHandler(db2StatsPath)) {
            if (!FileHandler.STATUS_SUCCESS.equals(db2File.openInput())) {
                LOGGER.info("No DB2 statistics file available");
                return;
            }

            String line;
            while ((line = db2File.readLine()) != null) {
                db2RecordCount++;
                parseDb2StatsLine(line);
            }
        } catch (Exception e) {
            errorHandler.handleSystemError("E200", "Error processing DB2 stats", e);
        }
    }

    /**
     * Parses a DB2 statistics record.
     * Expected format: CALLS|ELAPSED|CPU|WAIT|DEADLOCKS|TIMEOUTS
     */
    private void parseDb2StatsLine(String line) {
        try {
            String[] fields = line.split("\\|");
            if (fields.length >= 4) {
                db2TotalCalls += Long.parseLong(fields[0].trim());
                db2TotalElapsed += Long.parseLong(fields[1].trim());
                db2TotalCpu += Long.parseLong(fields[2].trim());
                db2TotalWait += Long.parseLong(fields[3].trim());
            }
            if (fields.length >= 5) {
                db2DeadlockCount += Long.parseLong(fields[4].trim());
            }
            if (fields.length >= 6) {
                db2TimeoutCount += Long.parseLong(fields[5].trim());
            }
        } catch (NumberFormatException e) {
            // Skip malformed lines
        }
    }

    /**
     * 3000-PROCESS-BATCH-STATS: Read and accumulate batch job statistics.
     *
     * Maps to:
     *   OPEN INPUT BATCH-STATS
     *   READ BATCH-STATS AT END SET WS-EOF-BATCH TO TRUE
     *   PERFORM UNTIL WS-EOF-BATCH
     *     ADD 1 TO WS-TOTAL-JOBS
     *     IF BCH-STATUS = 'S'
     *       ADD 1 TO WS-SUCCESSFUL
     *     ELSE
     *       ADD 1 TO WS-FAILED
     *     END-IF
     *     ADD BCH-ELAPSED TO WS-TOTAL-ELAPSED
     *     ADD BCH-RECORDS TO WS-TOTAL-RECORDS
     *     READ BATCH-STATS AT END SET WS-EOF-BATCH TO TRUE
     *   END-PERFORM
     */
    private void processBatchStats() {
        try (FileHandler batchFile = new FileHandler(batchStatsPath)) {
            if (!FileHandler.STATUS_SUCCESS.equals(batchFile.openInput())) {
                LOGGER.info("No batch statistics file available");
                return;
            }

            String line;
            while ((line = batchFile.readLine()) != null) {
                batchRecordCount++;
                parseBatchStatsLine(line);
            }
        } catch (Exception e) {
            errorHandler.handleSystemError("E300", "Error processing batch stats", e);
        }
    }

    /**
     * Parses a batch statistics record.
     * Expected format: JOBNAME|STATUS|ELAPSED|RECORDS
     */
    private void parseBatchStatsLine(String line) {
        try {
            String[] fields = line.split("\\|");
            if (fields.length >= 4) {
                batchTotalJobs++;

                String status = fields[1].trim();
                if ("S".equals(status) || "SUCCESS".equalsIgnoreCase(status)) {
                    batchSuccessful++;
                } else {
                    batchFailed++;
                }

                batchTotalElapsed += Long.parseLong(fields[2].trim());
                batchTotalRecords += Long.parseLong(fields[3].trim());
            }
        } catch (NumberFormatException e) {
            // Skip malformed lines
        }
    }

    /**
     * 5000-PRINT-REPORT: Generate the formatted statistics report.
     */
    private void printReport() {
        try (FileHandler reportFile = new FileHandler(reportFilePath)) {
            reportFile.openOutput();

            // Report header
            reportFile.writeLine("=".repeat(PAGE_WIDTH));
            reportFile.writeLine(String.format("%-40s %s %40s",
                    "DATE: " + LocalDate.now().format(REPORT_DATE_FMT),
                    "SYSTEM STATISTICS REPORT",
                    "PROGRAM: " + PROGRAM_ID));
            reportFile.writeLine("=".repeat(PAGE_WIDTH));
            reportFile.writeLine("");

            // DB2 Statistics Section
            reportFile.writeLine("DB2 PERFORMANCE STATISTICS");
            reportFile.writeLine("-".repeat(50));
            reportFile.writeLine(String.format("  %-35s %,15d", "Total SQL Calls:", db2TotalCalls));
            reportFile.writeLine(String.format("  %-35s %,15d ms", "Total Elapsed Time:", db2TotalElapsed));
            reportFile.writeLine(String.format("  %-35s %,15d ms", "Total CPU Time:", db2TotalCpu));
            reportFile.writeLine(String.format("  %-35s %,15d ms", "Total Wait Time:", db2TotalWait));
            reportFile.writeLine(String.format("  %-35s %,15d", "Deadlock Count:", db2DeadlockCount));
            reportFile.writeLine(String.format("  %-35s %,15d", "Timeout Count:", db2TimeoutCount));
            reportFile.writeLine(String.format("  %-35s %,15d", "Statistics Records:", db2RecordCount));

            // DB2 averages
            if (db2TotalCalls > 0) {
                reportFile.writeLine("");
                reportFile.writeLine("  AVERAGES (per call):");
                BigDecimal avgElapsed = BigDecimal.valueOf(db2TotalElapsed)
                        .divide(BigDecimal.valueOf(db2TotalCalls), 2, RoundingMode.HALF_UP);
                BigDecimal avgCpu = BigDecimal.valueOf(db2TotalCpu)
                        .divide(BigDecimal.valueOf(db2TotalCalls), 2, RoundingMode.HALF_UP);
                BigDecimal avgWait = BigDecimal.valueOf(db2TotalWait)
                        .divide(BigDecimal.valueOf(db2TotalCalls), 2, RoundingMode.HALF_UP);
                reportFile.writeLine(String.format("  %-35s %15s ms", "Avg Elapsed Time:", avgElapsed));
                reportFile.writeLine(String.format("  %-35s %15s ms", "Avg CPU Time:", avgCpu));
                reportFile.writeLine(String.format("  %-35s %15s ms", "Avg Wait Time:", avgWait));
            }
            reportFile.writeLine("");

            // Batch Statistics Section
            reportFile.writeLine("BATCH JOB STATISTICS");
            reportFile.writeLine("-".repeat(50));
            reportFile.writeLine(String.format("  %-35s %,15d", "Total Jobs:", batchTotalJobs));
            reportFile.writeLine(String.format("  %-35s %,15d", "Successful:", batchSuccessful));
            reportFile.writeLine(String.format("  %-35s %,15d", "Failed:", batchFailed));
            reportFile.writeLine(String.format("  %-35s %,15d ms", "Total Elapsed Time:", batchTotalElapsed));
            reportFile.writeLine(String.format("  %-35s %,15d", "Total Records Processed:", batchTotalRecords));
            reportFile.writeLine(String.format("  %-35s %,15d", "Statistics Records:", batchRecordCount));

            // Batch averages and success rate
            if (batchTotalJobs > 0) {
                reportFile.writeLine("");
                reportFile.writeLine("  AVERAGES AND RATES:");
                BigDecimal successRate = BigDecimal.valueOf(batchSuccessful)
                        .divide(BigDecimal.valueOf(batchTotalJobs), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                BigDecimal avgElapsed = BigDecimal.valueOf(batchTotalElapsed)
                        .divide(BigDecimal.valueOf(batchTotalJobs), 2, RoundingMode.HALF_UP);
                BigDecimal avgRecords = BigDecimal.valueOf(batchTotalRecords)
                        .divide(BigDecimal.valueOf(batchTotalJobs), 0, RoundingMode.HALF_UP);

                reportFile.writeLine(String.format("  %-35s %14s%%", "Success Rate:", successRate));
                reportFile.writeLine(String.format("  %-35s %15s ms", "Avg Elapsed Time:", avgElapsed));
                reportFile.writeLine(String.format("  %-35s %15s", "Avg Records per Job:", avgRecords));
            }
            reportFile.writeLine("");

            // Report footer
            reportFile.writeLine("=".repeat(PAGE_WIDTH));
            reportFile.writeLine(String.format("Report Generated: %s",
                    LocalDateTime.now().format(TIMESTAMP_FMT)));
            reportFile.writeLine(centerText("*** END OF STATISTICS REPORT ***"));

        } catch (Exception e) {
            errorHandler.handleSystemError("E500", "Error writing statistics report", e);
            returnCode.setCode(ReturnCode.ERROR);
        }
    }

    /**
     * 9000-TERMINATE: Final processing.
     */
    private void terminate() {
        if (batchFailed > 0 && returnCode.getCurrentCode() < ReturnCode.WARNING) {
            returnCode.setCode(ReturnCode.WARNING);
        }

        LOGGER.info(PROGRAM_ID + " - Report generated: DB2 records=" + db2RecordCount
                + " Batch records=" + batchRecordCount);
    }

    private String centerText(String text) {
        int pad = (PAGE_WIDTH - text.length()) / 2;
        return " ".repeat(Math.max(0, pad)) + text;
    }
}
