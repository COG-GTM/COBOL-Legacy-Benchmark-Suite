package com.cognition.portfolio.transaction.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Asserts the 88-level values of {@code TRN-STATUS} and the derived transition rule BR-23. */
class TransactionStatusTest {

  @Test
  @DisplayName("TRNREC 88-levels: P, D, F, R map to the four statuses")
  void codesMatchCopybook() {
    assertThat(TransactionStatus.PENDING.getCode()).isEqualTo("P");
    assertThat(TransactionStatus.DONE.getCode()).isEqualTo("D");
    assertThat(TransactionStatus.FAILED.getCode()).isEqualTo("F");
    assertThat(TransactionStatus.REVERSED.getCode()).isEqualTo("R");
    assertThat(TransactionStatus.fromCode("D")).contains(TransactionStatus.DONE);
    assertThat(TransactionStatus.fromCode("X")).isEmpty();
  }

  @Test
  @DisplayName("BR-23: a pending transaction can complete or fail")
  void pendingTransitions() {
    assertThat(TransactionStatus.PENDING.canTransitionTo(TransactionStatus.DONE)).isTrue();
    assertThat(TransactionStatus.PENDING.canTransitionTo(TransactionStatus.FAILED)).isTrue();
    assertThat(TransactionStatus.PENDING.canTransitionTo(TransactionStatus.REVERSED)).isFalse();
  }

  @Test
  @DisplayName("BR-23: only a completed transaction can be reversed")
  void doneTransitions() {
    assertThat(TransactionStatus.DONE.canTransitionTo(TransactionStatus.REVERSED)).isTrue();
    assertThat(TransactionStatus.DONE.canTransitionTo(TransactionStatus.PENDING)).isFalse();
    assertThat(TransactionStatus.DONE.canTransitionTo(TransactionStatus.FAILED)).isFalse();
  }

  @Test
  @DisplayName("BR-23: F and R are terminal")
  void terminalStatuses() {
    assertThat(TransactionStatus.FAILED.allowedTransitions()).isEmpty();
    assertThat(TransactionStatus.REVERSED.allowedTransitions()).isEmpty();
  }
}
