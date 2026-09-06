package com.cog.portfolio.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Decision 3: scale 4 (TRNREC) -> scale 3 (POSHIST) with HALF_UP, precision loss
 * detected explicitly. HALF_UP itself is a technical assumption pending business
 * approval.
 */
class ScaleConverterTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "1.2345, 1.235",   // .5 rounds up (HALF_UP)
            "1.2344, 1.234",
            "1.2346, 1.235",
            "-1.2345, -1.235", // HALF_UP is symmetric: away from zero on .5
            "0.0005, 0.001",
            "0.0004, 0.000",
            "99999999999.9999, 100000000000.000",
            "1.2340, 1.234"
    })
    void toPoshistScaleRoundsHalfUp(String input, String expected) {
        BigDecimal result = ScaleConverter.toPoshistScale(new BigDecimal(input));
        assertThat(result).isEqualByComparingTo(expected);
        assertThat(result.scale()).isEqualTo(3);
    }

    @Test
    @DisplayName("the .5 boundary goes up, not to even (distinguishes HALF_UP from HALF_EVEN)")
    void halfUpNotHalfEven() {
        assertThat(ScaleConverter.toPoshistScale(new BigDecimal("2.0025"))).isEqualByComparingTo("2.003");
        assertThat(ScaleConverter.toPoshistScale(new BigDecimal("2.0035"))).isEqualByComparingTo("2.004");
        assertThat(new BigDecimal("2.0025").setScale(3, RoundingMode.HALF_EVEN)).isEqualByComparingTo("2.002");
    }

    @Test
    void residueIsReportedWhenFourthDecimalIsNonZero() {
        ScaleConverter.ScaleConversion c = ScaleConverter.convertToPoshistScale(new BigDecimal("10.1234"));
        assertThat(c.hasPrecisionLoss()).isTrue();
        assertThat(c.converted()).isEqualByComparingTo("10.123");
        assertThat(c.residue()).isEqualByComparingTo("0.0004");
        assertThat(c.original().subtract(c.residue())).isEqualByComparingTo(c.converted());
    }

    @Test
    void residueIsNegativeWhenRoundingUp() {
        ScaleConverter.ScaleConversion c = ScaleConverter.convertToPoshistScale(new BigDecimal("10.1235"));
        assertThat(c.hasPrecisionLoss()).isTrue();
        assertThat(c.converted()).isEqualByComparingTo("10.124");
        assertThat(c.residue()).isEqualByComparingTo("-0.0005");
    }

    @Test
    void noResidueWhenFourthDecimalIsZero() {
        ScaleConverter.ScaleConversion c = ScaleConverter.convertToPoshistScale(new BigDecimal("10.1230"));
        assertThat(c.hasPrecisionLoss()).isFalse();
        assertThat(c.residue()).isZero();
        assertThat(c.converted()).isEqualByComparingTo("10.123");
    }

    @Test
    void neverTruncates() {
        BigDecimal truncated = new BigDecimal("1.2349").setScale(3, RoundingMode.DOWN);
        BigDecimal converted = ScaleConverter.toPoshistScale(new BigDecimal("1.2349"));
        assertThat(converted).isNotEqualByComparingTo(truncated);
        assertThat(converted).isEqualByComparingTo("1.235");
    }

    @Test
    void domainScaleRejectsMoreThanFourDecimals() {
        assertThat(ScaleConverter.toDomainQuantityScale(new BigDecimal("1.5"))).isEqualByComparingTo("1.5000");
        assertThatThrownBy(() -> ScaleConverter.toDomainQuantityScale(new BigDecimal("1.23456")))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void policyConstantsAreHalfUpAndScaleThree() {
        assertThat(ScaleConverter.POSHIST_ROUNDING).isEqualTo(RoundingMode.HALF_UP);
        assertThat(ScaleConverter.POSHIST_SCALE).isEqualTo(3);
        assertThat(ScaleConverter.DOMAIN_QUANTITY_SCALE).isEqualTo(4);
    }

    @Test
    @DisplayName("ScaleConverter exposes no double/float in its API")
    void noFloatingPointInApi() {
        Class<?> c = ScaleConverter.class;
        for (Method m : c.getDeclaredMethods()) {
            assertThat(m.getReturnType()).isNotIn(double.class, float.class, Double.class, Float.class);
            assertThat(Arrays.asList(m.getParameterTypes()))
                    .doesNotContain(double.class, float.class, Double.class, Float.class);
        }
        for (Field f : c.getDeclaredFields()) {
            assertThat(f.getType()).isNotIn(double.class, float.class, Double.class, Float.class);
        }
    }
}
