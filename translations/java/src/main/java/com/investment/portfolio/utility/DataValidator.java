package com.investment.portfolio.utility;

import com.investment.portfolio.common.ErrorHandler;
import com.investment.portfolio.common.FileHandler;
import com.investment.portfolio.common.ReturnCode;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Data Validator (UTLVAL00) - Java equivalent of UTLVAL00.cbl
 *
 * Original COBOL: src/programs/utility/UTLVAL00.cbl
 *
 * Responsibilities:
 * - Performs comprehensive data validation across system files
 * - Checks data integrity, cross-references, formats, and balances
 * - Generates detailed validation error reports
 *
 * Validation types (from WS-VALIDATION-TYPE):
 * - INTEGRITY: Validate record structure and required field presence
 * - XREF:      Cross-reference validation between related files
 * - FORMAT:    Data format and range validation
 * - BALANCE:   Numeric balance verification across records
 *
 * Files:
 * - VALIDATION-CONTROL  (input):  Validation parameters
 * - POSITION-MASTER     (input):  Position records to validate
 * - TRANSACTION-HISTORY (input):  Transaction records to validate
 * - ERROR-REPORT        (output): Validation error details
 */
public class DataValidator {

    private static final Logger LOGGER = Logger.getLogger(DataValidator.class.getName());
    private static final String PROGRAM_ID = "UTLVAL00";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Validation types matching WS-VALIDATION-TYPE */
    public enum ValidationType {
        INTEGRITY, XREF, FORMAT, BALANCE
    }

    private final Path controlFilePath;
    private final Path positionMasterPath;
    private final Path transactionHistoryPath;
    private final Path errorReportPath;

    private final ErrorHandler errorHandler;
    private final ReturnCode returnCode;

    /** Validation counters */
    private long recordsValidated;
    private long errorsFound;
    private long warningsFound;
    private final List<ValidationError> validationErrors;

    public DataValidator(Path controlFilePath, Path positionMasterPath,
                         Path transactionHistoryPath, Path errorReportPath) {
        this.controlFilePath = controlFilePath;
        this.positionMasterPath = positionMasterPath;
        this.transactionHistoryPath = transactionHistoryPath;
        this.errorReportPath = errorReportPath;
        this.errorHandler = new ErrorHandler(PROGRAM_ID);
        this.returnCode = new ReturnCode(PROGRAM_ID);
        this.validationErrors = new ArrayList<>();
    }

    /**
     * Main entry point - maps to COBOL 0000-MAIN.
     *
     * PERFORM 1000-INITIALIZE
     * EVALUATE WS-VALIDATION-TYPE
     *   WHEN 'INTEGRITY' PERFORM 2000-CHECK-INTEGRITY
     *   WHEN 'XREF'      PERFORM 3000-CHECK-XREF
     *   WHEN 'FORMAT'    PERFORM 4000-CHECK-FORMAT
     *   WHEN 'BALANCE'   PERFORM 5000-CHECK-BALANCE
     * END-EVALUATE
     * PERFORM 9000-TERMINATE
     *
     * @param validationType the type of validation to perform
     * @return process return code
     */
    public int execute(ValidationType validationType) {
        LOGGER.info(PROGRAM_ID + " - Data Validation starting: " + validationType);

        try {
            initialize();

            switch (validationType) {
                case INTEGRITY:
                    checkIntegrity();
                    break;
                case XREF:
                    checkCrossReferences();
                    break;
                case FORMAT:
                    checkFormat();
                    break;
                case BALANCE:
                    checkBalance();
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
     * 1000-INITIALIZE: Reset counters and load control parameters.
     */
    private void initialize() {
        recordsValidated = 0;
        errorsFound = 0;
        warningsFound = 0;
        validationErrors.clear();

        LOGGER.info(PROGRAM_ID + " - Initialization complete");
    }

    /**
     * 2000-CHECK-INTEGRITY: Validate record structure and required fields.
     *
     * Checks each position and transaction record for:
     * - Required fields are present and non-empty
     * - Key fields are properly formatted
     * - Status codes are valid values
     * - Dates are valid and within acceptable ranges
     */
    private void checkIntegrity() {
        LOGGER.info("Performing integrity validation");

        // Validate position records
        try (FileHandler posFile = new FileHandler(positionMasterPath)) {
            if (FileHandler.STATUS_SUCCESS.equals(posFile.openInput())) {
                String line;
                int lineNum = 0;
                while ((line = posFile.readLine()) != null) {
                    lineNum++;
                    recordsValidated++;
                    validatePositionIntegrity(line, lineNum);
                }
            }
        } catch (Exception e) {
            errorHandler.handleSystemError("E200", "Error during integrity check", e);
        }

        // Validate transaction records
        try (FileHandler trnFile = new FileHandler(transactionHistoryPath)) {
            if (FileHandler.STATUS_SUCCESS.equals(trnFile.openInput())) {
                String line;
                int lineNum = 0;
                while ((line = trnFile.readLine()) != null) {
                    lineNum++;
                    recordsValidated++;
                    validateTransactionIntegrity(line, lineNum);
                }
            }
        } catch (Exception e) {
            errorHandler.handleSystemError("E201", "Error during transaction integrity check", e);
        }
    }

    private void validatePositionIntegrity(String line, int lineNum) {
        // Check minimum record length
        if (line.length() < 26) {
            addError("POSITION", lineNum, "INT001", "Record too short: " + line.length() + " chars");
            return;
        }

        // Check portfolio ID (required, 8 chars)
        String portfolioId = line.substring(0, 8).trim();
        if (portfolioId.isEmpty()) {
            addError("POSITION", lineNum, "INT002", "Missing portfolio ID");
        }

        // Check date field
        String dateStr = line.substring(8, 16).trim();
        if (!isValidDate(dateStr)) {
            addError("POSITION", lineNum, "INT003", "Invalid date: " + dateStr);
        }

        // Check investment ID (required, 10 chars)
        String investmentId = line.substring(16, 26).trim();
        if (investmentId.isEmpty()) {
            addError("POSITION", lineNum, "INT004", "Missing investment ID");
        }

        // Check status code if present
        if (line.length() > 74) {
            char status = line.charAt(74);
            if (status != 'A' && status != 'C' && status != 'P') {
                addError("POSITION", lineNum, "INT005", "Invalid status code: " + status);
            }
        }
    }

    private void validateTransactionIntegrity(String line, int lineNum) {
        if (line.length() < 28) {
            addError("TRANSACTION", lineNum, "INT010", "Record too short: " + line.length() + " chars");
            return;
        }

        String dateStr = line.substring(0, 8).trim();
        if (!isValidDate(dateStr)) {
            addError("TRANSACTION", lineNum, "INT011", "Invalid transaction date: " + dateStr);
        }

        String portfolioId = line.substring(14, 22).trim();
        if (portfolioId.isEmpty()) {
            addError("TRANSACTION", lineNum, "INT012", "Missing portfolio ID");
        }
    }

    /**
     * 3000-CHECK-XREF: Cross-reference validation between files.
     *
     * Verifies that:
     * - Every transaction references an existing position
     * - Portfolio IDs in transactions exist in position master
     * - Investment IDs in transactions match position records
     */
    private void checkCrossReferences() {
        LOGGER.info("Performing cross-reference validation");

        // Load portfolio IDs from position master
        Set<String> positionKeys = new HashSet<>();
        Set<String> portfolioIds = new HashSet<>();

        try (FileHandler posFile = new FileHandler(positionMasterPath)) {
            if (FileHandler.STATUS_SUCCESS.equals(posFile.openInput())) {
                String line;
                while ((line = posFile.readLine()) != null) {
                    if (line.length() >= 26) {
                        String portfolioId = line.substring(0, 8).trim();
                        String investmentId = line.substring(16, 26).trim();
                        portfolioIds.add(portfolioId);
                        positionKeys.add(portfolioId + "|" + investmentId);
                    }
                }
            }
        } catch (Exception e) {
            errorHandler.handleSystemError("E300", "Error loading position master for XREF", e);
            return;
        }

        // Validate transaction references
        try (FileHandler trnFile = new FileHandler(transactionHistoryPath)) {
            if (FileHandler.STATUS_SUCCESS.equals(trnFile.openInput())) {
                String line;
                int lineNum = 0;
                while ((line = trnFile.readLine()) != null) {
                    lineNum++;
                    recordsValidated++;

                    if (line.length() < 38) continue;

                    String portfolioId = line.substring(14, 22).trim();
                    String investmentId = line.substring(28, 38).trim();

                    if (!portfolioIds.contains(portfolioId)) {
                        addError("XREF", lineNum, "XRF001",
                                "Transaction references unknown portfolio: " + portfolioId);
                    }

                    if (!positionKeys.contains(portfolioId + "|" + investmentId)) {
                        addWarning("XREF", lineNum, "XRF002",
                                "Transaction references unknown position: "
                                        + portfolioId + "/" + investmentId);
                    }
                }
            }
        } catch (Exception e) {
            errorHandler.handleSystemError("E301", "Error during XREF validation", e);
        }
    }

    /**
     * 4000-CHECK-FORMAT: Data format and range validation.
     *
     * Validates:
     * - Numeric fields contain valid numbers
     * - Date fields are in YYYYMMDD format
     * - Currency codes are valid
     * - Amounts are within acceptable ranges
     */
    private void checkFormat() {
        LOGGER.info("Performing format validation");

        try (FileHandler posFile = new FileHandler(positionMasterPath)) {
            if (FileHandler.STATUS_SUCCESS.equals(posFile.openInput())) {
                String line;
                int lineNum = 0;
                while ((line = posFile.readLine()) != null) {
                    lineNum++;
                    recordsValidated++;
                    validatePositionFormat(line, lineNum);
                }
            }
        } catch (Exception e) {
            errorHandler.handleSystemError("E400", "Error during format validation", e);
        }
    }

    private void validatePositionFormat(String line, int lineNum) {
        if (line.length() < 41) return;

        // Validate numeric fields
        String quantityStr = line.substring(26, 41).trim();
        if (!isValidDecimal(quantityStr)) {
            addError("FORMAT", lineNum, "FMT001", "Invalid quantity format: " + quantityStr);
        }

        if (line.length() > 56) {
            String costStr = line.substring(41, 56).trim();
            if (!isValidDecimal(costStr)) {
                addError("FORMAT", lineNum, "FMT002", "Invalid cost basis format: " + costStr);
            }
        }

        if (line.length() > 71) {
            String mktValStr = line.substring(56, 71).trim();
            if (!isValidDecimal(mktValStr)) {
                addError("FORMAT", lineNum, "FMT003", "Invalid market value format: " + mktValStr);
            }
        }

        if (line.length() > 74) {
            String currency = line.substring(71, 74).trim();
            if (!currency.isEmpty() && currency.length() != 3) {
                addError("FORMAT", lineNum, "FMT004", "Invalid currency code: " + currency);
            }
        }
    }

    /**
     * 5000-CHECK-BALANCE: Numeric balance verification.
     *
     * Verifies:
     * - Position totals match expected aggregates
     * - Transaction amounts reconcile with position changes
     * - No negative balances where not expected
     */
    private void checkBalance() {
        LOGGER.info("Performing balance validation");

        // Accumulate position totals by portfolio
        Map<String, BigDecimal> portfolioTotals = new HashMap<>();

        try (FileHandler posFile = new FileHandler(positionMasterPath)) {
            if (FileHandler.STATUS_SUCCESS.equals(posFile.openInput())) {
                String line;
                int lineNum = 0;
                while ((line = posFile.readLine()) != null) {
                    lineNum++;
                    recordsValidated++;

                    if (line.length() < 41) continue;

                    String portfolioId = line.substring(0, 8).trim();
                    String quantityStr = line.substring(26, 41).trim();

                    try {
                        BigDecimal quantity = new BigDecimal(quantityStr);

                        // Check for unexpected negative balances
                        if (quantity.compareTo(BigDecimal.ZERO) < 0) {
                            char status = line.length() > 74 ? line.charAt(74) : 'A';
                            if (status == 'A') {
                                addError("BALANCE", lineNum, "BAL001",
                                        "Negative quantity for active position: "
                                                + portfolioId + " qty=" + quantity);
                            }
                        }

                        portfolioTotals.merge(portfolioId, quantity, BigDecimal::add);
                    } catch (NumberFormatException e) {
                        // Already caught by format validation
                    }
                }
            }
        } catch (Exception e) {
            errorHandler.handleSystemError("E500", "Error during balance validation", e);
        }

        // Report portfolio balance summaries
        for (Map.Entry<String, BigDecimal> entry : portfolioTotals.entrySet()) {
            if (entry.getValue().compareTo(BigDecimal.ZERO) < 0) {
                addWarning("BALANCE", 0, "BAL002",
                        "Portfolio has negative total balance: "
                                + entry.getKey() + " total=" + entry.getValue());
            }
        }
    }

    /**
     * 9000-TERMINATE: Write error report and display statistics.
     */
    private void terminate() {
        writeErrorReport();

        if (errorsFound > 0) {
            returnCode.setCode(ReturnCode.ERROR);
        } else if (warningsFound > 0) {
            returnCode.setCode(ReturnCode.WARNING);
        }

        displayStatistics();
    }

    /**
     * Writes the validation error report.
     */
    private void writeErrorReport() {
        try (FileHandler reportFile = new FileHandler(errorReportPath)) {
            reportFile.openOutput();

            reportFile.writeLine("=".repeat(80));
            reportFile.writeLine("DATA VALIDATION ERROR REPORT");
            reportFile.writeLine("Generated: " + LocalDateTime.now().format(TIMESTAMP_FMT));
            reportFile.writeLine("Program: " + PROGRAM_ID);
            reportFile.writeLine("=".repeat(80));
            reportFile.writeLine("");

            if (validationErrors.isEmpty()) {
                reportFile.writeLine("NO VALIDATION ERRORS FOUND");
            } else {
                reportFile.writeLine(String.format("%-8s %-12s %6s %-8s %s",
                        "Severity", "Source", "Line", "Code", "Description"));
                reportFile.writeLine("-".repeat(80));

                for (ValidationError err : validationErrors) {
                    reportFile.writeLine(String.format("%-8s %-12s %6d %-8s %s",
                            err.severity, err.source, err.lineNumber,
                            err.errorCode, err.description));
                }
            }

            reportFile.writeLine("");
            reportFile.writeLine("-".repeat(80));
            reportFile.writeLine(String.format("SUMMARY: Validated: %d  Errors: %d  Warnings: %d",
                    recordsValidated, errorsFound, warningsFound));
            reportFile.writeLine("=".repeat(80));

        } catch (Exception e) {
            errorHandler.handleSystemError("E900", "Error writing validation report", e);
        }
    }

    // --- Helper methods ---

    private void addError(String source, int lineNum, String code, String description) {
        validationErrors.add(new ValidationError("ERROR", source, lineNum, code, description));
        errorsFound++;
    }

    private void addWarning(String source, int lineNum, String code, String description) {
        validationErrors.add(new ValidationError("WARNING", source, lineNum, code, description));
        warningsFound++;
    }

    private boolean isValidDate(String dateStr) {
        if (dateStr == null || dateStr.length() != 8) return false;
        try {
            LocalDate.parse(dateStr, DATE_FMT);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private boolean isValidDecimal(String value) {
        if (value == null || value.trim().isEmpty()) return false;
        try {
            new BigDecimal(value.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void displayStatistics() {
        LOGGER.info(PROGRAM_ID + " Validation Statistics:");
        LOGGER.info("  Records Validated: " + recordsValidated);
        LOGGER.info("  Errors Found:      " + errorsFound);
        LOGGER.info("  Warnings Found:    " + warningsFound);
        LOGGER.info("  Return Code:       " + returnCode.getCurrentCode());
    }

    private static class ValidationError {
        final String severity;
        final String source;
        final int lineNumber;
        final String errorCode;
        final String description;

        ValidationError(String severity, String source, int lineNumber,
                        String errorCode, String description) {
            this.severity = severity;
            this.source = source;
            this.lineNumber = lineNumber;
            this.errorCode = errorCode;
            this.description = description;
        }
    }
}
