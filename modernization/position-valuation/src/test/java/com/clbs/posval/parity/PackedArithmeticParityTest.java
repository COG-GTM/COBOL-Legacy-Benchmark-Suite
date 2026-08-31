package com.clbs.posval.parity;

import static org.assertj.core.api.Assertions.assertThat;

import com.clbs.posval.cobol.CobolDecimal;
import com.clbs.posval.cobol.PackedField;
import com.clbs.posval.cobol.SignedEditedField;
import com.clbs.posval.service.PositionValuationService;
import com.clbs.posval.service.PositionValuationService.ChangePercent;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/**
 * Asserts the Java money arithmetic against {@code arithmetic-golden.csv}, the stdout of
 * {@code parity/cobol/PARITHM.cbl} and {@code parity/cobol/PDIVZER.cbl}. Those harnesses re-execute
 * the {@code ADD}, {@code SUBTRACT} and {@code COMPUTE} statements of
 * {@code PORTTRAN 2210/2220/2240} and {@code RPTPOS00 2110} using the production {@code PIC}
 * clauses, so each row fixes a result that GnuCOBOL actually produced.
 *
 * <p>Operation codes: {@code AQ} add to units, {@code SQ} subtract from units, {@code AA} add to
 * cost, {@code SA} subtract from cost, {@code PC} percentage change, {@code PZ} percentage change
 * with a zero divisor.
 */
class PackedArithmeticParityTest {

    private static final String GOLDEN = "/parity/arithmetic-golden.csv";

    private final PositionValuationService valuationService = new PositionValuationService();

    @TestFactory
    @DisplayName("packed decimal: every golden vector reproduces the COBOL result")
    Stream<DynamicTest> matchesEveryGoldenVector() {
        List<GoldenVectors.Row> rows = GoldenVectors.load(GOLDEN);
        return rows.stream().map(row -> {
            String op = row.get(0);
            BigDecimal a = new BigDecimal(row.trimmed(1));
            BigDecimal b = new BigDecimal(row.trimmed(2));
            String expected = row.get(4);

            return DynamicTest.dynamicTest("%s %s %s -> %s".formatted(op, a, b, expected.strip()), () -> {
                switch (op) {
                    case "AQ" -> assertThat(CobolDecimal.add(a, b, PackedField.QUANTITY))
                            .isEqualByComparingTo(new BigDecimal(expected.strip()));
                    case "SQ" -> assertThat(CobolDecimal.subtract(a, b, PackedField.QUANTITY))
                            .isEqualByComparingTo(new BigDecimal(expected.strip()));
                    case "AA" -> assertThat(CobolDecimal.add(a, b, PackedField.AMOUNT))
                            .isEqualByComparingTo(new BigDecimal(expected.strip()));
                    case "SA" -> assertThat(CobolDecimal.subtract(a, b, PackedField.AMOUNT))
                            .isEqualByComparingTo(new BigDecimal(expected.strip()));
                    case "PC" -> assertThat(valuationService.changePercent(a, b).edited())
                            .isEqualTo(expected);
                    case "PZ" -> assertThat(valuationService.changePercent(a, b).sizeError()).isTrue();
                    default -> throw new IllegalStateException("unknown golden vector op: " + op);
                }
            });
        });
    }

    @Test
    @DisplayName("R-5.1: stores truncate toward zero, they never round")
    void storesTruncateTowardZero() {
        assertThat(PackedField.AMOUNT.store(new BigDecimal("0.999")))
                .isEqualByComparingTo(new BigDecimal("0.99"));
        assertThat(PackedField.AMOUNT.store(new BigDecimal("-0.999")))
                .isEqualByComparingTo(new BigDecimal("-0.99"));
        assertThat(PackedField.QUANTITY.store(new BigDecimal("1.00009")))
                .isEqualByComparingTo(new BigDecimal("1.0000"));
    }

    @Test
    @DisplayName("R-5.2: with no ON SIZE ERROR, overflow wraps and keeps the sign")
    void overflowWrapsModuloTheFieldWidth() {
        BigDecimal justUnder = new BigDecimal("99999999999.9999");

        assertThat(CobolDecimal.add(justUnder, new BigDecimal("0.0001"), PackedField.QUANTITY))
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(CobolDecimal.add(justUnder, BigDecimal.ONE, PackedField.QUANTITY))
                .isEqualByComparingTo(new BigDecimal("0.9999"));
        assertThat(CobolDecimal.subtract(justUnder.negate(), BigDecimal.ONE, PackedField.QUANTITY))
                .isEqualByComparingTo(new BigDecimal("-0.9999"));
    }

    @Test
    @DisplayName("R-6.2: the percentage change field holds three integer digits and wraps past 999.99")
    void percentageChangeWrapsPastThreeIntegerDigits() {
        ChangePercent result = valuationService.changePercent(
                new BigDecimal("12345.67"), new BigDecimal("100.00"));

        assertThat(result.overflowed()).isTrue();
        assertThat(result.edited()).isEqualTo("+245.67");
    }

    @Test
    @DisplayName("R-6.3: a zero previous value is a size error, not a zero percentage")
    void zeroPreviousValueIsASizeError() {
        ChangePercent result = valuationService.changePercent(
                new BigDecimal("100.00"), BigDecimal.ZERO);

        assertThat(result.sizeError()).isTrue();
        assertThat(result.percentChange()).isNull();
        assertThat(result.edited()).isNull();
    }

    @Test
    @DisplayName("R-6.1: operands are truncated to two decimals before the division")
    void operandsAreTruncatedBeforeTheDivision() {
        assertThat(valuationService.changePercent(new BigDecimal("100.005"), new BigDecimal("100.00")).edited())
                .isEqualTo("+  0.00");
        assertThat(valuationService.changePercent(new BigDecimal("100.01"), new BigDecimal("100.00")).edited())
                .isEqualTo("+  0.01");
    }

    @Test
    @DisplayName("PIC +ZZ9.99: sign is always printed and leading zeros are spaces")
    void editedFieldRendering() {
        assertThat(SignedEditedField.format(new BigDecimal("10.00"))).isEqualTo("+ 10.00");
        assertThat(SignedEditedField.format(new BigDecimal("0.00"))).isEqualTo("+  0.00");
        assertThat(SignedEditedField.format(new BigDecimal("-100.00"))).isEqualTo("-100.00");
        assertThat(SignedEditedField.format(new BigDecimal("-66.666"))).isEqualTo("- 66.66");
    }
}
