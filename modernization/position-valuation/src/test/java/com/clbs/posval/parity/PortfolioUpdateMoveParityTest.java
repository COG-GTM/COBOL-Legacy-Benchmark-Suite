package com.clbs.posval.parity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.clbs.posval.service.PortfolioUpdateService;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/**
 * Asserts the {@code 'V'} branch of {@code PORTUPDT 2200-APPLY-UPDATE} against
 * {@code portupdt-move-golden.csv}, the stdout of {@code parity/cobol/PUPDMOV.cbl}, which performs
 * the same two {@code MOVE} statements on the same {@code PIC} clauses.
 */
class PortfolioUpdateMoveParityTest {

    private static final String GOLDEN = "/parity/portupdt-move-golden.csv";

    private final PortfolioUpdateService service = new PortfolioUpdateService();

    @TestFactory
    @DisplayName("PORTUPDT 'V': every golden vector reproduces the COBOL amount")
    Stream<DynamicTest> matchesEveryGoldenVector() {
        List<GoldenVectors.Row> rows = GoldenVectors.load(GOLDEN);
        return rows.stream().map(row -> {
            String input = row.get(1);
            BigDecimal expected = new BigDecimal(row.trimmed(2));

            return DynamicTest.dynamicTest("'%s' -> %s".formatted(input, expected), () ->
                    assertThat(service.convertValue(input)).isEqualByComparingTo(expected));
        });
    }

    @Test
    @DisplayName("R-11.3: the amendment value is truncated to S9(13)V99, high order digits are lost")
    void amendmentValueIsTruncatedToTheFieldWidth() {
        assertThat(service.convertValue("999999999999999"))
                .isEqualByComparingTo(new BigDecimal("9999999999999.00"));
        assertThat(service.convertValue("12500.005"))
                .isEqualByComparingTo(new BigDecimal("12500.00"));
    }

    @Test
    @DisplayName("R-11.4: a non-numeric amendment value has no defined result and is refused")
    void nonNumericAmendmentValueIsRefused() {
        assertThatThrownBy(() -> service.convertValue("TWELVE THOUSAND"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("R-11.2: a 'S' amendment keeps only the first character of a PIC X(50) field")
    void statusAmendmentKeepsOnlyTheFirstCharacter() {
        assertThat(service.applyUpdate(true, PortfolioUpdateService.ACTION_STATUS, "CLOSED").status())
                .isEqualTo("C");
    }

    @Test
    @DisplayName("R-11.5: an unknown action rewrites the record unchanged and is counted as applied")
    void unknownActionIsSilentlyAccepted() {
        PortfolioUpdateService.UpdateOutcome outcome = service.applyUpdate(true, 'Z', "anything");

        assertThat(outcome.applied()).isTrue();
        assertThat(outcome.status()).isNull();
        assertThat(outcome.clientName()).isNull();
        assertThat(outcome.totalValue()).isNull();
    }

    @Test
    @DisplayName("R-11.6: a missing portfolio record is counted as an error, not applied")
    void missingRecordIsNotApplied() {
        assertThat(service.applyUpdate(false, PortfolioUpdateService.ACTION_VALUE, "1250000").applied())
                .isFalse();
    }
}
