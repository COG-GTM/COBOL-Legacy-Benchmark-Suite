package com.investment.portfolio.utility;

import com.investment.portfolio.common.ErrorHandler;
import com.investment.portfolio.common.FileHandler;
import com.investment.portfolio.common.ReturnCode;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

/**
 * File Maintenance Utility (UTLMNT00) - Java equivalent of UTLMNT00.cbl
 *
 * Original COBOL: src/programs/utility/UTLMNT00.cbl
 *
 * Responsibilities:
 * - Performs VSAM file maintenance operations
 * - Archives aged records to archive files
 * - Cleans up expired records
 * - Reorganizes files for optimal access
 * - Analyzes file statistics and health
 *
 * Functions (from WS-MAINT-FUNCTION):
 * - ARCHIVE:  Archive aged records beyond retention period
 * - CLEANUP:  Remove expired/deleted records
 * - REORG:    Reorganize file for performance
 * - ANALYZE:  Analyze file statistics and report
 *
 * Files:
 * - CONTROL-FILE (input):  Maintenance control parameters
 * - ARCHIVE-FILE (output): Archived records destination
 * - REPORT-FILE  (output): Maintenance activity report
 */
public class FileMaintenance {

    private static final Logger LOGGER = Logger.getLogger(FileMaintenance.class.getName());
    private static final String PROGRAM_ID = "UTLMNT00";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Maintenance function codes matching COBOL WS-MAINT-FUNCTION */
    public enum MaintenanceFunction {
        ARCHIVE, CLEANUP, REORG, ANALYZE
    }

    private final Path controlFilePath;
    private final Path archiveFilePath;
    private final Path reportFilePath;
    private final Path dataDirectoryPath;

    private final ErrorHandler errorHandler;
    private final ReturnCode returnCode;

    /** Processing counters */
    private long recordsRead;
    private long recordsArchived;
    private long recordsDeleted;
    private long recordsProcessed;
    private long errorCount;

    /** Control parameters */
    private int retentionDays = 365;
    private int cleanupAgeDays = 90;

    public FileMaintenance(Path controlFilePath, Path archiveFilePath,
                           Path reportFilePath, Path dataDirectoryPath) {
        this.controlFilePath = controlFilePath;
        this.archiveFilePath = archiveFilePath;
        this.reportFilePath = reportFilePath;
        this.dataDirectoryPath = dataDirectoryPath;
        this.errorHandler = new ErrorHandler(PROGRAM_ID);
        this.returnCode = new ReturnCode(PROGRAM_ID);
    }

    /**
     * Main entry point - maps to COBOL 0000-MAIN.
     *
     * PERFORM 1000-INITIALIZE
     * EVALUATE WS-MAINT-FUNCTION
     *   WHEN 'ARCHIVE' PERFORM 2000-ARCHIVE-RECORDS
     *   WHEN 'CLEANUP' PERFORM 3000-CLEANUP-RECORDS
     *   WHEN 'REORG'   PERFORM 4000-REORGANIZE-FILE
     *   WHEN 'ANALYZE' PERFORM 5000-ANALYZE-FILE
     * END-EVALUATE
     * PERFORM 9000-TERMINATE
     */
    public int execute(MaintenanceFunction function) {
        LOGGER.info(PROGRAM_ID + " - File Maintenance starting: " + function);

        try {
            initialize();

            switch (function) {
                case ARCHIVE:
                    archiveRecords();
                    break;
                case CLEANUP:
                    cleanupRecords();
                    break;
                case REORG:
                    reorganizeFile();
                    break;
                case ANALYZE:
                    analyzeFile();
                    break;
            }

            terminate();
        } catch (Exception e) {
            errorHandler.handleSystemError("E999", "Unexpected error in " + PROGRAM_ID, e);
            returnCode.setCode(ReturnCode.SEVERE);
        }

        LOGGER.info(PROGRAM_ID + " - Completed with return code: " + returnCode.getCurrentCode());
        return returnCode.getCurrentCode();
    }

    /**
     * 1000-INITIALIZE: Read control file, initialize counters.
     */
    private void initialize() {
        recordsRead = 0;
        recordsArchived = 0;
        recordsDeleted = 0;
        recordsProcessed = 0;
        errorCount = 0;

        loadControlParameters();
        LOGGER.info(PROGRAM_ID + " - Initialization complete. Retention: "
                + retentionDays + " days");
    }

    /**
     * Loads maintenance control parameters from the control file.
     * Maps to COBOL READ CONTROL-FILE.
     */
    private void loadControlParameters() {
        try (FileHandler controlFile = new FileHandler(controlFilePath)) {
            if (!FileHandler.STATUS_SUCCESS.equals(controlFile.openInput())) {
                LOGGER.info("No control file found; using defaults");
                return;
            }

            String line;
            while ((line = controlFile.readLine()) != null) {
                if (line.startsWith("RETENTION=")) {
                    retentionDays = Integer.parseInt(line.substring(10).trim());
                } else if (line.startsWith("CLEANUP_AGE=")) {
                    cleanupAgeDays = Integer.parseInt(line.substring(12).trim());
                }
            }
        } catch (Exception e) {
            LOGGER.info("Using default control parameters");
        }
    }

    /**
     * 2000-ARCHIVE-RECORDS: Archive aged records beyond retention period.
     *
     * Maps to:
     *   OPEN INPUT  source-file
     *   OPEN OUTPUT ARCHIVE-FILE
     *   READ source-file AT END SET WS-EOF TO TRUE
     *   PERFORM UNTIL WS-EOF
     *     IF record-date < cutoff-date
     *       WRITE ARCHIVE-RECORD FROM WS-DATA-RECORD
     *       DELETE source-file RECORD
     *       ADD 1 TO WS-ARCHIVED
     *     END-IF
     *     READ source-file AT END SET WS-EOF TO TRUE
     *   END-PERFORM
     */
    private void archiveRecords() {
        LOGGER.info("Archiving records older than " + retentionDays + " days");
        LocalDate cutoffDate = LocalDate.now().minusDays(retentionDays);
        String cutoff = cutoffDate.format(DATE_FMT);

        try {
            if (!Files.exists(dataDirectoryPath)) {
                LOGGER.warning("Data directory not found: " + dataDirectoryPath);
                return;
            }

            try (FileHandler archiveFile = new FileHandler(archiveFilePath)) {
                archiveFile.openOutput();

                // Process each data file in the directory
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(dataDirectoryPath, "*.dat")) {
                    for (Path dataFile : stream) {
                        archiveFileRecords(dataFile, archiveFile, cutoff);
                    }
                }
            }
        } catch (Exception e) {
            errorHandler.handleSystemError("E200", "Error during archive", e);
            returnCode.setCode(ReturnCode.ERROR);
        }
    }

    /**
     * Archives records from a single data file.
     */
    private void archiveFileRecords(Path dataFile, FileHandler archiveFile, String cutoff) {
        Path tempFile = dataFile.resolveSibling(dataFile.getFileName() + ".tmp");

        try (FileHandler inputFile = new FileHandler(dataFile);
             FileHandler outputFile = new FileHandler(tempFile)) {

            inputFile.openInput();
            outputFile.openOutput();

            String line;
            while ((line = inputFile.readLine()) != null) {
                recordsRead++;

                // Extract date from record (first 8 chars or embedded date field)
                String recordDate = extractDateField(line);

                if (recordDate != null && recordDate.compareTo(cutoff) < 0) {
                    // Archive old record
                    archiveFile.writeLine(line);
                    recordsArchived++;
                } else {
                    // Keep current record
                    outputFile.writeLine(line);
                }
                recordsProcessed++;
            }
        } catch (Exception e) {
            errorHandler.handleProcessingError("E201", "Error archiving file: " + dataFile, e.getMessage());
            errorCount++;
        }

        // Replace original with filtered version
        try {
            if (Files.exists(tempFile)) {
                Files.move(tempFile, dataFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            errorHandler.handleSystemError("E202", "Error replacing file after archive", e);
        }
    }

    /**
     * 3000-CLEANUP-RECORDS: Remove expired/deleted records.
     *
     * Maps to the COBOL CLEANUP function that deletes logically deleted
     * records (status 'D' or 'X') and records past cleanup age.
     */
    private void cleanupRecords() {
        LOGGER.info("Cleaning up expired records older than " + cleanupAgeDays + " days");
        LocalDate cutoffDate = LocalDate.now().minusDays(cleanupAgeDays);
        String cutoff = cutoffDate.format(DATE_FMT);

        try {
            if (!Files.exists(dataDirectoryPath)) {
                LOGGER.warning("Data directory not found: " + dataDirectoryPath);
                return;
            }

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dataDirectoryPath, "*.dat")) {
                for (Path dataFile : stream) {
                    cleanupFileRecords(dataFile, cutoff);
                }
            }
        } catch (Exception e) {
            errorHandler.handleSystemError("E300", "Error during cleanup", e);
            returnCode.setCode(ReturnCode.ERROR);
        }
    }

    private void cleanupFileRecords(Path dataFile, String cutoff) {
        Path tempFile = dataFile.resolveSibling(dataFile.getFileName() + ".tmp");

        try (FileHandler inputFile = new FileHandler(dataFile);
             FileHandler outputFile = new FileHandler(tempFile)) {

            inputFile.openInput();
            outputFile.openOutput();

            String line;
            while ((line = inputFile.readLine()) != null) {
                recordsRead++;

                boolean shouldDelete = false;

                // Check for logically deleted records (status D or X)
                if (line.length() > 0) {
                    char lastChar = line.charAt(line.length() - 1);
                    if (lastChar == 'D' || lastChar == 'X') {
                        shouldDelete = true;
                    }
                }

                // Check for expired records
                if (!shouldDelete) {
                    String recordDate = extractDateField(line);
                    if (recordDate != null && recordDate.compareTo(cutoff) < 0) {
                        shouldDelete = true;
                    }
                }

                if (shouldDelete) {
                    recordsDeleted++;
                } else {
                    outputFile.writeLine(line);
                }
                recordsProcessed++;
            }
        } catch (Exception e) {
            errorHandler.handleProcessingError("E301", "Error cleaning file: " + dataFile, e.getMessage());
            errorCount++;
        }

        try {
            if (Files.exists(tempFile)) {
                Files.move(tempFile, dataFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            errorHandler.handleSystemError("E302", "Error replacing file after cleanup", e);
        }
    }

    /**
     * 4000-REORGANIZE-FILE: Reorganize files for optimal access.
     *
     * Maps to the COBOL REORG function that reloads VSAM files
     * to reclaim CA/CI splits and optimize performance.
     * In Java, this copies files to remove fragmentation.
     */
    private void reorganizeFile() {
        LOGGER.info("Reorganizing data files");

        try {
            if (!Files.exists(dataDirectoryPath)) {
                LOGGER.warning("Data directory not found: " + dataDirectoryPath);
                return;
            }

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dataDirectoryPath, "*.dat")) {
                for (Path dataFile : stream) {
                    reorganizeSingleFile(dataFile);
                }
            }
        } catch (Exception e) {
            errorHandler.handleSystemError("E400", "Error during reorganization", e);
            returnCode.setCode(ReturnCode.ERROR);
        }
    }

    private void reorganizeSingleFile(Path dataFile) {
        Path tempFile = dataFile.resolveSibling(dataFile.getFileName() + ".reorg");

        try (FileHandler inputFile = new FileHandler(dataFile);
             FileHandler outputFile = new FileHandler(tempFile)) {

            inputFile.openInput();
            outputFile.openOutput();

            String line;
            while ((line = inputFile.readLine()) != null) {
                recordsRead++;
                if (!line.trim().isEmpty()) {
                    outputFile.writeLine(line);
                    recordsProcessed++;
                }
            }
        } catch (Exception e) {
            errorHandler.handleProcessingError("E401", "Error reorganizing: " + dataFile, e.getMessage());
            errorCount++;
            return;
        }

        try {
            Files.move(tempFile, dataFile, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("Reorganized: " + dataFile.getFileName());
        } catch (IOException e) {
            errorHandler.handleSystemError("E402", "Error replacing file after reorg", e);
        }
    }

    /**
     * 5000-ANALYZE-FILE: Analyze file statistics and generate report.
     *
     * Maps to the COBOL ANALYZE function that examines VSAM cluster
     * statistics: record counts, CI/CA splits, freespace, etc.
     * In Java, this examines file sizes, record counts, and health metrics.
     */
    private void analyzeFile() {
        LOGGER.info("Analyzing data files");

        try (FileHandler reportFile = new FileHandler(reportFilePath)) {
            reportFile.openOutput();

            reportFile.writeLine("=" .repeat(70));
            reportFile.writeLine("FILE MAINTENANCE ANALYSIS REPORT");
            reportFile.writeLine("Generated: " + LocalDateTime.now().format(TIMESTAMP_FMT));
            reportFile.writeLine("=" .repeat(70));
            reportFile.writeLine("");

            if (!Files.exists(dataDirectoryPath)) {
                reportFile.writeLine("WARNING: Data directory not found: " + dataDirectoryPath);
                return;
            }

            long totalFiles = 0;
            long totalRecords = 0;
            long totalSize = 0;

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dataDirectoryPath, "*.dat")) {
                for (Path dataFile : stream) {
                    totalFiles++;
                    long fileSize = Files.size(dataFile);
                    totalSize += fileSize;

                    // Count records
                    long fileRecords = 0;
                    try (FileHandler f = new FileHandler(dataFile)) {
                        f.openInput();
                        while (f.readLine() != null) {
                            fileRecords++;
                        }
                    }
                    totalRecords += fileRecords;

                    BasicFileAttributes attrs = Files.readAttributes(dataFile, BasicFileAttributes.class);

                    reportFile.writeLine(String.format("File: %-30s Records: %8d  Size: %10d bytes",
                            dataFile.getFileName(), fileRecords, fileSize));
                    reportFile.writeLine(String.format("  Created: %s  Modified: %s",
                            attrs.creationTime(), attrs.lastModifiedTime()));
                    recordsProcessed++;
                }
            }

            reportFile.writeLine("");
            reportFile.writeLine("-".repeat(70));
            reportFile.writeLine(String.format("SUMMARY: Files: %d  Total Records: %d  Total Size: %d bytes",
                    totalFiles, totalRecords, totalSize));
            reportFile.writeLine("-".repeat(70));

        } catch (Exception e) {
            errorHandler.handleSystemError("E500", "Error during analysis", e);
            returnCode.setCode(ReturnCode.ERROR);
        }
    }

    /**
     * 9000-TERMINATE: Display final statistics.
     */
    private void terminate() {
        if (errorCount > 0 && returnCode.getCurrentCode() < ReturnCode.WARNING) {
            returnCode.setCode(ReturnCode.WARNING);
        }

        displayStatistics();
    }

    /**
     * Extracts date field from a record line.
     * Assumes date is in the first 8 characters (YYYYMMDD format).
     */
    private String extractDateField(String line) {
        if (line == null || line.length() < 8) return null;
        String potential = line.substring(0, 8).trim();
        if (potential.matches("\\d{8}")) {
            return potential;
        }
        return null;
    }

    private void displayStatistics() {
        LOGGER.info(PROGRAM_ID + " Processing Statistics:");
        LOGGER.info("  Records Read:      " + recordsRead);
        LOGGER.info("  Records Processed: " + recordsProcessed);
        LOGGER.info("  Records Archived:  " + recordsArchived);
        LOGGER.info("  Records Deleted:   " + recordsDeleted);
        LOGGER.info("  Error Count:       " + errorCount);
        LOGGER.info("  Return Code:       " + returnCode.getCurrentCode());
    }
}
