package com.cognition.portfolio.transaction.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.cognition.portfolio.transaction.TestTransactions;
import com.cognition.portfolio.transaction.domain.PortfolioTransaction;
import com.cognition.portfolio.transaction.domain.TransactionType;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Asserts the validation rules extracted from {@code PORTTRAN 2100-VALIDATE-TRANSACTION}. */
class TransactionValidatorTest {

  private final PortfolioFieldValidator fieldValidator = new PortfolioFieldValidator();
  private final TransactionValidator validator =
      new TransactionValidator(new FormatOnlyPortfolioReferenceValidator(fieldValidator));

  @Test
  @DisplayName("A well formed BU transaction passes every check (PORTTRAN 2100)")
  void validTransactionPasses() {
    assertThat(validator.validate(TestTransactions.buy()).isValid()).isTrue();
  }

  @Nested
  @DisplayName("BR-01/BR-02 PORTTRAN 2110-CHECK-PORTFOLIO")
  class CheckPortfolio {

    @Test
    @DisplayName("BR-01: blank TRN-PORTFOLIO-ID is rejected with 'Portfolio ID is required'")
    void blankPortfolioIdRejected() {
      PortfolioTransaction transaction =
          TestTransactions.builder("20240320", "093015", "        ", "000001").build();

      ValidationOutcome outcome = validator.validate(transaction);

      assertThat(outcome.isValid()).isFalse();
      assertThat(outcome.message()).isEqualTo("Portfolio ID is required");
      assertThat(outcome.ruleId()).isEqualTo("BR-01");
    }

    @Test
    @DisplayName("BR-02: unknown portfolio is rejected with 'Invalid Portfolio ID: <id>'")
    void unknownPortfolioRejected() {
      PortfolioTransaction transaction =
          TestTransactions.builder("20240320", "093015", "XXXX0001", "000001").build();

      ValidationOutcome outcome = validator.validate(transaction);

      assertThat(outcome.isValid()).isFalse();
      assertThat(outcome.message()).isEqualTo("Invalid Portfolio ID: XXXX0001");
      assertThat(outcome.ruleId()).isEqualTo("BR-02");
    }
  }

  @Nested
  @DisplayName("BR-04/BR-05/BR-06 PORTTRAN 2130-CHECK-AMOUNTS")
  class CheckAmounts {

    @Test
    @DisplayName("BR-04: TRN-QUANTITY must be greater than zero")
    void zeroQuantityRejected() {
      PortfolioTransaction transaction =
          TestTransactions.builder("20240320", "093015", "PORT0001", "000001")
              .trnQuantity(BigDecimal.ZERO)
              .build();

      ValidationOutcome outcome = validator.validate(transaction);

      assertThat(outcome.message()).isEqualTo("Quantity must be greater than zero");
      assertThat(outcome.ruleId()).isEqualTo("BR-04");
    }

    @Test
    @DisplayName("BR-04: the quantity check has no TR exemption, unlike price and amount")
    void transferWithZeroQuantityStillRejected() {
      PortfolioTransaction transfer =
          TestTransactions.builder("20240320", "113000", "PORT0002", "000001")
              .trnType(TransactionType.TRANSFER)
              .trnQuantity(BigDecimal.ZERO)
              .trnPrice(BigDecimal.ZERO)
              .trnAmount(BigDecimal.ZERO)
              .build();

      assertThat(validator.validate(transfer).message()).isEqualTo("Quantity must be greater than zero");
    }

    @Test
    @DisplayName("BR-05: TRN-PRICE must be greater than zero unless TRN-TYPE = 'TR'")
    void zeroPriceRejectedExceptForTransfer() {
      PortfolioTransaction buy =
          TestTransactions.builder("20240320", "093015", "PORT0001", "000001")
              .trnPrice(BigDecimal.ZERO)
              .build();

      ValidationOutcome outcome = validator.validate(buy);

      assertThat(outcome.message()).isEqualTo("Price must be greater than zero");
      assertThat(outcome.ruleId()).isEqualTo("BR-05");
      assertThat(validator.validate(TestTransactions.transfer()).isValid()).isTrue();
    }

    @Test
    @DisplayName("BR-06: TRN-AMOUNT must be greater than zero unless TRN-TYPE = 'TR'")
    void zeroAmountRejectedExceptForTransfer() {
      PortfolioTransaction fee =
          TestTransactions.builder("20240320", "104500", "PORT0001", "000003")
              .trnType(TransactionType.FEE)
              .trnAmount(BigDecimal.ZERO)
              .build();

      ValidationOutcome outcome = validator.validate(fee);

      assertThat(outcome.message()).isEqualTo("Amount must be greater than zero");
      assertThat(outcome.ruleId()).isEqualTo("BR-06");
    }
  }

  @Test
  @DisplayName("BR-07: checks short-circuit in COBOL order 2110 -> 2120 -> 2130")
  void firstFailureWins() {
    PortfolioTransaction transaction =
        TestTransactions.builder("20240320", "093015", "XXXX0001", "000001")
            .trnQuantity(BigDecimal.ZERO)
            .build();

    ValidationOutcome outcome = validator.validate(transaction);

    assertThat(outcome.ruleId()).isEqualTo("BR-02");
    assertThat(outcome.cobolParagraph()).isEqualTo("PORTTRAN 2110-CHECK-PORTFOLIO");
  }

  @Test
  @DisplayName("BR-03: a TRN-TYPE outside the 88-levels hits the WHEN OTHER branch of 2120")
  void unknownTypeRejected() {
    assertThat(TransactionType.fromCode("XX")).isEmpty();
    assertThat(validator.checkTransactionType(null).message()).startsWith("Invalid Transaction Type");
    assertThat(validator.checkTransactionType(null).ruleId()).isEqualTo("BR-03");
  }

  @Test
  @DisplayName("BR-03: BU, SL, TR and FE are the accepted TRN-TYPE values")
  void acceptedTypes() {
    assertThat(TransactionType.fromCode("BU")).contains(TransactionType.BUY);
    assertThat(TransactionType.fromCode("SL")).contains(TransactionType.SELL);
    assertThat(TransactionType.fromCode("TR")).contains(TransactionType.TRANSFER);
    assertThat(TransactionType.fromCode("FE")).contains(TransactionType.FEE);
  }
}
