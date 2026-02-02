package com.portfolio.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for REST API
 * Migrated from COBOL ERRHNDL program
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PortfolioException.class)
    public ResponseEntity<Map<String, Object>> handlePortfolioException(PortfolioException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("message", ex.getMessage());
        response.put("errorCode", ex.getErrorCode().name());
        response.put("returnCode", ex.getReturnCode());
        response.put("category", ex.getErrorCode().getCategory());

        HttpStatus status = mapReturnCodeToHttpStatus(ex.getReturnCode());
        return new ResponseEntity<>(response, status);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("message", "Internal server error: " + ex.getMessage());
        response.put("errorCode", "SEVERE");
        response.put("returnCode", 12);

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private HttpStatus mapReturnCodeToHttpStatus(int returnCode) {
        return switch (returnCode) {
            case 0 -> HttpStatus.OK;
            case 4 -> HttpStatus.OK;
            case 8 -> HttpStatus.BAD_REQUEST;
            case 12 -> HttpStatus.INTERNAL_SERVER_ERROR;
            case 16 -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
