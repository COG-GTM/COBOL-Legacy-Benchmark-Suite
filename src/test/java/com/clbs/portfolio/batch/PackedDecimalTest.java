package com.clbs.portfolio.batch;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PackedDecimalTest {

    @ParameterizedTest
    @ValueSource(strings = {"0.00", "12345.67", "-42.10", "9999999999999.99", "-9999999999999.99"})
    void roundTrip(String value) {
        BigDecimal v = new BigDecimal(value);
        byte[] packed = PackedDecimal.encode(v, 8, 2);
        assertThat(packed).hasSize(8);
        assertThat(PackedDecimal.decode(packed, 0, 8, 2)).isEqualByComparingTo(v);
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345.67"})
    void encodesSignNibble(String value) {
        byte[] packed = PackedDecimal.encode(new BigDecimal(value), 8, 2);
        assertThat(packed[7] & 0x0F).isEqualTo(0x0C);
        assertThat(PackedDecimal.encode(new BigDecimal(value).negate(), 8, 2)[7] & 0x0F).isEqualTo(0x0D);
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345.67", "-42.10", "0"})
    void signedDisplayMatchesCobol(String value) {
        String display = PortfolioBatchService.signedAmount(new BigDecimal(value));
        assertThat(display).hasSize(17).matches("[+-]\\d{13}\\.\\d{2}");
    }
}
