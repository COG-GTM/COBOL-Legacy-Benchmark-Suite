package com.portfolio.online;

import com.portfolio.support.BatchExceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Global Exception Handler.
 * Migrated from COBOL ERRHNDL (online error handler).
 * Replaces ERRMAP BMS screen with structured JSON error responses.
 * Applied via @ControllerAdvice to all REST endpoints.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(buildErrorResponse("ACCESS_DENIED", "Access denied", ex.getMessage(), 8));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDataAccessException(DataAccessException ex) {
        log.error("Database error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorResponse("DB_ERROR", "Database error", ex.getMessage(), 12));
    }

    @ExceptionHandler(BatchExceptions.BatchWarningException.class)
    public ResponseEntity<Map<String, Object>> handleBatchWarning(BatchExceptions.BatchWarningException ex) {
        log.warn("Batch warning: {}", ex.getMessage());
        return ResponseEntity.ok(buildErrorResponse("WARNING", ex.getMessage(), null, 4));
    }

    @ExceptionHandler(BatchExceptions.BatchErrorException.class)
    public ResponseEntity<Map<String, Object>> handleBatchError(BatchExceptions.BatchErrorException ex) {
        log.error("Batch error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorResponse("BATCH_ERROR", ex.getMessage(), null, 8));
    }

    @ExceptionHandler(BatchExceptions.BatchSevereException.class)
    public ResponseEntity<Map<String, Object>> handleBatchSevere(BatchExceptions.BatchSevereException ex) {
        log.error("Batch severe error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorResponse("SEVERE_ERROR", ex.getMessage(), null, 12));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Invalid request: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(buildErrorResponse("INVALID_REQUEST", ex.getMessage(), null, 4));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorResponse("INTERNAL_ERROR", "An unexpected error occurred",
                        ex.getMessage(), 12));
    }

    /**
     * Build a structured error response (replaces ERRMAP BMS screen).
     */
    private Map<String, Object> buildErrorResponse(String errorCode, String message,
                                                    String details, int returnCode) {
        return Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "errorCode", errorCode,
                "message", message != null ? message : "",
                "details", details != null ? details : "",
                "returnCode", returnCode
        );
    }
}
