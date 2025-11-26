package com.portfolio.transaction.service.validation;

import com.portfolio.transaction.domain.dto.TransactionRequest;
import com.portfolio.transaction.domain.dto.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTypeValidatorTest {

    private TransactionTypeValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TransactionTypeValidator();
    }

    @ParameterizedTest
    @ValueSource(strings = {"BU", "SL", "TR", "FE", "bu", "sl", "tr", "fe"})
    void shouldAcceptValidTransactionTypes(String type) {
        TransactionRequest request = new TransactionRequest(
            "PORT001", type, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN);

        ValidationResult result = validator.validate(request);

        assertTrue(result.isValid());
        assertNull(result.getErrorMessage());
    }

    @Test
    void shouldRejectNullTransactionType() {
        TransactionRequest request = new TransactionRequest(
            "PORT001", null, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN);

        ValidationResult result = validator.validate(request);

        assertFalse(result.isValid());
        assertEquals("Invalid Transaction Type: null", result.getErrorMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"XX", "AB", "12", "BUY", "SELL"})
    void shouldRejectInvalidTransactionTypes(String type) {
        TransactionRequest request = new TransactionRequest(
            "PORT001", type, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN);

        ValidationResult result = validator.validate(request);

        assertFalse(result.isValid());
        assertEquals("Invalid Transaction Type: " + type, result.getErrorMessage());
    }

    @Test
    void shouldHaveOrderTwo() {
        assertEquals(2, validator.getOrder());
    }
}
