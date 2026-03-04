package com.cobolbenchmark.common;

import com.cobolbenchmark.model.ErrorSeverity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * Error Handling Service - migrated from ERRHNDL.cbl.
 * Centralized error logging to DB2 ERRLOG table and recovery determination.
 * Replaces DFHCOMMAREA-based error handling pattern.
 */
@Service
public class ErrorHandlingService {

    private static final Logger logger = LoggerFactory.getLogger(ErrorHandlingService.class);

    private final JdbcTemplate jdbcTemplate;

    public ErrorHandlingService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Log an error to the ERRLOG DB2 table.
     * From ERRHNDL.cbl: P200-LOG-ERROR paragraph.
     */
    public void logError(String programId, String errorCode, ErrorSeverity severity, String errorMessage) {
        try {
            jdbcTemplate.update(
                "INSERT INTO ERRLOG (PROGRAM_ID, ERROR_CODE, SEVERITY, ERROR_MESSAGE, ERROR_TIMESTAMP) " +
                "VALUES (?, ?, ?, ?, ?)",
                programId, errorCode, severity.getCode(), errorMessage,
                Timestamp.from(Instant.now())
            );
        } catch (Exception e) {
            // If we can't log to DB, at least log to application log
            logger.error("Failed to log error to ERRLOG table: {}", e.getMessage());
        }
        // Always log to application log as well
        switch (severity) {
            case INFO:
                logger.info("[{}] {}: {}", programId, errorCode, errorMessage);
                break;
            case WARNING:
                logger.warn("[{}] {}: {}", programId, errorCode, errorMessage);
                break;
            case ERROR:
            case SEVERE:
                logger.error("[{}] {}: {}", programId, errorCode, errorMessage);
                break;
        }
    }

    /**
     * Determine recovery action based on error severity.
     * From ERRHNDL.cbl: P300-DETERMINE-ACTION paragraph.
     */
    public RecoveryAction determineRecoveryAction(ErrorSeverity severity) {
        switch (severity) {
            case INFO:
            case WARNING:
                return RecoveryAction.CONTINUE;
            case ERROR:
                return RecoveryAction.RETRY;
            case SEVERE:
                return RecoveryAction.ABORT;
            default:
                return RecoveryAction.ABORT;
        }
    }

    /**
     * Recovery action types from ERRHNDL.cbl.
     */
    public enum RecoveryAction {
        CONTINUE,
        RETRY,
        ABORT
    }
}
