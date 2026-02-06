package com.clbs.repository;

import com.clbs.model.PositionHistoryRecord;
import com.clbs.exception.BatchProcessingException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Java equivalent of DB2 operations in HISTLD00.cbl
 * 
 * COBOL Original:
 * <pre>
 *     EXEC SQL
 *         INSERT INTO POSHIST
 *         VALUES (:POSHIST-RECORD)
 *     END-EXEC
 *     
 *     IF SQLCODE = 0
 *         ADD 1 TO WS-RECORDS-WRITTEN
 *     ELSE
 *         IF SQLCODE = -803
 *             CONTINUE
 *         ELSE
 *             ADD 1 TO WS-ERROR-COUNT
 *             PERFORM DB2-ERROR-ROUTINE
 *         END-IF
 *     END-IF
 * </pre>
 * 
 * Migration Notes:
 * - Embedded SQL converted to JDBC PreparedStatement
 * - SQLCODE handling converted to SQLException with vendor code checking
 * - SQLCODE -803 (duplicate key) handled as specific case
 * - Host variables (:field) converted to PreparedStatement parameters
 */
public class PositionHistoryRepository {

    private static final int SQLCODE_DUPLICATE_KEY = -803;
    private static final String PROGRAM_ID = "HISTLD00";

    private static final String INSERT_SQL = 
        "INSERT INTO POSHIST (" +
        "ACCOUNT_NO, PORTFOLIO_ID, TRANS_DATE, TRANS_TIME, TRANS_TYPE, " +
        "SECURITY_ID, QUANTITY, PRICE, AMOUNT, FEES, TOTAL_AMOUNT, " +
        "COST_BASIS, GAIN_LOSS, PROCESS_DATE, PROCESS_TIME, " +
        "PROGRAM_ID, USER_ID, AUDIT_TIMESTAMP" +
        ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private final Connection connection;

    public PositionHistoryRepository(Connection connection) {
        this.connection = connection;
    }

    public enum InsertResult {
        SUCCESS,
        DUPLICATE_KEY,
        ERROR
    }

    public InsertResult insert(PositionHistoryRecord record) throws BatchProcessingException {
        try (PreparedStatement stmt = connection.prepareStatement(INSERT_SQL)) {
            int paramIndex = 1;
            
            stmt.setString(paramIndex++, record.getAccountNo());
            stmt.setString(paramIndex++, record.getPortfolioId());
            stmt.setDate(paramIndex++, record.getTransDate() != null ? 
                java.sql.Date.valueOf(record.getTransDate()) : null);
            stmt.setTime(paramIndex++, record.getTransTime() != null ? 
                java.sql.Time.valueOf(record.getTransTime()) : null);
            stmt.setString(paramIndex++, record.getTransType());
            stmt.setString(paramIndex++, record.getSecurityId());
            stmt.setBigDecimal(paramIndex++, record.getQuantity());
            stmt.setBigDecimal(paramIndex++, record.getPrice());
            stmt.setBigDecimal(paramIndex++, record.getAmount());
            stmt.setBigDecimal(paramIndex++, record.getFees());
            stmt.setBigDecimal(paramIndex++, record.getTotalAmount());
            stmt.setBigDecimal(paramIndex++, record.getCostBasis());
            stmt.setBigDecimal(paramIndex++, record.getGainLoss());
            stmt.setDate(paramIndex++, record.getProcessDate() != null ? 
                java.sql.Date.valueOf(record.getProcessDate()) : null);
            stmt.setTime(paramIndex++, record.getProcessTime() != null ? 
                java.sql.Time.valueOf(record.getProcessTime()) : null);
            stmt.setString(paramIndex++, record.getProgramId());
            stmt.setString(paramIndex++, record.getUserId());
            stmt.setTimestamp(paramIndex++, record.getAuditTimestamp() != null ? 
                Timestamp.valueOf(record.getAuditTimestamp()) : null);

            stmt.executeUpdate();
            return InsertResult.SUCCESS;

        } catch (SQLException e) {
            if (e.getErrorCode() == SQLCODE_DUPLICATE_KEY || 
                e.getSQLState() != null && e.getSQLState().equals("23505")) {
                return InsertResult.DUPLICATE_KEY;
            }
            
            throw new BatchProcessingException(
                "DB2 insert failed: " + e.getMessage(),
                PROGRAM_ID,
                BatchProcessingException.Category.DATABASE,
                BatchProcessingException.Severity.ERROR,
                String.valueOf(e.getErrorCode()),
                e
            );
        }
    }

    public void commit() throws BatchProcessingException {
        try {
            connection.commit();
        } catch (SQLException e) {
            throw new BatchProcessingException(
                "Commit failed: " + e.getMessage(),
                PROGRAM_ID,
                BatchProcessingException.Category.DATABASE,
                BatchProcessingException.Severity.SEVERE,
                "COMMIT",
                e
            );
        }
    }

    public void rollback() {
        try {
            connection.rollback();
        } catch (SQLException e) {
            System.err.println("Rollback failed: " + e.getMessage());
        }
    }
}
