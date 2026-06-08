package com.portfolio.infrastructure.error;

import com.portfolio.domain.exception.InsufficientUnitsException;
import com.portfolio.domain.exception.PortfolioNotFoundException;
import com.portfolio.domain.exception.ValidationException;
import com.portfolio.domain.model.ErrorCategory;
import com.portfolio.domain.model.ErrorSeverity;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ErrorProcessorTest {

    private final ErrorProcessor processor = new ErrorProcessor();

    @Test
    void validationExceptionReturns400() {
        ValidationException ex = new ValidationException(101, "Invalid amount");

        ResponseEntity<ErrorResponse> response = processor.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("VL101", body.errorCode());
        assertEquals(ErrorCategory.VALIDATION, body.category());
        assertEquals(ErrorSeverity.ERROR, body.severity());
        assertEquals("Invalid amount", body.message());
        assertNotNull(body.timestamp());
    }

    @Test
    void insufficientUnitsExceptionReturns409() {
        InsufficientUnitsException ex = new InsufficientUnitsException(
                new BigDecimal("100"), new BigDecimal("50"));

        ResponseEntity<ErrorResponse> response = processor.handleInsufficientUnits(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("PR001", body.errorCode());
        assertEquals(ErrorCategory.PROCESSING, body.category());
        assertEquals(ErrorSeverity.ERROR, body.severity());
    }

    @Test
    void portfolioNotFoundExceptionReturns404() {
        PortfolioNotFoundException ex = new PortfolioNotFoundException("PORT999");

        ResponseEntity<ErrorResponse> response = processor.handleNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("VS001", body.errorCode());
        assertEquals(ErrorCategory.VSAM, body.category());
        assertTrue(body.message().contains("PORT999"));
    }

    @Test
    void entityNotFoundExceptionReturns404() {
        jakarta.persistence.EntityNotFoundException ex =
                new jakarta.persistence.EntityNotFoundException("Entity missing");

        ResponseEntity<ErrorResponse> response = processor.handleNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ErrorCategory.VSAM, response.getBody().category());
    }

    @Test
    void unsupportedOperationExceptionReturns501() {
        UnsupportedOperationException ex = new UnsupportedOperationException("Not yet");

        ResponseEntity<ErrorResponse> response = processor.handleUnsupported(ex);

        assertEquals(HttpStatus.NOT_IMPLEMENTED, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("PR002", body.errorCode());
        assertEquals(ErrorCategory.PROCESSING, body.category());
        assertEquals(ErrorSeverity.SEVERE, body.severity());
    }

    @Test
    void genericExceptionReturns500() {
        Exception ex = new RuntimeException("Something broke");

        ResponseEntity<ErrorResponse> response = processor.handleGeneric(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("SY001", body.errorCode());
        assertEquals(ErrorCategory.SYSTEM, body.category());
        assertEquals(ErrorSeverity.TERMINAL, body.severity());
        assertEquals("Internal server error", body.message());
    }
}
