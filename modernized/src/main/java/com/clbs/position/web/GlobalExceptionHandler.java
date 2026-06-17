package com.clbs.position.web;

import com.clbs.position.domain.InsufficientPositionException;
import com.clbs.position.domain.TransactionValidationException;
import com.clbs.position.domain.UnsupportedTransactionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Maps the domain exceptions ported from the COBOL error routines
 * ({@code PORTTRAN.cbl 9000-ERROR-ROUTINE}, data-dictionary error codes E001-E004)
 * to HTTP responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** E003/E004-style edit failures &rarr; 400 Bad Request. */
    @ExceptionHandler({TransactionValidationException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, String>> badRequest(RuntimeException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    /** E004 insufficient balance &rarr; 409 Conflict. */
    @ExceptionHandler(InsufficientPositionException.class)
    public ResponseEntity<Map<String, String>> conflict(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    /** Unimplemented transfer &rarr; 422 Unprocessable Entity. */
    @ExceptionHandler(UnsupportedTransactionException.class)
    public ResponseEntity<Map<String, String>> unsupported(RuntimeException ex) {
        return ResponseEntity.unprocessableEntity().body(Map.of("error", ex.getMessage()));
    }
}
