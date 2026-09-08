package com.clbs.portfolio;

import static org.assertj.core.api.Assertions.assertThat;

import com.clbs.portfolio.PortfolioValidator.Result;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PortfolioValidatorTest {

    private final PortfolioValidator validator = new PortfolioValidator();

    @Test
    void acceptsPortPrefixWithNumericSuffix() {
        assertThat(validator.validateId("PORT0001").valid()).isTrue();
    }

    @Test
    void rejectsIdWithoutPortPrefix() {
        Result result = validator.validateId("XXXX0001");
        assertThat(result.returnCode()).isEqualTo(PortfolioValidator.VAL_INVALID_ID);
        assertThat(result.errorMessage()).isEqualTo(PortfolioValidator.ERR_ID);
    }

    @Test
    void rejectsIdWithNonNumericSuffix() {
        assertThat(validator.validateId("PORTABCD").valid()).isFalse();
    }

    @Test
    void rejectsShortId() {
        assertThat(validator.validateId("PORT1").valid()).isFalse();
    }

    @Test
    void rejectsNonNumericOrZeroAccount() {
        assertThat(validator.validateAccount("12345678").valid()).isTrue();
        assertThat(validator.validateAccount("00000000").returnCode())
                .isEqualTo(PortfolioValidator.VAL_INVALID_ACCT);
        assertThat(validator.validateAccount("ABC").returnCode())
                .isEqualTo(PortfolioValidator.VAL_INVALID_ACCT);
        assertThat(validator.validateAccount("").returnCode())
                .isEqualTo(PortfolioValidator.VAL_INVALID_ACCT);
    }

    @Test
    void acceptsOnlyTheFourInvestmentTypes() {
        assertThat(validator.validateType("STK").valid()).isTrue();
        assertThat(validator.validateType("BND").valid()).isTrue();
        assertThat(validator.validateType("MMF").valid()).isTrue();
        assertThat(validator.validateType("ETF").valid()).isTrue();
        assertThat(validator.validateType("XXX").errorMessage())
                .isEqualTo(PortfolioValidator.ERR_TYPE);
    }

    @Test
    void enforcesSignedThirteenDigitAmountRange() {
        assertThat(validator.validateAmount(new BigDecimal("9999999999999.99")).valid()).isTrue();
        assertThat(validator.validateAmount(new BigDecimal("-9999999999999.99")).valid()).isTrue();
        assertThat(validator.validateAmount(new BigDecimal("10000000000000.00")).returnCode())
                .isEqualTo(PortfolioValidator.VAL_INVALID_AMT);
        assertThat(validator.validateAmount("not-a-number").returnCode())
                .isEqualTo(PortfolioValidator.VAL_INVALID_AMT);
    }

    @Test
    void dispatchesOnValidationType() {
        assertThat(validator.validate('I', "PORT0001").valid()).isTrue();
        assertThat(validator.validate('A', "12345678").valid()).isTrue();
        assertThat(validator.validate('T', "STK").valid()).isTrue();
        assertThat(validator.validate('M', "100.00").valid()).isTrue();
        assertThat(validator.validate('Z', "anything").errorMessage())
                .isEqualTo(PortfolioValidator.ERR_REQUEST);
    }
}
