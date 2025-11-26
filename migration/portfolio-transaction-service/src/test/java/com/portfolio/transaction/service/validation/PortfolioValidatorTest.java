package com.portfolio.transaction.service.validation;

import com.portfolio.transaction.domain.dto.TransactionRequest;
import com.portfolio.transaction.domain.dto.ValidationResult;
import com.portfolio.transaction.repository.PortfolioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioValidatorTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    private PortfolioValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PortfolioValidator(portfolioRepository);
    }

    @Test
    void shouldRejectWhenPortfolioIdIsNull() {
        TransactionRequest request = new TransactionRequest(
            null, "BU", BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN);

        ValidationResult result = validator.validate(request);

        assertFalse(result.isValid());
        assertEquals("Portfolio ID is required", result.getErrorMessage());
    }

    @Test
    void shouldRejectWhenPortfolioIdIsBlank() {
        TransactionRequest request = new TransactionRequest(
            "   ", "BU", BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN);

        ValidationResult result = validator.validate(request);

        assertFalse(result.isValid());
        assertEquals("Portfolio ID is required", result.getErrorMessage());
    }

    @Test
    void shouldRejectWhenPortfolioDoesNotExist() {
        TransactionRequest request = new TransactionRequest(
            "PORT001", "BU", BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN);
        when(portfolioRepository.existsById("PORT001")).thenReturn(false);

        ValidationResult result = validator.validate(request);

        assertFalse(result.isValid());
        assertEquals("Invalid Portfolio ID: PORT001", result.getErrorMessage());
    }

    @Test
    void shouldAcceptWhenPortfolioExists() {
        TransactionRequest request = new TransactionRequest(
            "PORT001", "BU", BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN);
        when(portfolioRepository.existsById("PORT001")).thenReturn(true);

        ValidationResult result = validator.validate(request);

        assertTrue(result.isValid());
        assertNull(result.getErrorMessage());
    }

    @Test
    void shouldHaveOrderOne() {
        assertEquals(1, validator.getOrder());
    }
}
