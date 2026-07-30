package com.cognition.portfolio.transaction.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Asserts BR-22: {@code TRN-AMOUNT = TRN-QUANTITY * TRN-PRICE} with COBOL truncation semantics. */
class TransactionAmountCalculatorTest {

  private final TransactionAmountCalculator calculator = new TransactionAmountCalculator();

  @Test
  @DisplayName("BR-22: exact products keep their value and scale of 2")
  void exactProduct() {
    BigDecimal amount = calculator.computeAmount(new BigDecimal("150.0000"), new BigDecimal("187.4500"));

    assertThat(amount).isEqualByComparingTo("28117.50");
    assertThat(amount.scale()).isEqualTo(2);
  }

  @Test
  @DisplayName("BR-22: the third decimal is truncated, not rounded up (no ROUNDED phrase in COBOL)")
  void truncatesRatherThanRounds() {
    // 3.3333 x 3.3333 = 11.11088889 -> S9(13)V9(2) keeps 11.11
    assertThat(calculator.computeAmount(new BigDecimal("3.3333"), new BigDecimal("3.3333")))
        .isEqualByComparingTo("11.11");

    // 1.0000 x 0.9999 = 0.9999 -> 0.99, whereas HALF_UP would give 1.00
    assertThat(calculator.computeAmount(new BigDecimal("1.0000"), new BigDecimal("0.9999")))
        .isEqualByComparingTo("0.99");
  }

  @Test
  @DisplayName("BR-22: negative products truncate toward zero, as COBOL does")
  void truncatesTowardZeroForNegatives() {
    assertThat(calculator.computeAmount(new BigDecimal("-1.0000"), new BigDecimal("0.9999")))
        .isEqualByComparingTo("-0.99");
  }

  @Test
  @DisplayName("BR-22: a supplied TRN-AMOUNT can be reconciled against quantity x price")
  void consistencyCheck() {
    assertThat(
            calculator.isConsistent(
                new BigDecimal("150.0000"), new BigDecimal("187.4500"), new BigDecimal("28117.50")))
        .isTrue();
    assertThat(
            calculator.isConsistent(
                new BigDecimal("150.0000"), new BigDecimal("187.4500"), new BigDecimal("28117.51")))
        .isFalse();
  }

  @Test
  @DisplayName("Quantity and price keep the four decimal places of PIC S9(11)V9(4)")
  void scales() {
    assertThat(TransactionAmountCalculator.QUANTITY_SCALE).isEqualTo(4);
    assertThat(TransactionAmountCalculator.AMOUNT_SCALE).isEqualTo(2);
  }
}
