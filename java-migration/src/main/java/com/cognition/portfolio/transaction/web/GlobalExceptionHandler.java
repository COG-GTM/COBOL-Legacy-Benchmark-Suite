package com.cognition.portfolio.transaction.web;

import com.cognition.portfolio.transaction.exception.DuplicateTransactionException;
import com.cognition.portfolio.transaction.exception.TransactionNotFoundException;
import com.cognition.portfolio.transaction.exception.TransactionProcessingException;
import com.cognition.portfolio.transaction.exception.TransactionValidationException;
import com.cognition.portfolio.transaction.web.dto.ErrorResponse;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps the migrated error paths onto HTTP status codes. Response bodies carry the original COBOL
 * {@code ERR-TEXT} so a failure here can be matched against the legacy job log.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(TransactionNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(TransactionNotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.of(e.getMessage()));
  }

  @ExceptionHandler(DuplicateTransactionException.class)
  public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateTransactionException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(e.getMessage()));
  }

  @ExceptionHandler(TransactionValidationException.class)
  public ResponseEntity<ErrorResponse> handleValidation(TransactionValidationException e) {
    return ResponseEntity.badRequest()
        .body(
            new ErrorResponse(
                e.getOutcome().message(), e.getOutcome().ruleId(), e.getOutcome().cobolParagraph()));
  }

  @ExceptionHandler(TransactionProcessingException.class)
  public ResponseEntity<ErrorResponse> handleProcessing(TransactionProcessingException e) {
    return ResponseEntity.badRequest()
        .body(new ErrorResponse(e.getMessage(), e.getRuleId(), e.getCobolParagraph()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
    return ResponseEntity.badRequest().body(ErrorResponse.of(e.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleBeanValidation(MethodArgumentNotValidException e) {
    String message =
        e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining("; "));
    return ResponseEntity.badRequest().body(ErrorResponse.of(message));
  }
}
