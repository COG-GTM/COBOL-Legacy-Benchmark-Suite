package com.cognition.portfolio.transaction.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Asserts the layout and ordering of {@code TRN-KEY} (BR-21). */
class TransactionKeyTest {

  @Test
  @DisplayName("TRN-KEY is 28 characters: 8 + 6 + 8 + 6")
  void keyLayout() {
    TransactionKey key = new TransactionKey("20240320", "093015", "PORT0001", "000001");

    assertThat(key.toKeyString()).isEqualTo("20240320093015PORT0001000001");
    assertThat(key.toKeyString()).hasSize(TransactionKey.KEY_LENGTH).hasSize(28);
    assertThat(key.getTransactionDate()).contains(LocalDate.of(2024, 3, 20));
    assertThat(key.getTransactionTime()).contains(LocalTime.of(9, 30, 15));
  }

  @Test
  @DisplayName("OQ-11: TRN-DATE/TRN-TIME are PIC X, so the typed views are empty, never throwing")
  void typedViewsToleratePicXValues() {
    TransactionKey key = new TransactionKey("20240230", "999999", "PORT0001", "000001");

    assertThat(key.getTransactionDate()).isEmpty();
    assertThat(key.getTransactionTime()).isEmpty();
    assertThat(key.toKeyString()).isEqualTo("20240230999999PORT0001000001");
  }

  @Test
  @DisplayName("A 28 character key round trips through parse()")
  void parseRoundTrip() {
    TransactionKey key = TransactionKey.parse("20240320093015PORT0001000001");

    assertThat(key.getTrnDate()).isEqualTo("20240320");
    assertThat(key.getTrnTime()).isEqualTo("093015");
    assertThat(key.getTrnPortfolioId()).isEqualTo("PORT0001");
    assertThat(key.getTrnSequenceNo()).isEqualTo("000001");
  }

  @Test
  @DisplayName("A key of the wrong length is rejected")
  void parseRejectsWrongLength() {
    assertThatThrownBy(() -> TransactionKey.parse("20240320"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("BR-21: keys sort by date, then time, then portfolio id, then sequence number")
  void vsamKeyOrdering() {
    TransactionKey a = new TransactionKey("20240320", "093015", "PORT0001", "000001");
    TransactionKey b = new TransactionKey("20240320", "093015", "PORT0001", "000002");
    TransactionKey c = new TransactionKey("20240320", "093015", "PORT0002", "000001");
    TransactionKey d = new TransactionKey("20240320", "101122", "PORT0001", "000001");
    TransactionKey e = new TransactionKey("20240321", "080000", "PORT0001", "000001");

    assertThat(List.of(e, c, d, b, a).stream().sorted().toList()).containsExactly(a, b, c, d, e);
  }
}
