package com.portfolio.portmstr.exception;

import com.portfolio.portmstr.dto.PortfolioResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler.
 * Replaces COBOL 9000-ERROR paragraph and CALL 'ERRPROC' error routing.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final int RC_WARNING = 4;
    private static final int RC_ERROR = 8;

    @ExceptionHandler(PortfolioNotFoundException.class)
    public ResponseEntity<PortfolioResponse> handleNotFound(PortfolioNotFoundException ex) {
        PortfolioResponse response = new PortfolioResponse(
                ex.getPortfolioId(), null, null, null, null,
                null, null, null, null, null, null, null,
                RC_WARNING, ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(DuplicatePortfolioException.class)
    public ResponseEntity<PortfolioResponse> handleDuplicate(DuplicatePortfolioException ex) {
        PortfolioResponse response = new PortfolioResponse(
                ex.getPortfolioId(), null, null, null, null,
                null, null, null, null, null, null, null,
                RC_ERROR, ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(PortfolioValidationException.class)
    public ResponseEntity<PortfolioResponse> handleValidation(PortfolioValidationException ex) {
        PortfolioResponse response = new PortfolioResponse(
                null, null, null, null, null,
                null, null, null, null, null, null, null,
                RC_ERROR, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(InsufficientUnitsException.class)
    public ResponseEntity<PortfolioResponse> handleInsufficientUnits(InsufficientUnitsException ex) {
        PortfolioResponse response = new PortfolioResponse(
                null, null, null, null, null,
                null, null, null, null, null, null, null,
                RC_ERROR, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<PortfolioResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        PortfolioResponse response = new PortfolioResponse(
                null, null, null, null, null,
                null, null, null, null, null, null, null,
                RC_ERROR, errorMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(BatchProcessingException.class)
    public ResponseEntity<PortfolioResponse> handleBatchProcessing(BatchProcessingException ex) {
        PortfolioResponse response = new PortfolioResponse(
                null, null, null, null, null,
                null, null, null, null, null, null, null,
                ex.getReturnCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
