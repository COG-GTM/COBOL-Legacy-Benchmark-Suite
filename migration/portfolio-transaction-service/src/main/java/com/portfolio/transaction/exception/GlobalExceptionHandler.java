package com.portfolio.transaction.exception;

import com.portfolio.transaction.audit.ErrorLogService;
import com.portfolio.transaction.domain.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final ErrorLogService errorLogService;

    public GlobalExceptionHandler(ErrorLogService errorLogService) {
        this.errorLogService = errorLogService;
    }

    @ExceptionHandler(PortfolioNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePortfolioNotFound(PortfolioNotFoundException ex) {
        errorLogService.logError("PORTTRAN", ex.getErrorCode(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(InvalidTransactionTypeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidType(InvalidTransactionTypeException ex) {
        errorLogService.logError("PORTTRAN", ex.getErrorCode(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(InsufficientUnitsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientUnits(InsufficientUnitsException ex) {
        errorLogService.logError("PORTTRAN", ex.getErrorCode(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(new ErrorResponse(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
        errorLogService.logError("PORTTRAN", ex.getErrorCode(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedOperation(UnsupportedOperationException ex) {
        errorLogService.logError("PORTTRAN", "ERR_NOT_IMPLEMENTED", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body(new ErrorResponse("ERR_NOT_IMPLEMENTED", ex.getMessage()));
    }
}
