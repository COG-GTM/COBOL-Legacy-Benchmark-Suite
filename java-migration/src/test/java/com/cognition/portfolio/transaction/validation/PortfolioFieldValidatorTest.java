package com.cognition.portfolio.transaction.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Asserts the field validation rules of {@code PORTVALD.cbl} and the constants of {@code PORTVAL.cpy}. */
class PortfolioFieldValidatorTest {

  private final PortfolioFieldValidator validator = new PortfolioFieldValidator();

  @ParameterizedTest
  @CsvSource({
    "PORT0001,true",
    "PORT9999,true",
    "PORTABCD,false",
    "XXXX0001,false",
    "PORT001,false"
  })
  @DisplayName("BR-15 PORTVALD 1000-VALIDATE-ID: 'PORT' prefix plus four numeric digits")
  void validatePortfolioId(String value, boolean valid) {
    ValidationOutcome outcome = validator.validatePortfolioId(value);

    assertThat(outcome.isValid()).isEqualTo(valid);
    if (!valid) {
      assertThat(outcome.returnCode()).isEqualTo(ValidationReturnCode.INVALID_ID);
      assertThat(outcome.message()).isEqualTo(PortfolioFieldValidator.ERR_ID);
    }
  }

  @ParameterizedTest
  @CsvSource({"1000000001,true", "0000000000,false", "12345ABCDE,false", "'',false"})
  @DisplayName("BR-16 PORTVALD 2000-VALIDATE-ACCOUNT: numeric and non-zero")
  void validateAccountNumber(String value, boolean valid) {
    ValidationOutcome outcome = validator.validateAccountNumber(value);

    assertThat(outcome.isValid()).isEqualTo(valid);
    if (!valid) {
      assertThat(outcome.returnCode()).isEqualTo(ValidationReturnCode.INVALID_ACCT);
      assertThat(outcome.message()).isEqualTo(PortfolioFieldValidator.ERR_ACCT);
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"STK", "BND", "MMF", "ETF"})
  @DisplayName("BR-17 PORTVALD 3000-VALIDATE-TYPE: accepted investment types")
  void validInvestmentTypes(String value) {
    assertThat(validator.validateInvestmentType(value).isValid()).isTrue();
  }

  @Test
  @DisplayName("BR-17 PORTVALD 3000-VALIDATE-TYPE: anything else returns VAL-INVALID-TYPE")
  void invalidInvestmentType() {
    ValidationOutcome outcome = validator.validateInvestmentType("CRY");

    assertThat(outcome.returnCode()).isEqualTo(ValidationReturnCode.INVALID_TYPE);
    assertThat(outcome.message()).isEqualTo(PortfolioFieldValidator.ERR_TYPE);
  }

  @Test
  @DisplayName("BR-18 PORTVALD 4000-VALIDATE-AMOUNT: bounds are VAL-MIN-AMOUNT and VAL-MAX-AMOUNT inclusive")
  void validateAmountRange() {
    assertThat(validator.validateAmount(PortfolioFieldValidator.MIN_AMOUNT).isValid()).isTrue();
    assertThat(validator.validateAmount(PortfolioFieldValidator.MAX_AMOUNT).isValid()).isTrue();
    assertThat(validator.validateAmount(BigDecimal.ZERO).isValid()).isTrue();

    ValidationOutcome tooLarge =
        validator.validateAmount(PortfolioFieldValidator.MAX_AMOUNT.add(new BigDecimal("0.01")));

    assertThat(tooLarge.returnCode()).isEqualTo(ValidationReturnCode.INVALID_AMT);
    assertThat(tooLarge.message()).isEqualTo(PortfolioFieldValidator.ERR_AMT);
  }

  @Test
  @DisplayName("BR-19 PORTVALD 0000-MAIN: dispatch on LS-VALIDATE-TYPE, WHEN OTHER is rejected")
  void dispatchOnValidateType() {
    assertThat(validator.validate('I', "PORT0001").isValid()).isTrue();
    assertThat(validator.validate('A', "1000000001").isValid()).isTrue();
    assertThat(validator.validate('T', "STK").isValid()).isTrue();
    assertThat(validator.validate('M', "1000.00").isValid()).isTrue();

    ValidationOutcome unknown = validator.validate('Z', "anything");

    assertThat(unknown.returnCode()).isEqualTo(ValidationReturnCode.INVALID_ID);
    assertThat(unknown.message()).isEqualTo("Invalid validation type");
  }

  @Test
  @DisplayName("PORTVAL.cpy: return codes keep their COBOL numeric values")
  void returnCodeValues() {
    assertThat(ValidationReturnCode.SUCCESS.getCode()).isZero();
    assertThat(ValidationReturnCode.INVALID_ID.getCode()).isEqualTo(1);
    assertThat(ValidationReturnCode.INVALID_ACCT.getCode()).isEqualTo(2);
    assertThat(ValidationReturnCode.INVALID_TYPE.getCode()).isEqualTo(3);
    assertThat(ValidationReturnCode.INVALID_AMT.getCode()).isEqualTo(4);
  }
}
