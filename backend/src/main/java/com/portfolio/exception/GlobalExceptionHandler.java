package com.portfolio.exception;

import com.portfolio.entity.ErrorLog;
import com.portfolio.repository.ErrorLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global exception handler - replaces COBOL ERRHNDL and ERRPROC programs.
 * Source: src/programs/online/ERRHNDL.cbl, src/programs/common/ERRPROC.cbl
 *
 * Maps error handling patterns:
 * - ERRHNDL: Online error processing, user message management, screen error display
 * - ERRPROC: Batch error processing, return code management, error logging
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ErrorLogRepository errorLogRepository;

    public GlobalExceptionHandler(ErrorLogRepository errorLogRepository) {
        this.errorLogRepository = errorLogRepository;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        logError("APP", 2, "E004", ex.getMessage(), "ResourceNotFound");
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(BatchProcessingException.class)
    public ResponseEntity<Map<String, Object>> handleBatchProcessing(BatchProcessingException ex) {
        logError("APP", 3, "E008", ex.getMessage(), "BatchProcessing");
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        logError("SEC", 2, "E001", "Authentication failed", "Security");
        return buildResponse(HttpStatus.UNAUTHORIZED, "Invalid username or password");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        logError("SEC", 3, "E002", "Access denied", "Security");
        return buildResponse(HttpStatus.FORBIDDEN, "Access denied");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        logError("APP", 2, "E003", message, "Validation");
        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        log.error("Unhandled exception", ex);
        logError("SYS", 4, "E999", ex.getMessage(), "System");
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred");
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return new ResponseEntity<>(body, status);
    }

    private void logError(String errorType, int severity, String errorCode, String message, String programId) {
        try {
            ErrorLog errorLog = new ErrorLog();
            errorLog.setErrorTimestamp(LocalDateTime.now());
            errorLog.setProgramId(programId);
            errorLog.setErrorType(errorType.substring(0, 1));
            errorLog.setErrorSeverity(severity);
            errorLog.setErrorCode(errorCode);
            errorLog.setErrorMessage(message != null && message.length() > 200 ? message.substring(0, 200) : (message != null ? message : "Unknown error"));
            errorLog.setProcessDate(LocalDate.now());
            errorLog.setProcessTime(LocalTime.now());
            errorLog.setUserId("SYSTEM");
            errorLogRepository.save(errorLog);
        } catch (Exception e) {
            log.error("Failed to log error to database", e);
        }
    }
}
