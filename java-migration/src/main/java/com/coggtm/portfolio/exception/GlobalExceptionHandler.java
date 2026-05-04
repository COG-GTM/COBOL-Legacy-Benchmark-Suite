package com.coggtm.portfolio.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global exception handler mapping the COBOL error hierarchy to HTTP status codes.
 * Maps to COBOL ERRHNDL / ERRPROC error dispatch logic.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(ValidationException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(PortfolioException.class)
    public ResponseEntity<Map<String, Object>> handlePortfolio(PortfolioException ex) {
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex);
    }

    @ExceptionHandler(SevereException.class)
    public ResponseEntity<Map<String, Object>> handleSevere(SevereException ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("returnCode", 16);
        body.put("message", "Unexpected error");
        body.put("detail", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, PortfolioException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("returnCode", ex.getReturnCode());
        body.put("message", ex.getMessage());
        return ResponseEntity.status(status).body(body);
    }
}
