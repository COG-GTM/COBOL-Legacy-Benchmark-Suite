package com.clbs.portfolio.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global exception handler providing consistent error responses.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BatchProcessingException.class)
    public ResponseEntity<Map<String, Object>> handleBatchProcessingException(BatchProcessingException ex) {
        log.error("Batch processing error in program {}: {} (severity: {})",
                ex.getProgramId(), ex.getMessage(), ex.getSeverity());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("error", "Batch Processing Error");
        body.put("message", ex.getMessage());
        body.put("severity", ex.getSeverity().name());
        body.put("programId", ex.getProgramId());
        body.put("returnCode", ex.getSeverity().getCode());

        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(VsamFileException.class)
    public ResponseEntity<Map<String, Object>> handleVsamFileException(VsamFileException ex) {
        log.error("VSAM file error on {}: {} (status: {})",
                ex.getFileName(), ex.getMessage(), ex.getFileStatus());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("error", "Data Access Error");
        body.put("message", ex.getMessage());
        body.put("fileStatus", ex.getFileStatus());
        body.put("fileName", ex.getFileName());

        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("error", "Internal Server Error");
        body.put("message", ex.getMessage());

        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
