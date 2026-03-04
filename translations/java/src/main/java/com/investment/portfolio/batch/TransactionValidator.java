package com.investment.portfolio.batch;

import com.investment.portfolio.common.ErrorHandler;
import com.investment.portfolio.common.FileHandler;
import com.investment.portfolio.common.ReturnCode;
import com.investment.portfolio.model.BatchControlRecord;
import com.investment.portfolio.model.BatchControlRecord.BatchStatus;
import com.investment.portfolio.model.CheckpointControl;
import com.investment.portfolio.model.CheckpointControl.CheckpointStatus;
import com.investment.portfolio.model.CheckpointControl.ProcessPhase;
import com.investment.portfolio.model.TransactionRecord;
import com.investment.portfolio.model.TransactionRecord.TransactionStatus;
import com.investment.portfolio.model.TransactionRecord.TransactionType;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.logging.Logger;

/**
 * Transaction Validator (TRNVAL00) - Java equivalent of the COBOL
 * Transaction Validation batch program.
 *
 * Original COBOL: src/programs/batch/ (TRNMAIN / TRNVAL00)
 *
 * Responsibilities:
 * - Reads input transaction file
 * - Validates each transaction against business rules
 * - Writes valid transactions to output for position updates
 * - Logs rejected transactions with error details
 * - Supports checkpoint/restart for large volumes
 *
 * Validation rules (from data-dictionary.md Section 5.1):
 * - Account Number must be numeric and exist in customer master
 * - Fund ID must exist in fund master
 * - Transaction Date must not be future date
 * - Share Quantity must not be zero for BU/SL
 * - Amount must be non-zero for FE
 * - Price must be greater than zero for BU/SL
 */
public class TransactionValidator {

    private static final Logger LOGGER = Logger.getLogger(TransactionValidator.class.getName());
    private static final String PROGRAM_ID = "TRNVAL00";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final Path inputFilePath;
    private final Path validOutputPath;
    private final Path rejectOutputPath;
    private final Path controlFilePath;

    private final ErrorHandler errorHandler;
    private final ReturnCode returnCode;
    private final CheckpointControl checkpoint;

    /** Processing counters */
    private long recordsRead;
    private long recordsValid;
    private long recordsRejected;
    private long errorCount;

    /** Checkpoint threshold - maps to COBOL checkpoint every 1000 records */
    private static final int CHECKPOINT_FREQUENCY = 1000;

    public TransactionValidator(Path inputFilePath, Path validOutputPath,
                                Path rejectOutputPath, Path controlFilePath) {
        this.inputFilePath = inputFilePath;
        this.validOutputPath = validOutputPath;
        this.rejectOutputPath = rejectOutputPath;
        this.controlFilePath = controlFilePath;
        this.errorHandler = new ErrorHandler(PROGRAM_ID);
        this.returnCode = new ReturnCode(PROGRAM_ID);
        this.checkpoint = new CheckpointControl();
        this.checkpoint.setProgramId(PROGRAM_ID);
    }

    /**
     * Main entry point - equivalent to COBOL 0000-MAIN paragraph.
     *
     * @return process return code (0=success, 4=warnings, 8=errors, 12=severe)
     */
    public int execute() {
        LOGGER.info(PROGRAM_ID + " - Transaction Validation starting");

        try {
            initialize();
            process();
            terminate();
        } catch (Exception e) {
            errorHandler.handleSystemError("E999", "Unexpected error in " + PROGRAM_ID, e);
            returnCode.setCode(ReturnCode.SEVERE);
        }

        LOGGER.info(PROGRAM_ID + " - Completed with return code: " + returnCode.getCurrentCode());
        return returnCode.getCurrentCode();
    }

    /**
     * 1000-INITIALIZE equivalent.
     * Opens files, initializes checkpoint control, reads control record.
     */
    private void initialize() {
        checkpoint.setStatus(CheckpointStatus.INITIAL);
        checkpoint.setPhase(ProcessPhase.INIT);
        checkpoint.setRunDate(LocalDate.now().format(DATE_FMT));

        recordsRead = 0;
        recordsValid = 0;
        recordsRejected = 0;
        errorCount = 0;

        LOGGER.info(PROGRAM_ID + " - Initialization complete");
    }

    /**
     * 2000-PROCESS equivalent.
     * Main processing loop: reads, validates, and routes each transaction.
     */
    private void process() {
        checkpoint.setStatus(CheckpointStatus.ACTIVE);
        checkpoint.setPhase(ProcessPhase.READ);

        try (FileHandler inputFile = new FileHandler(inputFilePath);
             FileHandler validOutput = new FileHandler(validOutputPath);
             FileHandler rejectOutput = new FileHandler(rejectOutputPath)) {

            String inputStatus = inputFile.openInput();
            if (!FileHandler.STATUS_SUCCESS.equals(inputStatus)) {
                errorHandler.handleFileError(inputStatus, inputFilePath.toString());
                returnCode.setCode(ReturnCode.SEVERE);
                return;
            }

            validOutput.openOutput();
            rejectOutput.openOutput();

            String line;
            while ((line = inputFile.readLine()) != null) {
                recordsRead++;
                checkpoint.setRecordsRead(recordsRead);
                checkpoint.setPhase(ProcessPhase.PROCESS);

                TransactionRecord transaction = parseTransaction(line);
                if (transaction == null) {
                    recordsRejected++;
                    rejectOutput.writeLine(line + "|PARSE_ERROR");
                    continue;
                }

                ValidationResult result = validateTransaction(transaction);

                if (result.isValid()) {
                    recordsValid++;
                    transaction.setStatus(TransactionStatus.PENDING);
                    validOutput.writeLine(formatTransaction(transaction));
                } else {
                    recordsRejected++;
                    rejectOutput.writeLine(line + "|" + result.getErrorCode()
                            + "|" + result.getErrorMessage());
                    errorCount++;
                }

                // Checkpoint every N records
                if (recordsRead % CHECKPOINT_FREQUENCY == 0) {
                    takeCheckpoint();
                }

                // Abort if too many errors
                if (errorCount > checkpoint.getMaxErrors()) {
                    LOGGER.severe("Error threshold exceeded: " + errorCount);
                    returnCode.setCode(ReturnCode.ERROR);
                    break;
                }
            }

        } catch (Exception e) {
            errorHandler.handleSystemError("E100", "Error during processing", e);
            returnCode.setCode(ReturnCode.SEVERE);
        }
    }

    /**
     * 3000-TERMINATE equivalent.
     * Final checkpoint, close files, display statistics.
     */
    private void terminate() {
        checkpoint.setStatus(CheckpointStatus.COMPLETE);
        checkpoint.setPhase(ProcessPhase.TERMINATE);

        if (errorCount > 0 && returnCode.getCurrentCode() < ReturnCode.WARNING) {
            returnCode.setCode(ReturnCode.WARNING);
        }

        displayStatistics();
    }

    /**
     * Parses a fixed-format transaction record line into a TransactionRecord.
     * Maps field positions from TRNREC.cpy layout.
     */
    private TransactionRecord parseTransaction(String line) {
        try {
            if (line == null || line.length() < 28) {
                return null;
            }

            TransactionRecord trn = new TransactionRecord();
            int pos = 0;

            trn.setTransactionDate(line.substring(pos, pos + 8).trim());   pos += 8;
            trn.setTransactionTime(line.substring(pos, pos + 6).trim());   pos += 6;
            trn.setPortfolioId(line.substring(pos, pos + 8).trim());       pos += 8;
            trn.setSequenceNumber(line.substring(pos, pos + 6).trim());    pos += 6;

            if (line.length() > pos + 10) {
                trn.setInvestmentId(line.substring(pos, pos + 10).trim()); pos += 10;
            }
            if (line.length() > pos + 2) {
                String typeCode = line.substring(pos, pos + 2).trim();     pos += 2;
                trn.setType(TransactionType.fromCode(typeCode));
            }
            if (line.length() > pos + 15) {
                trn.setQuantity(new BigDecimal(line.substring(pos, pos + 15).trim())); pos += 15;
            }
            if (line.length() > pos + 15) {
                trn.setPrice(new BigDecimal(line.substring(pos, pos + 15).trim()));    pos += 15;
            }
            if (line.length() > pos + 15) {
                trn.setAmount(new BigDecimal(line.substring(pos, pos + 15).trim()));   pos += 15;
            }
            if (line.length() > pos + 3) {
                trn.setCurrency(line.substring(pos, pos + 3).trim());      pos += 3;
            }
            if (line.length() > pos) {
                trn.setStatus(TransactionStatus.fromCode(line.charAt(pos)));
            }

            return trn;
        } catch (Exception e) {
            LOGGER.fine("Parse error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Validates a transaction against business rules.
     * Maps to the COBOL validation paragraphs and rules from data-dictionary.md.
     */
    private ValidationResult validateTransaction(TransactionRecord trn) {
        // Rule: Transaction Date must not be future date
        try {
            LocalDate transDate = LocalDate.parse(trn.getTransactionDate(), DATE_FMT);
            if (transDate.isAfter(LocalDate.now())) {
                return new ValidationResult(false, "E001", "Future transaction date");
            }
        } catch (DateTimeParseException e) {
            return new ValidationResult(false, "E001", "Invalid transaction date format");
        }

        // Rule: Portfolio ID must be present
        if (trn.getPortfolioId() == null || trn.getPortfolioId().isEmpty()) {
            return new ValidationResult(false, "E002", "Missing portfolio ID");
        }

        // Rule: Investment ID must be present
        if (trn.getInvestmentId() == null || trn.getInvestmentId().isEmpty()) {
            return new ValidationResult(false, "E002", "Missing investment ID");
        }

        // Rule: Transaction type must be valid
        if (trn.getType() == null) {
            return new ValidationResult(false, "E003", "Invalid transaction type");
        }

        // Rule: Share Quantity must not be zero for BU/SL
        if ((trn.getType() == TransactionType.BUY || trn.getType() == TransactionType.SELL)
                && (trn.getQuantity() == null
                    || trn.getQuantity().compareTo(BigDecimal.ZERO) == 0)) {
            return new ValidationResult(false, "E004",
                    "Zero quantity for buy/sell transaction");
        }

        // Rule: Price must be greater than zero for BU/SL
        if ((trn.getType() == TransactionType.BUY || trn.getType() == TransactionType.SELL)
                && (trn.getPrice() == null
                    || trn.getPrice().compareTo(BigDecimal.ZERO) <= 0)) {
            return new ValidationResult(false, "E004",
                    "Invalid price for buy/sell transaction");
        }

        // Rule: Amount must be non-zero for FE
        if (trn.getType() == TransactionType.FEE
                && (trn.getAmount() == null
                    || trn.getAmount().compareTo(BigDecimal.ZERO) == 0)) {
            return new ValidationResult(false, "E004",
                    "Zero amount for fee transaction");
        }

        return new ValidationResult(true, null, null);
    }

    /**
     * Formats a validated transaction for output.
     */
    private String formatTransaction(TransactionRecord trn) {
        return String.format("%-8s%-6s%-8s%-6s%-10s%-2s%15s%15s%15s%-3s%c",
                trn.getTransactionDate(),
                trn.getTransactionTime(),
                trn.getPortfolioId(),
                trn.getSequenceNumber(),
                trn.getInvestmentId(),
                trn.getType().getCode(),
                trn.getQuantity(),
                trn.getPrice(),
                trn.getAmount(),
                trn.getCurrency() != null ? trn.getCurrency() : "USD",
                trn.getStatus().getCode());
    }

    /**
     * Takes a checkpoint.
     * Maps to checkpoint/restart pattern from CKPRST.cpy.
     */
    private void takeCheckpoint() {
        checkpoint.setRecordsRead(recordsRead);
        checkpoint.setRecordsProcessed(recordsValid);
        checkpoint.setRecordsInError(recordsRejected);
        LOGGER.info(String.format("Checkpoint at record %d: valid=%d rejected=%d",
                recordsRead, recordsValid, recordsRejected));
    }

    /**
     * Displays processing statistics.
     * Maps to DISPLAY statements in COBOL 3000-TERMINATE.
     */
    private void displayStatistics() {
        LOGGER.info(PROGRAM_ID + " Processing Statistics:");
        LOGGER.info("  Records Read:     " + recordsRead);
        LOGGER.info("  Records Valid:    " + recordsValid);
        LOGGER.info("  Records Rejected: " + recordsRejected);
        LOGGER.info("  Error Count:      " + errorCount);
        LOGGER.info("  Return Code:      " + returnCode.getCurrentCode());
    }

    /**
     * Validation result container.
     */
    private static class ValidationResult {
        private final boolean valid;
        private final String errorCode;
        private final String errorMessage;

        ValidationResult(boolean valid, String errorCode, String errorMessage) {
            this.valid = valid;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }

        boolean isValid() { return valid; }
        String getErrorCode() { return errorCode; }
        String getErrorMessage() { return errorMessage; }
    }
}
