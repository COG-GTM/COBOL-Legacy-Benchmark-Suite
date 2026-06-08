package com.portfolio.infrastructure.error;

import com.portfolio.domain.exception.InsufficientUnitsException;
import com.portfolio.domain.exception.PortfolioNotFoundException;
import com.portfolio.domain.exception.ValidationException;
import com.portfolio.domain.model.ErrorCategory;
import com.portfolio.domain.model.ErrorSeverity;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Centralized exception handler mapping COBOL ERRPROC.cbl error categories
 * (ERRHAND.cpy) to HTTP responses.
 */
@ControllerAdvice
public class ErrorProcessor {

    private static final Logger log = LoggerFactory.getLogger(ErrorProcessor.class);

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        ErrorResponse response = ErrorResponse.of(
                "VL" + ex.getValidationCode(),
                ErrorCategory.VALIDATION,
                ErrorSeverity.ERROR,
                ex.getMessage(),
                "Validation code: " + ex.getValidationCode()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(InsufficientUnitsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientUnits(InsufficientUnitsException ex) {
        log.warn("Insufficient units: {}", ex.getMessage());
        ErrorResponse response = ErrorResponse.of(
                "PR001",
                ErrorCategory.PROCESSING,
                ErrorSeverity.ERROR,
                ex.getMessage(),
                "Requested: " + ex.getRequested() + ", Available: " + ex.getAvailable()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler({EntityNotFoundException.class, PortfolioNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex) {
        log.warn("Entity not found: {}", ex.getMessage());
        ErrorResponse response = ErrorResponse.of(
                "VS001",
                ErrorCategory.VSAM,
                ErrorSeverity.ERROR,
                ex.getMessage(),
                null
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ErrorResponse> handleUnsupported(UnsupportedOperationException ex) {
        log.warn("Unsupported operation: {}", ex.getMessage());
        ErrorResponse response = ErrorResponse.of(
                "PR002",
                ErrorCategory.PROCESSING,
                ErrorSeverity.SEVERE,
                ex.getMessage(),
                null
        );
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        ErrorResponse response = ErrorResponse.of(
                "SY001",
                ErrorCategory.SYSTEM,
                ErrorSeverity.TERMINAL,
                "Internal server error",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
