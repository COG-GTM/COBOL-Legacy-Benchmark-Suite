package com.cog.portfolio.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** PORTVALD.cbl / TSTVAL00.cbl rules. */
class PortfolioValidatorTest {

    private final PortfolioValidator validator = new PortfolioValidator();

    @ParameterizedTest
    @ValueSource(strings = {"PORT0001", "PORT9999", "PORT0000"})
    void validPortfolioId(String id) {
        assertThat(validator.validatePortfolioId(id).isValid()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"PORTABCD", "port0001", "XXXX0001", "PORT001", "PORT00011", "", "PORT 001"})
    void invalidPortfolioId(String id) {
        ValidationResult r = validator.validatePortfolioId(id);
        assertThat(r.code()).isEqualTo(ValidationResult.Code.INVALID_ID);
        assertThat(r.message()).isEqualTo(ValidationResult.ERR_ID);
    }

    @Test
    void nullPortfolioIdIsInvalid() {
        assertThat(validator.validatePortfolioId(null).code()).isEqualTo(ValidationResult.Code.INVALID_ID);
    }

    @Test
    void validAccount() {
        assertThat(validator.validateAccountNo("1234567890").isValid()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0000000000", "12345ABCDE", "", "12345 7890"})
    void invalidAccount(String account) {
        ValidationResult r = validator.validateAccountNo(account);
        assertThat(r.code()).isEqualTo(ValidationResult.Code.INVALID_ACCOUNT);
        assertThat(r.message()).isEqualTo(ValidationResult.ERR_ACCOUNT);
    }

    @ParameterizedTest
    @ValueSource(strings = {"STK", "BND", "MMF", "ETF"})
    void validInvestmentType(String type) {
        assertThat(validator.validateInvestmentType(type).isValid()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"XYZ", "stk", "", "OPT"})
    void invalidInvestmentType(String type) {
        assertThat(validator.validateInvestmentType(type).code()).isEqualTo(ValidationResult.Code.INVALID_TYPE);
    }

    @Test
    void amountWithinRange() {
        assertThat(validator.validateAmount(new BigDecimal("0.00")).isValid()).isTrue();
        assertThat(validator.validateAmount(new BigDecimal("9999999999999.99")).isValid()).isTrue();
        assertThat(validator.validateAmount(new BigDecimal("-9999999999999.99")).isValid()).isTrue();
    }

    @Test
    void amountOutsideRange() {
        assertThat(validator.validateAmount(new BigDecimal("10000000000000.00")).code())
                .isEqualTo(ValidationResult.Code.INVALID_AMOUNT);
        assertThat(validator.validateAmount(new BigDecimal("-10000000000000.00")).code())
                .isEqualTo(ValidationResult.Code.INVALID_AMOUNT);
        assertThat(validator.validateAmount("abc").message()).isEqualTo(ValidationResult.ERR_AMOUNT);
    }
}
