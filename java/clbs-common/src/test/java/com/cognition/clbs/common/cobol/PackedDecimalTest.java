package com.cognition.clbs.common.cobol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PackedDecimalTest {

  @Test
  void byteLengthFollowsCobolRule() {
    assertThat(PackedDecimal.byteLength(1)).isEqualTo(1);
    assertThat(PackedDecimal.byteLength(2)).isEqualTo(2);
    assertThat(PackedDecimal.byteLength(3)).isEqualTo(2);
    assertThat(PackedDecimal.byteLength(15)).isEqualTo(8);
    assertThat(PackedDecimal.byteLength(18)).isEqualTo(10);
    assertThatThrownBy(() -> PackedDecimal.byteLength(0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> PackedDecimal.byteLength(32))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void encodesKnownBitPatterns() {
    // PIC S9(3) COMP-3 value 123 -> x'123C'
    assertThat(PackedDecimal.encode(new BigDecimal("123"), 3, 0)).containsExactly(0x12, 0x3C);
    // PIC S9(3)V99 value -12.34 -> x'01234D' (leading zero fills the spare nibble)
    assertThat(PackedDecimal.encode(new BigDecimal("-12.34"), 5, 2))
        .containsExactly(0x01, 0x23, 0x4D);
    // PIC S9(4) COMP-3 value 0 -> x'00000C'
    assertThat(PackedDecimal.encode(BigDecimal.ZERO, 4, 0)).containsExactly(0x00, 0x00, 0x0C);
  }

  @Test
  void decodesUnsignedSignNibbleAsPositive() {
    assertThat(PackedDecimal.decode(new byte[] {0x12, 0x3F}, 0)).isEqualByComparingTo("123");
    assertThat(PackedDecimal.decode(new byte[] {0x00, 0x00, 0x0C}, 2))
        .isEqualTo(new BigDecimal("0.00"));
  }

  @ParameterizedTest
  @CsvSource({
    "0.00, 15, 2",
    "9999999999999.99, 15, 2",
    "-9999999999999.99, 15, 2",
    "12345678901.2345, 15, 4",
    "-0.0001, 15, 4",
    "1, 1, 0",
    "-9, 1, 0"
  })
  void roundTripsAcrossPictures(String text, int digits, int scale) {
    BigDecimal value = new BigDecimal(text);
    byte[] packed = PackedDecimal.encode(value, digits, scale);

    assertThat(packed).hasSize(PackedDecimal.byteLength(digits));
    assertThat(PackedDecimal.decode(packed, scale)).isEqualTo(value.setScale(scale));
  }

  @Test
  void rejectsValuesThatDoNotFitThePicture() {
    assertThatThrownBy(() -> PackedDecimal.encode(new BigDecimal("1000"), 3, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("picture allows 3");
    assertThatThrownBy(() -> PackedDecimal.encode(new BigDecimal("1.234"), 5, 2))
        .isInstanceOf(ArithmeticException.class);
  }

  @Test
  void rejectsInvalidInput() {
    assertThatThrownBy(() -> PackedDecimal.decode(new byte[0], 0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> PackedDecimal.decode(new byte[] {0x12, 0x30}, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("sign nibble");
    assertThatThrownBy(() -> PackedDecimal.decode(new byte[] {(byte) 0xAB, 0x3C}, 0))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
