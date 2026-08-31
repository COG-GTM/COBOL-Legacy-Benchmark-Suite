package com.clbs.posval.parity;

import static org.assertj.core.api.Assertions.assertThat;

import com.clbs.posval.validation.PortfolioValidator;
import com.clbs.posval.validation.ValidationResult;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.DynamicTest;
import java.util.stream.Stream;

/**
 * Asserts the Java validator against {@code portvald-golden.csv}, which is the stdout of
 * {@code parity/cobol/PVDRIVE.cbl} calling the unmodified {@code src/programs/portfolio/PORTVALD.cbl}
 * compiled by GnuCOBOL. Every row is a return code and message actually produced by the COBOL.
 */
class PortfolioValidatorParityTest {

    private static final String GOLDEN = "/parity/portvald-golden.csv";

    private final PortfolioValidator validator = new PortfolioValidator();

    @TestFactory
    @DisplayName("PORTVALD: every golden vector reproduces the COBOL return code and message")
    Stream<DynamicTest> matchesEveryGoldenVector() {
        List<GoldenVectors.Row> rows = GoldenVectors.load(GOLDEN);
        return rows.stream().map(row -> {
            char type = row.get(0).charAt(0);
            String input = row.get(1);
            int expectedCode = Integer.parseInt(row.get(2));
            String expectedMessage = row.trimmed(3);

            return DynamicTest.dynamicTest(
                    "type=%c input='%s' -> %04d".formatted(type, input, expectedCode),
                    () -> {
                        ValidationResult result = validator.validate(type, input);
                        assertThat(result.returnCode()).isEqualTo(expectedCode);
                        assertThat(result.trimmedMessage()).isEqualTo(expectedMessage);
                    });
        });
    }

    @Test
    @DisplayName("R-1.3: 1000-VALIDATE-ID rejects even a well formed portfolio ID")
    void wellFormedPortfolioIdIsStillRejected() {
        ValidationResult result = validator.validatePortfolioId("PORT0001");

        assertThat(result.returnCode()).isEqualTo(ValidationResult.VAL_INVALID_ID);
        assertThat(result.trimmedMessage()).isEqualTo(ValidationResult.ERR_ID);
    }

    @Test
    @DisplayName("R-2.2: 2000-VALIDATE-ACCOUNT rejects a ten digit account number")
    void tenDigitAccountNumberIsRejected() {
        assertThat(validator.validateAccountNumber("1234567890").returnCode())
                .isEqualTo(ValidationResult.VAL_INVALID_ACCT);
    }

    @Test
    @DisplayName("R-2.2: only a fifty digit non-zero account number passes")
    void fiftyDigitAccountNumberPasses() {
        assertThat(validator.validateAccountNumber("1".repeat(50)).isSuccess()).isTrue();
        assertThat(validator.validateAccountNumber("0".repeat(50)).returnCode())
                .isEqualTo(ValidationResult.VAL_INVALID_ACCT);
    }

    @Test
    @DisplayName("R-3.1: investment type matching is case sensitive and space padded")
    void investmentTypeMatching() {
        assertThat(validator.validateInvestmentType("STK").isSuccess()).isTrue();
        assertThat(validator.validateInvestmentType("STK   ").isSuccess()).isTrue();
        assertThat(validator.validateInvestmentType("stk").returnCode())
                .isEqualTo(ValidationResult.VAL_INVALID_TYPE);
        assertThat(validator.validateInvestmentType("STKX").returnCode())
                .isEqualTo(ValidationResult.VAL_INVALID_TYPE);
    }

    @Test
    @DisplayName("R-4.1: 4000-VALIDATE-AMOUNT accepts everything, VAL-INVALID-AMT is unreachable")
    void amountValidationIsVacuous() {
        assertThat(validator.validateAmount("0").isSuccess()).isTrue();
        assertThat(validator.validateAmount("-9999999999999.99").isSuccess()).isTrue();
        assertThat(validator.validateAmount("99999999999999999999").isSuccess()).isTrue();
        assertThat(validator.validateAmount("ABC").isSuccess()).isTrue();
        assertThat(validator.validateAmount("").isSuccess()).isTrue();
    }

    @Test
    @DisplayName("R-0.2: an unknown request type returns VAL-INVALID-ID, not a distinct code")
    void unknownRequestTypeReusesTheIdReturnCode() {
        ValidationResult result = validator.validate('X', "0001");

        assertThat(result.returnCode()).isEqualTo(ValidationResult.VAL_INVALID_ID);
        assertThat(result.trimmedMessage()).isEqualTo(ValidationResult.ERR_VALIDATE_TYPE);
    }
}
