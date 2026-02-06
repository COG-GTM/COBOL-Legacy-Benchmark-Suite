package com.clbs.batch;

import com.clbs.exception.BatchProcessingException;
import com.clbs.model.BatchControlRecord;
import com.clbs.model.PositionHistoryRecord;
import com.clbs.model.TransactionHistoryRecord;
import com.clbs.repository.PositionHistoryRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;

/**
 * Java translation of COBOL HISTLD00.cbl - Position History DB2 Load Program
 * 
 * This class demonstrates the migration of a COBOL batch program to Java,
 * preserving the original program structure and logic flow.
 * 
 * COBOL Original Structure:
 * <pre>
 *     0000-MAIN.
 *         PERFORM 1000-INITIALIZE
 *         PERFORM 2000-PROCESS UNTIL END-OF-FILE OR WS-ERROR-COUNT > 100
 *         PERFORM 3000-TERMINATE
 *         MOVE WS-ERROR-COUNT TO RETURN-CODE
 *         GOBACK
 * </pre>
 * 
 * Migration Notes:
 * - PROCEDURE DIVISION paragraphs converted to methods
 * - PERFORM...UNTIL converted to while loop
 * - Working-storage counters converted to instance fields
 * - FILE STATUS checking converted to exception handling
 * - GOBACK with RETURN-CODE converted to return int
 * - Checkpoint/restart pattern preserved with BatchControlRecord
 */
public class HistoryLoadProcessor {

    private static final String PROGRAM_ID = "HISTLD00";
    private static final int COMMIT_THRESHOLD = 1000;
    private static final int MAX_ERRORS = 100;

    private long recordsRead = 0;
    private long recordsWritten = 0;
    private long errorCount = 0;
    private int commitCount = 0;
    private boolean endOfFile = false;

    private final Connection connection;
    private final PositionHistoryRepository repository;
    private final BatchControlRecord batchControl;
    private Iterator<TransactionHistoryRecord> transactionHistoryIterator;
    private TransactionHistoryRecord currentRecord;

    public HistoryLoadProcessor(Connection connection) {
        this.connection = connection;
        this.repository = new PositionHistoryRepository(connection);
        this.batchControl = new BatchControlRecord();
    }

    /**
     * Main entry point - equivalent to 0000-MAIN paragraph
     * 
     * COBOL:
     * <pre>
     *     0000-MAIN.
     *         PERFORM 1000-INITIALIZE
     *         PERFORM 2000-PROCESS UNTIL END-OF-FILE OR WS-ERROR-COUNT > 100
     *         PERFORM 3000-TERMINATE
     *         MOVE WS-ERROR-COUNT TO RETURN-CODE
     *         GOBACK
     * </pre>
     */
    public int execute(Iterator<TransactionHistoryRecord> inputIterator) {
        this.transactionHistoryIterator = inputIterator;
        
        try {
            initialize();
            
            while (!endOfFile && errorCount <= MAX_ERRORS) {
                process();
            }
            
            terminate();
            
        } catch (BatchProcessingException e) {
            System.err.println(e.toString());
            repository.rollback();
            return e.getReturnCode();
        }
        
        return (int) errorCount;
    }

    /**
     * Initialization - equivalent to 1000-INITIALIZE paragraph
     * 
     * COBOL:
     * <pre>
     *     1000-INITIALIZE.
     *         PERFORM 1100-OPEN-FILES
     *         PERFORM 1200-CONNECT-DB2
     *         PERFORM 1300-INIT-CHECKPOINTS
     * </pre>
     */
    private void initialize() throws BatchProcessingException {
        initCheckpoints();
    }

    /**
     * Initialize checkpoints - equivalent to 1300-INIT-CHECKPOINTS paragraph
     * 
     * COBOL:
     * <pre>
     *     1300-INIT-CHECKPOINTS.
     *         MOVE SPACES TO BCT-KEY
     *         MOVE 'HISTLD00' TO BCT-JOB-NAME
     *         READ BATCH-CONTROL-FILE
     *             INVALID KEY
     *                 MOVE 'Control record not found' TO ERR-TEXT
     *                 PERFORM 9000-ERROR-ROUTINE
     *         END-READ
     *         MOVE BCT-STAT-ACTIVE TO BCT-STATUS
     *         REWRITE BATCH-CONTROL-RECORD
     * </pre>
     */
    private void initCheckpoints() {
        batchControl.setJobName(PROGRAM_ID);
        batchControl.setProcessDate(LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE));
        batchControl.setStatus(BatchControlRecord.Status.ACTIVE);
        batchControl.setAttemptTimestamp(LocalDateTime.now());
    }

    /**
     * Main processing loop - equivalent to 2000-PROCESS paragraph
     * 
     * COBOL:
     * <pre>
     *     2000-PROCESS.
     *         PERFORM 2100-READ-HISTORY
     *         IF MORE-RECORDS
     *             PERFORM 2200-LOAD-TO-DB2
     *             PERFORM 2300-CHECK-COMMIT
     *         END-IF
     * </pre>
     */
    private void process() throws BatchProcessingException {
        readHistory();
        
        if (!endOfFile) {
            loadToDb2();
            checkCommit();
        }
    }

    /**
     * Read history record - equivalent to 2100-READ-HISTORY paragraph
     * 
     * COBOL:
     * <pre>
     *     2100-READ-HISTORY.
     *         READ TRANSACTION-HISTORY
     *             AT END
     *                 SET END-OF-FILE TO TRUE
     *             NOT AT END
     *                 ADD 1 TO WS-RECORDS-READ
     *         END-READ
     * </pre>
     */
    private void readHistory() {
        if (transactionHistoryIterator.hasNext()) {
            currentRecord = transactionHistoryIterator.next();
            recordsRead++;
        } else {
            endOfFile = true;
        }
    }

    /**
     * Load record to DB2 - equivalent to 2200-LOAD-TO-DB2 paragraph
     * 
     * COBOL:
     * <pre>
     *     2200-LOAD-TO-DB2.
     *         INITIALIZE POSHIST-RECORD
     *         MOVE TH-ACCOUNT-NO    TO PH-ACCOUNT-NO
     *         MOVE TH-PORTFOLIO-ID  TO PH-PORTFOLIO-ID
     *         ... (field mappings)
     *         EXEC SQL
     *             INSERT INTO POSHIST VALUES (:POSHIST-RECORD)
     *         END-EXEC
     *         IF SQLCODE = 0
     *             ADD 1 TO WS-RECORDS-WRITTEN
     *         ELSE
     *             IF SQLCODE = -803
     *                 CONTINUE
     *             ELSE
     *                 ADD 1 TO WS-ERROR-COUNT
     *                 PERFORM DB2-ERROR-ROUTINE
     *             END-IF
     *         END-IF
     * </pre>
     */
    private void loadToDb2() throws BatchProcessingException {
        PositionHistoryRecord posHistRecord = new PositionHistoryRecord();
        
        posHistRecord.setAccountNo(currentRecord.getAccountNo());
        posHistRecord.setPortfolioId(currentRecord.getPortfolioId());
        posHistRecord.setTransDate(parseDate(currentRecord.getHistDate()));
        posHistRecord.setTransTime(parseTime(currentRecord.getHistTime()));
        posHistRecord.setTransType(currentRecord.getTransType());
        posHistRecord.setSecurityId(currentRecord.getSecurityId());
        posHistRecord.setQuantity(currentRecord.getQuantity());
        posHistRecord.setPrice(currentRecord.getPrice());
        posHistRecord.setAmount(currentRecord.getAmount());
        posHistRecord.setFees(currentRecord.getFees());
        posHistRecord.setTotalAmount(currentRecord.getTotalAmount());
        posHistRecord.setCostBasis(currentRecord.getCostBasis());
        posHistRecord.setGainLoss(currentRecord.getGainLoss());
        posHistRecord.setProcessDate(LocalDate.now());
        posHistRecord.setProcessTime(LocalTime.now());
        posHistRecord.setProgramId(PROGRAM_ID);
        posHistRecord.setAuditTimestamp(LocalDateTime.now());

        PositionHistoryRepository.InsertResult result = repository.insert(posHistRecord);
        
        switch (result) {
            case SUCCESS:
                recordsWritten++;
                break;
            case DUPLICATE_KEY:
                break;
            case ERROR:
                errorCount++;
                break;
        }
    }

    /**
     * Check commit threshold - equivalent to 2300-CHECK-COMMIT paragraph
     * 
     * COBOL:
     * <pre>
     *     2300-CHECK-COMMIT.
     *         ADD 1 TO WS-COMMIT-COUNT
     *         IF WS-COMMIT-COUNT >= WS-COMMIT-THRESHOLD
     *             EXEC SQL COMMIT WORK END-EXEC
     *             MOVE 0 TO WS-COMMIT-COUNT
     *             PERFORM 2310-UPDATE-CHECKPOINT
     *         END-IF
     * </pre>
     */
    private void checkCommit() throws BatchProcessingException {
        commitCount++;
        
        if (commitCount >= COMMIT_THRESHOLD) {
            repository.commit();
            commitCount = 0;
            updateCheckpoint();
        }
    }

    /**
     * Update checkpoint - equivalent to 2310-UPDATE-CHECKPOINT paragraph
     * 
     * COBOL:
     * <pre>
     *     2310-UPDATE-CHECKPOINT.
     *         MOVE WS-RECORDS-READ TO BCT-RECORDS-READ
     *         MOVE WS-RECORDS-WRITTEN TO BCT-RECORDS-WRITTEN
     *         REWRITE BATCH-CONTROL-RECORD
     * </pre>
     */
    private void updateCheckpoint() {
        batchControl.setRecordsRead(recordsRead);
        batchControl.setRecordsWritten(recordsWritten);
    }

    /**
     * Termination - equivalent to 3000-TERMINATE paragraph
     * 
     * COBOL:
     * <pre>
     *     3000-TERMINATE.
     *         PERFORM 3100-FINAL-COMMIT
     *         PERFORM 3200-CLOSE-FILES
     *         PERFORM 3300-DISCONNECT-DB2
     *         PERFORM 3400-DISPLAY-STATS
     * </pre>
     */
    private void terminate() throws BatchProcessingException {
        finalCommit();
        displayStats();
        
        batchControl.setStatus(errorCount > 0 ? 
            BatchControlRecord.Status.ERROR : BatchControlRecord.Status.DONE);
        batchControl.setCompleteTimestamp(LocalDateTime.now());
    }

    /**
     * Final commit - equivalent to 3100-FINAL-COMMIT paragraph
     * 
     * COBOL:
     * <pre>
     *     3100-FINAL-COMMIT.
     *         EXEC SQL COMMIT WORK END-EXEC
     *         PERFORM 2310-UPDATE-CHECKPOINT
     * </pre>
     */
    private void finalCommit() throws BatchProcessingException {
        repository.commit();
        updateCheckpoint();
    }

    /**
     * Display statistics - equivalent to 3400-DISPLAY-STATS paragraph
     * 
     * COBOL:
     * <pre>
     *     3400-DISPLAY-STATS.
     *         DISPLAY 'HISTLD00 Processing Statistics:'
     *         DISPLAY '  Records Read:    ' WS-RECORDS-READ
     *         DISPLAY '  Records Written: ' WS-RECORDS-WRITTEN
     *         DISPLAY '  Errors:         ' WS-ERROR-COUNT
     * </pre>
     */
    private void displayStats() {
        System.out.println("HISTLD00 Processing Statistics:");
        System.out.println("  Records Read:    " + recordsRead);
        System.out.println("  Records Written: " + recordsWritten);
        System.out.println("  Errors:          " + errorCount);
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.BASIC_ISO_DATE);
        } catch (Exception e) {
            return null;
        }
    }

    private LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HHmmss"));
        } catch (Exception e) {
            return null;
        }
    }

    public long getRecordsRead() {
        return recordsRead;
    }

    public long getRecordsWritten() {
        return recordsWritten;
    }

    public long getErrorCount() {
        return errorCount;
    }

    public BatchControlRecord getBatchControl() {
        return batchControl;
    }
}
