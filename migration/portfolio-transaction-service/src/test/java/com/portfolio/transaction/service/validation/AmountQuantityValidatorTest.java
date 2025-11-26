package com.portfolio.transaction.service.validation;

import com.portfolio.transaction.domain.dto.TransactionRequest;
import com.portfolio.transaction.domain.dto.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class AmountQuantityValidatorTest {

    private AmountQuantityValidator validator;

    @BeforeEach
    void setUp() {
        validator = new AmountQuantityValidator();
    }

    @Test
    void shouldRejectWhenQuantityIsNull() {
        TransactionRequest request = new TransactionRequest(
            "PORT001", "BU", null, BigDecimal.TEN, BigDecimal.TEN);

        ValidationResult result = validator.validate(request);

        assertFalse(result.isValid());
        assertEquals("Quantity must be greater than zero", result.getErrorMessage());
    }

    @Test
    void shouldRejectWhenQuantityIsZero() {
        TransactionRequest request = new TransactionRequest(
            "PORT001", "BU", BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.TEN);

        ValidationResult result = validator.validate(request);

        assertFalse(result.isValid());
        assertEquals("Quantity must be greater than zero", result.getErrorMessage());
    }

    @Test
    void shouldRejectWhenQuantityIsNegative() {
        TransactionRequest request = new TransactionRequest(
            "PORT001", "BU", new BigDecimal("-10"), BigDecimal.TEN, BigDecimal.TEN);

        ValidationResult result = validator.validate(request);

        assertFalse(result.isValid());
        assertEquals("Quantity must be greater than zero", result.getErrorMessage());
    }

    @Test
    void shouldRejectWhenPriceIsZeroForNonTransfer() {
        TransactionRequest request = new TransactionRequest(
            "PORT001", "BU", BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN);

        ValidationResult result = validator.validate(request);

        assertFalse(result.isValid());
        assertEquals("Price must be greater than zero", result.getErrorMessage());
    }

    @Test
    void shouldRejectWhenAmountIsZeroForNonTransfer() {
        TransactionRequest request = new TransactionRequest(
            "PORT001", "BU", BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO);

        ValidationResult result = validator.validate(request);

        assertFalse(result.isValid());
        assertEquals("Amount must be greater than zero", result.getErrorMessage());
    }

    @Test
    void shouldAcceptTransferWithoutPriceAndAmount() {
        TransactionRequest request = new TransactionRequest(
            "PORT001", "TR", BigDecimal.TEN, null, null);

        ValidationResult result = validator.validate(request);

        assertTrue(result.isValid());
        assertNull(result.getErrorMessage());
    }

    @Test
    void shouldAcceptValidBuyTransaction() {
        TransactionRequest request = new TransactionRequest(
            "PORT001", "BU", BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN);

        ValidationResult result = validator.validate(request);

        assertTrue(result.isValid());
        assertNull(result.getErrorMessage());
    }

    @Test
    void shouldAcceptValidSellTransaction() {
        TransactionRequest request = new TransactionRequest(
            "PORT001", "SL", BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN);

        ValidationResult result = validator.validate(request);

        assertTrue(result.isValid());
        assertNull(result.getErrorMessage());
    }

    @Test
    void shouldAcceptValidFeeTransaction() {
        TransactionRequest request = new TransactionRequest(
            "PORT001", "FE", BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN);

        ValidationResult result = validator.validate(request);

        assertTrue(result.isValid());
        assertNull(result.getErrorMessage());
    }

    @Test
    void shouldHaveOrderThree() {
        assertEquals(3, validator.getOrder());
    }
}
