package com.cognition.clbs.common.cobol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class BinaryFieldTest {

  @Test
  void byteLengthFollowsIbmConvention() {
    assertThat(BinaryField.byteLength(1)).isEqualTo(2);
    assertThat(BinaryField.byteLength(4)).isEqualTo(2);
    assertThat(BinaryField.byteLength(5)).isEqualTo(4);
    assertThat(BinaryField.byteLength(9)).isEqualTo(4);
    assertThat(BinaryField.byteLength(10)).isEqualTo(8);
    assertThat(BinaryField.byteLength(18)).isEqualTo(8);
    assertThatThrownBy(() -> BinaryField.byteLength(19))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void encodesBigEndianTwosComplement() {
    assertThat(BinaryField.encode(1, 4, true)).containsExactly(0x00, 0x01);
    assertThat(BinaryField.encode(-1, 4, true)).containsExactly(0xFF, 0xFF);
    assertThat(BinaryField.encode(-8, 4, true)).containsExactly(0xFF, 0xF8);
    assertThat(BinaryField.encode(100_000, 9, true)).containsExactly(0x00, 0x01, 0x86, 0xA0);
    assertThat(BinaryField.encode(1L << 40, 18, false))
        .containsExactly(0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00);
  }

  @Test
  void decodesSignedAndUnsigned() {
    byte[] allOnes = {(byte) 0xFF, (byte) 0xFF};
    assertThat(BinaryField.decode(allOnes, true)).isEqualTo(-1);
    assertThat(BinaryField.decode(allOnes, false)).isEqualTo(65535);
    assertThat(BinaryField.decode(new byte[] {0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF}, true))
        .isEqualTo(Integer.MAX_VALUE);
    assertThat(BinaryField.decode(new byte[0], true)).isZero();
  }

  @Test
  void enforcesPictureRange() {
    assertThat(BinaryField.maxMagnitude(4)).isEqualTo(9999);
    assertThatThrownBy(() -> BinaryField.encode(10_000, 4, true))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> BinaryField.encode(-1, 4, false))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> BinaryField.encode(-10_000, 4, true))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void roundTripsExtremes() {
    for (int digits : new int[] {2, 4, 5, 9, 10, 18}) {
      long max = BinaryField.maxMagnitude(digits);
      assertThat(BinaryField.decode(BinaryField.encode(max, digits, true), true)).isEqualTo(max);
      assertThat(BinaryField.decode(BinaryField.encode(-max, digits, true), true)).isEqualTo(-max);
      assertThat(BinaryField.decode(BinaryField.encode(max, digits, false), false)).isEqualTo(max);
    }
  }
}
