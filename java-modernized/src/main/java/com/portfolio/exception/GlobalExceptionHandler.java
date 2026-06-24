package com.portfolio.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centralized exception handling.
 * Maps application exceptions to appropriate HTTP status codes,
 * mirroring the COBOL return code structure from RETHND.cpy / ERRHAND.cpy.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PortfolioNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(PortfolioNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "E002", ex.getMessage());
    }

    @ExceptionHandler(DuplicatePortfolioException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(DuplicatePortfolioException ex) {
        return buildResponse(HttpStatus.CONFLICT, "E003", ex.getMessage());
    }

    @ExceptionHandler(InsufficientUnitsException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientUnits(InsufficientUnitsException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "E007", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return buildResponse(HttpStatus.BAD_REQUEST, "E008", errors);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "E001", ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status, String errorCode, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("errorCode", errorCode);
        body.put("message", message);
        return new ResponseEntity<>(body, status);
    }
}
