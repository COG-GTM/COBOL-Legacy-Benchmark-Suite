package com.portfolio.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolationException;
import java.time.Instant;

/**
 * Global exception handler replacing the CICS error handling framework.
 *
 * In the original COBOL program (INQONLN.cbl, lines 40-44):
 * <pre>
 *     EXEC CICS HANDLE CONDITION
 *               ERROR(P900-ERROR-ROUTINE)
 *               PGMIDERR(P900-ERROR-ROUTINE)
 *               NOTFND(P900-ERROR-ROUTINE)
 *     END-EXEC.
 * </pre>
 *
 * The {@code EXEC CICS HANDLE CONDITION} registered a global error handler
 * (P900-ERROR-ROUTINE) that was invoked whenever CICS detected an error,
 * a program-not-found, or a record-not-found condition. In the Spring Boot
 * world, {@code @ControllerAdvice} serves the same purpose: it intercepts
 * exceptions thrown by any controller method and produces a structured
 * HTTP error response.
 *
 * P900-ERROR-ROUTINE (lines 119-137) populated the ERR-MESSAGE structure
 * and called the ERRHNDL program. Here, we build an {@link ErrorResponse}
 * directly and return it as JSON.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles PortfolioNotFoundException -> HTTP 404.
     * Replaces VSAM status '23' (ERR-VSAM-NOTFND) handling.
     */
    @ExceptionHandler(PortfolioNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(PortfolioNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
                Instant.now(),
                ex.getProgramId(),
                ex.getCategory(),
                ex.getErrorCode(),
                "WARNING",
                ex.getMessage(),
                "VSAM status 23 equivalent - record not found in data store"
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handles validation errors -> HTTP 400.
     * Replaces ERR-CAT-VALID ('VL') error category from ERRHAND.cpy.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ConstraintViolationException ex) {
        ErrorResponse error = new ErrorResponse(
                Instant.now(),
                "INQONLN",
                "VL",
                "0400",
                "ERROR",
                "Validation error",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles type mismatch errors (e.g. invalid date format) -> HTTP 400.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ErrorResponse error = new ErrorResponse(
                Instant.now(),
                "INQONLN",
                "VL",
                "0400",
                "ERROR",
                "Invalid parameter: " + ex.getName(),
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles general PortfolioException -> HTTP 500.
     * Replaces ERR-CAT-PROC ('PR') and ERR-CAT-SYSTEM ('SY') categories.
     */
    @ExceptionHandler(PortfolioException.class)
    public ResponseEntity<ErrorResponse> handlePortfolioException(PortfolioException ex) {
        ErrorResponse error = new ErrorResponse(
                Instant.now(),
                ex.getProgramId(),
                ex.getCategory(),
                ex.getErrorCode(),
                "ERROR",
                ex.getMessage(),
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Catch-all for unexpected exceptions -> HTTP 500.
     * Replaces the EXEC CICS ABEND ABCODE('IERR') path in P900-ERROR-ROUTINE (line 132).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        ErrorResponse error = new ErrorResponse(
                Instant.now(),
                "SYSTEM",
                "SY",
                "9999",
                "SEVERE",
                "An unexpected error occurred",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
