package com.portfolio.service.common;

import com.portfolio.exception.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.stereotype.Service;

/**
 * Database Error Handler - migrated from COBOL DB2ERR.cbl.
 * Translates SQLException/DataAccessException to domain exceptions.
 * Deadlock -> Spring retry; SQLCODE -803 (duplicate) -> DataIntegrityViolationException.
 */
@Service
public class DatabaseErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(DatabaseErrorHandler.class);

    private final ErrorProcessingService errorProcessingService;

    public DatabaseErrorHandler(ErrorProcessingService errorProcessingService) {
        this.errorProcessingService = errorProcessingService;
    }

    public DatabaseException handleDatabaseError(DataAccessException ex, String operation) {
        String errorCode;
        String message;

        if (ex instanceof DataIntegrityViolationException) {
            errorCode = "E003";
            message = "Duplicate record or constraint violation during " + operation;
        } else if (ex instanceof DeadlockLoserDataAccessException) {
            errorCode = "E005";
            message = "Deadlock detected during " + operation + " - retry recommended";
        } else {
            errorCode = "E005";
            message = "Database error during " + operation + ": " + ex.getMessage();
        }

        log.error("Database error [{}]: {}", errorCode, message, ex);
        errorProcessingService.processError("DB2ERR", "DB", errorCode, 3, message,
                ex.getMessage());

        return new DatabaseException(message, ex);
    }
}
