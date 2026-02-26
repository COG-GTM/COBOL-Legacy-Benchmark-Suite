package com.portfolio.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Component;

/**
 * DB2 Error Handler.
 * Migrated from COBOL DB2ERR program.
 * Maps SQLCODE errors to appropriate Java exceptions and return codes.
 */
@Component
public class Db2ErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(Db2ErrorHandler.class);

    /**
     * Handles a DataAccessException and returns the appropriate return code.
     * Mirrors COBOL DB2ERR SQLCODE error mapping.
     */
    public int handleError(DataAccessException ex, String programId, String operation) {
        log.error("DB2 error in program={} operation={}: {}", programId, operation, ex.getMessage());

        if (ex instanceof DuplicateKeyException) {
            log.warn("Duplicate key error (SQLCODE -803 equivalent)");
            return BatchExceptions.RC_WARNING;
        }

        if (ex instanceof EmptyResultDataAccessException) {
            log.warn("No data found (SQLCODE +100 equivalent)");
            return BatchExceptions.RC_WARNING;
        }

        if (ex instanceof DeadlockLoserDataAccessException) {
            log.error("Deadlock detected (SQLCODE -911 equivalent)");
            return BatchExceptions.RC_ERROR;
        }

        // Default: severe error
        log.error("Unhandled DB2 error: {}", ex.getClass().getSimpleName());
        return BatchExceptions.RC_SEVERE;
    }

    /**
     * Maps a SQLCODE-equivalent to a descriptive message.
     */
    public String getErrorMessage(DataAccessException ex) {
        if (ex instanceof DuplicateKeyException) {
            return "Duplicate record key";
        }
        if (ex instanceof EmptyResultDataAccessException) {
            return "Record not found";
        }
        if (ex instanceof DeadlockLoserDataAccessException) {
            return "Deadlock detected - retry may be needed";
        }
        return "Unexpected database error: " + ex.getMessage();
    }
}
