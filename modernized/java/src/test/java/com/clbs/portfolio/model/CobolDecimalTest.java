package com.clbs.portfolio.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Packed-decimal storage semantics: fixed scale, truncation, and silent high-order loss. */
class CobolDecimalTest {

    @Test
    @DisplayName("PIC S9(11)V9(4) fields always carry four decimals")
    void quantityScale() {
        assertEquals(4, CobolDecimal.quantity("100").scale());
        assertEquals(new BigDecimal("100.0000"), CobolDecimal.quantity("100"));
        assertEquals(new BigDecimal("0.0000"), CobolDecimal.quantity((BigDecimal) null));
    }

    @Test
    @DisplayName("PIC S9(13)V9(2) fields always carry two decimals")
    void amountScale() {
        assertEquals(2, CobolDecimal.amount("12500").scale());
        assertEquals(new BigDecimal("12500.00"), CobolDecimal.amount("12500"));
    }

    @Test
    @DisplayName("excess decimals are truncated toward zero, never rounded")
    void truncatesTowardZero() {
        assertEquals(new BigDecimal("1.23"), CobolDecimal.amount("1.239"));
        assertEquals(new BigDecimal("-1.23"), CobolDecimal.amount("-1.239"));
        assertEquals(new BigDecimal("0.9999"), CobolDecimal.quantity("0.99999"));
    }

    @Test
    @DisplayName("integer digits beyond the picture are dropped silently, keeping the sign")
    void truncatesHighOrderDigits() {
        // 14 integer digits into a PIC S9(13)V99 field: the leading 1 is lost.
        assertEquals(new BigDecimal("1234567890123.45"),
                CobolDecimal.amount("11234567890123.45"));
        assertEquals(new BigDecimal("-1234567890123.45"),
                CobolDecimal.amount("-11234567890123.45"));
        // 12 integer digits into a PIC S9(11)V9(4) field.
        assertEquals(new BigDecimal("23456789012.0000"),
                CobolDecimal.quantity("123456789012"));
    }

    @Test
    @DisplayName("the documented maxima fit their fields exactly")
    void documentedMaximaFit() {
        BigDecimal maxTransaction = new BigDecimal("99999999999.99");
        BigDecimal maxPortfolioValue = new BigDecimal("9999999999999.99");
        assertEquals(maxTransaction, CobolDecimal.amount(maxTransaction));
        assertEquals(maxPortfolioValue, CobolDecimal.amount(maxPortfolioValue));
    }

    @Test
    @DisplayName("IF field <= ZERO ignores scale")
    void notPositive() {
        assertTrue(CobolDecimal.isNotPositive(CobolDecimal.quantity("0")));
        assertTrue(CobolDecimal.isNotPositive(CobolDecimal.quantity("-0.0001")));
        assertTrue(CobolDecimal.isNotPositive(null));
        assertFalse(CobolDecimal.isNotPositive(CobolDecimal.quantity("0.0001")));
    }

    @Test
    @DisplayName("packed fields render as a sign plus their unscaled digits")
    void image() {
        assertEquals("+0000012500000", CobolDecimal.image(new BigDecimal("125000.00"), 11, 2));
        assertEquals("-0000000000001", CobolDecimal.image(new BigDecimal("-0.01"), 11, 2));
    }
}
