package com.investment.portfolio.batch;

import com.investment.portfolio.common.DatabaseManager;
import com.investment.portfolio.common.ErrorHandler;
import com.investment.portfolio.common.FileHandler;
import com.investment.portfolio.common.ReturnCode;
import com.investment.portfolio.model.CheckpointControl;
import com.investment.portfolio.model.CheckpointControl.CheckpointStatus;
import com.investment.portfolio.model.CheckpointControl.ProcessPhase;
import com.investment.portfolio.model.PositionHistoryDbRecord;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

/**
 * History Loader (HISTLD00) - Java equivalent of HISTLD00.cbl
 *
 * Original COBOL: src/programs/batch/HISTLD00.cbl
 *
 * Responsibilities:
 * - Reads transaction history from VSAM sequential file
 * - Inserts records into DB2 POSHIST table via JDBC
 * - Commits every 1000 records (checkpoint/restart pattern)
 * - Updates batch control record with progress
 *
 * DB2 table: POSHIST
 * SQL: INSERT INTO POSHIST (ACCOUNT_NO, PORTFOLIO_ID, TRANS_DATE, TRANS_TIME,
 *        TRANS_TYPE, SECURITY_ID, QUANTITY, PRICE, AMOUNT, FEES,
 *        TOTAL_AMOUNT, COST_BASIS, GAIN_LOSS, PROCESS_DATE, PROCESS_TIME,
 *        PROGRAM_ID, USER_ID, AUDIT_TIMESTAMP)
 *      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
 */
public class HistoryLoader {

    private static final Logger LOGGER = Logger.getLogger(HistoryLoader.class.getName());
    private static final String PROGRAM_ID = "HISTLD00";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** SQL insert for POSHIST table - matches HISTLD00.cbl EXEC SQL INSERT */
    private static final String INSERT_SQL =
            "INSERT INTO POSHIST " +
            "(ACCOUNT_NO, PORTFOLIO_ID, TRANS_DATE, TRANS_TIME, TRANS_TYPE, " +
            " SECURITY_ID, QUANTITY, PRICE, AMOUNT, FEES, " +
            " TOTAL_AMOUNT, COST_BASIS, GAIN_LOSS, PROCESS_DATE, PROCESS_TIME, " +
            " PROGRAM_ID, USER_ID, AUDIT_TIMESTAMP) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private final Path inputFilePath;
    private final Path controlFilePath;
    private final DatabaseManager dbManager;
    private final ErrorHandler errorHandler;
    private final ReturnCode returnCode;
    private final CheckpointControl checkpoint;

    /** Processing counters - maps to BCT-RECORDS-READ, BCT-RECORDS-WRITTEN */
    private long recordsRead;
    private long recordsWritten;
    private long recordsInError;
    private long duplicateCount;

    /**
     * Tracks inserts since last commit so we can adjust recordsWritten
     * if a rollback occurs (rollback undoes all uncommitted inserts).
     */
    private long recordsSinceLastCommit;

    /** Commit frequency - maps to COBOL WS-COMMIT-FREQUENCY VALUE 1000 */
    private static final int COMMIT_FREQUENCY = 1000;

    public HistoryLoader(Path inputFilePath, Path controlFilePath,
                         DatabaseManager dbManager) {
        this.inputFilePath = inputFilePath;
        this.controlFilePath = controlFilePath;
        this.dbManager = dbManager;
        this.errorHandler = new ErrorHandler(PROGRAM_ID);
        this.returnCode = new ReturnCode(PROGRAM_ID);
        this.checkpoint = new CheckpointControl();
        this.checkpoint.setProgramId(PROGRAM_ID);
    }

    /**
     * Main entry point - maps to COBOL 0000-MAIN.
     * PERFORM 1000-INITIALIZE
     * PERFORM 2000-PROCESS UNTIL WS-EOF = 'Y'
     * PERFORM 3000-TERMINATE
     */
    public int execute() {
        LOGGER.info(PROGRAM_ID + " - History Load starting");

        try {
            initialize();
            processRecords();
            terminate();
        } catch (Exception e) {
            errorHandler.handleSystemError("E999", "Unexpected error in " + PROGRAM_ID, e);
            returnCode.setCode(ReturnCode.SEVERE);
            dbManager.rollback();
        }

        LOGGER.info(PROGRAM_ID + " - Completed with return code: " + returnCode.getCurrentCode());
        return returnCode.getCurrentCode();
    }

    /**
     * 1000-INITIALIZE: Open files, connect to database, initialize counters.
     *
     * Maps to:
     *   OPEN INPUT TRANSACTION-HISTORY
     *   OPEN I-O BATCH-CONTROL-FILE
     *   PERFORM CONNECT-TO-DB2
     */
    private void initialize() {
        checkpoint.setStatus(CheckpointStatus.INITIAL);
        checkpoint.setPhase(ProcessPhase.INIT);
        checkpoint.setRunDate(LocalDate.now().format(DATE_FMT));

        recordsRead = 0;
        recordsWritten = 0;
        recordsInError = 0;
        duplicateCount = 0;
        recordsSinceLastCommit = 0;

        // Connect to database - maps to PERFORM CONNECT-TO-DB2
        dbManager.connect();

        LOGGER.info(PROGRAM_ID + " - Initialization complete");
    }

    /**
     * 2000-PROCESS: Main processing loop.
     *
     * Maps to:
     *   READ TRANSACTION-HISTORY INTO WS-TRANS-RECORD
     *     AT END SET WS-EOF TO TRUE
     *     NOT AT END
     *       PERFORM 2100-MOVE-FIELDS
     *       PERFORM 2200-LOAD-TO-DB2
     *       PERFORM 2300-CHECK-COMMIT
     *   END-READ
     */
    private void processRecords() {
        checkpoint.setStatus(CheckpointStatus.ACTIVE);
        checkpoint.setPhase(ProcessPhase.PROCESS);

        Connection conn = dbManager.getConnection();

        try (FileHandler inputFile = new FileHandler(inputFilePath);
             PreparedStatement insertStmt = conn.prepareStatement(INSERT_SQL)) {

            String inputStatus = inputFile.openInput();
            if (!FileHandler.STATUS_SUCCESS.equals(inputStatus)) {
                errorHandler.handleFileError(inputStatus, inputFilePath.toString());
                returnCode.setCode(ReturnCode.SEVERE);
                return;
            }

            String line;
            while ((line = inputFile.readLine()) != null) {
                recordsRead++;
                checkpoint.setRecordsRead(recordsRead);

                PositionHistoryDbRecord histRecord = parseHistoryRecord(line);
                if (histRecord == null) {
                    recordsInError++;
                    continue;
                }

                // 2200-LOAD-TO-DB2: Insert record
                boolean inserted = loadToDb2(insertStmt, histRecord);
                if (inserted) {
                    recordsWritten++;
                    recordsSinceLastCommit++;
                }

                // 2300-CHECK-COMMIT: Commit every COMMIT_FREQUENCY records
                if (recordsRead % COMMIT_FREQUENCY == 0) {
                    checkCommit();
                }

                // Check error threshold
                if (recordsInError > checkpoint.getMaxErrors()) {
                    LOGGER.severe("Error threshold exceeded: " + recordsInError);
                    returnCode.setCode(ReturnCode.ERROR);
                    break;
                }
            }

            // Final commit for remaining records
            dbManager.commit();
            LOGGER.info("Final commit after " + recordsWritten + " records");

        } catch (Exception e) {
            errorHandler.handleSystemError("E200", "Error during history load", e);
            returnCode.setCode(ReturnCode.SEVERE);
            dbManager.rollback();
        }
    }

    /**
     * 2200-LOAD-TO-DB2: Inserts a single record into POSHIST table.
     *
     * Maps to:
     *   EXEC SQL INSERT INTO POSHIST
     *     (ACCOUNT_NO, ..., AUDIT_TIMESTAMP)
     *     VALUES (:PH-ACCOUNT-NO, ..., CURRENT TIMESTAMP)
     *   END-EXEC
     *   EVALUATE SQLCODE
     *     WHEN 0 ADD 1 TO BCT-RECORDS-WRITTEN
     *     WHEN -803 ADD 1 TO WS-DUP-COUNT (duplicate key)
     *     WHEN OTHER PERFORM DB2-ERROR-ROUTINE
     *   END-EVALUATE
     */
    private boolean loadToDb2(PreparedStatement stmt, PositionHistoryDbRecord rec) {
        try {
            stmt.setString(1, rec.getAccountNumber());
            stmt.setString(2, rec.getPortfolioId());
            stmt.setString(3, rec.getTransactionDate());
            stmt.setString(4, rec.getTransactionTime());
            stmt.setString(5, rec.getTransactionType());
            stmt.setString(6, rec.getSecurityId());
            stmt.setBigDecimal(7, rec.getQuantity());
            stmt.setBigDecimal(8, rec.getPrice());
            stmt.setBigDecimal(9, rec.getAmount());
            stmt.setBigDecimal(10, rec.getFees());
            stmt.setBigDecimal(11, rec.getTotalAmount());
            stmt.setBigDecimal(12, rec.getCostBasis());
            stmt.setBigDecimal(13, rec.getGainLoss());
            stmt.setString(14, rec.getProcessDate());
            stmt.setString(15, rec.getProcessTime());
            stmt.setString(16, PROGRAM_ID);
            stmt.setString(17, rec.getUserId());
            stmt.setTimestamp(18, java.sql.Timestamp.valueOf(LocalDateTime.now()));

            stmt.executeUpdate();
            dbManager.resetRetryCount();
            return true;

        } catch (SQLException e) {
            String sqlState = e.getSQLState();

            // Duplicate key (SQLCODE -803 / SQLSTATE 23505)
            if (DatabaseManager.SQL_DUP_KEY.equals(sqlState)) {
                duplicateCount++;
                LOGGER.fine("Duplicate key for record: " + rec.getAccountNumber());
                return false;
            }

            // Retryable errors (deadlock/timeout)
            boolean shouldRetry = dbManager.handleSqlException(e, "INSERT POSHIST");
            if (shouldRetry) {
                return loadToDb2(stmt, rec); // Retry
            }

            // handleSqlException may have triggered a rollback, which undoes
            // all uncommitted inserts. Adjust recordsWritten accordingly.
            if (recordsSinceLastCommit > 0) {
                LOGGER.warning("Rollback detected: adjusting recordsWritten by -"
                        + recordsSinceLastCommit);
                recordsWritten -= recordsSinceLastCommit;
                recordsSinceLastCommit = 0;
            }

            recordsInError++;
            return false;
        }
    }

    /**
     * 2300-CHECK-COMMIT: Periodic commit with checkpoint update.
     *
     * Maps to:
     *   IF WS-RECORD-COUNT >= WS-COMMIT-FREQUENCY
     *     EXEC SQL COMMIT WORK END-EXEC
     *     MOVE WS-RECORDS-READ TO BCT-RECORDS-READ
     *     MOVE WS-RECORDS-WRITTEN TO BCT-RECORDS-WRITTEN
     *     REWRITE BCT-RECORD
     *   END-IF
     */
    private void checkCommit() {
        dbManager.commit();

        // Reset since-last-commit counter — these records are now durable
        recordsSinceLastCommit = 0;

        // Update checkpoint/batch control
        checkpoint.setRecordsRead(recordsRead);
        checkpoint.setRecordsProcessed(recordsWritten);
        checkpoint.setRecordsInError(recordsInError);

        LOGGER.info(String.format("Commit checkpoint: read=%d written=%d errors=%d duplicates=%d",
                recordsRead, recordsWritten, recordsInError, duplicateCount));
    }

    /**
     * 3000-TERMINATE: Disconnect from DB2, close files, display statistics.
     *
     * Maps to:
     *   PERFORM DISCONNECT-FROM-DB2
     *   CLOSE TRANSACTION-HISTORY
     *   CLOSE BATCH-CONTROL-FILE
     *   DISPLAY statistics
     */
    private void terminate() {
        checkpoint.setStatus(CheckpointStatus.COMPLETE);
        checkpoint.setPhase(ProcessPhase.TERMINATE);

        dbManager.disconnect();

        if (recordsInError > 0 && returnCode.getCurrentCode() < ReturnCode.WARNING) {
            returnCode.setCode(ReturnCode.WARNING);
        }

        displayStatistics();
    }

    /**
     * Parses a fixed-format history record.
     */
    private PositionHistoryDbRecord parseHistoryRecord(String line) {
        try {
            if (line == null || line.length() < 30) return null;

            PositionHistoryDbRecord rec = new PositionHistoryDbRecord();
            int pos = 0;

            rec.setAccountNumber(line.substring(pos, pos + 8).trim());    pos += 8;
            rec.setPortfolioId(line.substring(pos, pos + 10).trim());     pos += 10;
            rec.setTransactionDate(line.substring(pos, pos + 10).trim()); pos += 10;

            if (line.length() > pos + 8) {
                rec.setTransactionTime(line.substring(pos, pos + 8).trim()); pos += 8;
            }
            if (line.length() > pos + 2) {
                rec.setTransactionType(line.substring(pos, pos + 2).trim()); pos += 2;
            }
            if (line.length() > pos + 12) {
                rec.setSecurityId(line.substring(pos, pos + 12).trim());     pos += 12;
            }
            if (line.length() > pos + 15) {
                rec.setQuantity(parseBigDecimal(line.substring(pos, pos + 15))); pos += 15;
            }
            if (line.length() > pos + 15) {
                rec.setPrice(parseBigDecimal(line.substring(pos, pos + 15)));    pos += 15;
            }
            if (line.length() > pos + 15) {
                rec.setAmount(parseBigDecimal(line.substring(pos, pos + 15)));   pos += 15;
            }
            if (line.length() > pos + 15) {
                rec.setFees(parseBigDecimal(line.substring(pos, pos + 15)));     pos += 15;
            }

            // Compute derived fields
            BigDecimal amount = rec.getAmount() != null ? rec.getAmount() : BigDecimal.ZERO;
            BigDecimal fees = rec.getFees() != null ? rec.getFees() : BigDecimal.ZERO;
            rec.setTotalAmount(amount.add(fees));

            rec.setProcessDate(LocalDate.now().toString());
            rec.setProcessTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            rec.setUserId("BATCH");

            return rec;
        } catch (Exception e) {
            LOGGER.fine("Parse error: " + e.getMessage());
            return null;
        }
    }

    private BigDecimal parseBigDecimal(String value) {
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private void displayStatistics() {
        LOGGER.info(PROGRAM_ID + " Processing Statistics:");
        LOGGER.info("  Records Read:     " + recordsRead);
        LOGGER.info("  Records Written:  " + recordsWritten);
        LOGGER.info("  Records In Error: " + recordsInError);
        LOGGER.info("  Duplicate Count:  " + duplicateCount);
        LOGGER.info("  Return Code:      " + returnCode.getCurrentCode());
    }
}
