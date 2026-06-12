package com.clbs.common.cobol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Validates the COBOL fixed-width / COMP-3 DISPLAY translation helpers. */
class CobolFieldTest {

    @Test
    void alphanumericPadsAndTruncates() {
        assertThat(CobolField.alphanumeric("IBM", 10)).isEqualTo("IBM       ");
        assertThat(CobolField.alphanumeric("LONG-NAME-OVERFLOW", 4)).isEqualTo("LONG");
        assertThat(CobolField.parseAlphanumeric("IBM       ")).isEqualTo("IBM");
    }

    @Test
    void integerZeroPads() {
        assertThat(CobolField.integer(20240320, 8)).isEqualTo("20240320");
        assertThat(CobolField.integer(7, 8)).isEqualTo("00000007");
        assertThat(CobolField.parseInteger("00000007")).isEqualTo(7L);
    }

    @Test
    void numericRoundTrips() {
        String encoded = CobolField.numeric(new BigDecimal("12345678.99"), 13, 2);
        assertThat(encoded).hasSize(15).isEqualTo("000001234567899");
        assertThat(CobolField.parseNumeric(encoded, 2)).isEqualByComparingTo("12345678.99");
    }

    @Test
    void numericRejectsOverflow() {
        assertThatThrownBy(() -> CobolField.numeric(new BigDecimal("99999999999999.99"), 13, 2))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
