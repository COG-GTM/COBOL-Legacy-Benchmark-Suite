package com.clbs.portfolio.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PortfolioValidatorTest {

    private final PortfolioValidator validator = new PortfolioValidator();

    /** Replays every PORTVALD request from parity/fixtures and compares with the GnuCOBOL output. */
    @Test
    void matchesCobolBaseline() throws IOException {
        List<String> requests = Files.readAllLines(Path.of("parity/fixtures/portvald.txt"));
        List<String> expected = Files.readAllLines(Path.of("parity/baseline/08-portvald.txt"));
        assertThat(requests).hasSameSizeAs(expected);
        for (int i = 0; i < requests.size(); i++) {
            String req = requests.get(i);
            char type = req.charAt(0);
            String value = req.substring(1);
            ValidationResult r = validator.validate(type, value);
            String actual = type + "|" + value.strip() + "|" + String.format("%04d", r.returnCode()) + "|" + r.message();
            assertThat(actual).as("request %d", i + 1).isEqualTo(expected.get(i));
        }
    }

    @ParameterizedTest
    @CsvSource({"STK,0", "BND,0", "MMF,0", "ETF,0", "XYZ,3", "' STK',3", "stk,3"})
    void investmentType(String value, int expectedCode) {
        assertThat(validator.validate('T', value).returnCode()).isEqualTo(expectedCode);
    }

    @Test
    void portfolioIdNeverPassesBecauseNumericCheckFieldIsPadded() {
        // VAL-NUMERIC-CHECK is X(10) but receives only 4 digits; the trailing spaces fail IS NUMERIC.
        assertThat(validator.validate('I', "PORT1234").returnCode()).isEqualTo(ValidationResult.VAL_INVALID_ID);
        assertThat(validator.validate('I', "PORT1234").message()).isEqualTo("Invalid Portfolio ID format");
    }

    @Test
    void accountNumberMustFillTheWholeInputField() {
        assertThat(validator.validate('A', "1234567890").valid()).isFalse();
        assertThat(validator.validate('A', "9".repeat(50)).valid()).isTrue();
        assertThat(validator.validate('A', "0".repeat(50)).returnCode()).isEqualTo(ValidationResult.VAL_INVALID_ACCT);
    }

    @Test
    void amountWithinRange() {
        assertThat(validator.validate('M', "9999999999999.99").valid()).isTrue();
        assertThat(validator.validate('M', "-42.10").valid()).isTrue();
        assertThat(validator.validate('M', "not a number").valid()).isTrue();
    }

    @Test
    void unknownValidationType() {
        ValidationResult r = validator.validate('Z', "x");
        assertThat(r.returnCode()).isEqualTo(ValidationResult.VAL_INVALID_ID);
        assertThat(r.message()).isEqualTo("Invalid validation type");
    }
}
