package com.cognition.clbs.common.cobol;

/**
 * COMP / BINARY field sizing. Follows the IBM Enterprise COBOL convention: 1–4 digits occupy a
 * halfword (2 bytes), 5–9 digits a fullword (4 bytes), 10–18 digits a doubleword (8 bytes). Values
 * are big-endian two's complement.
 */
public final class BinaryField {

  private BinaryField() {}

  /** Storage size in bytes for a {@code PIC [S]9(digits) COMP} field. */
  public static int byteLength(int digits) {
    if (digits < 1 || digits > 18) {
      throw new IllegalArgumentException("COMP digits must be 1..18, got " + digits);
    }
    if (digits <= 4) {
      return 2;
    }
    return digits <= 9 ? 4 : 8;
  }

  /** Largest magnitude representable by the picture ({@code 10^digits - 1}). */
  public static long maxMagnitude(int digits) {
    long max = 1;
    for (int i = 0; i < digits; i++) {
      max *= 10;
    }
    return max - 1;
  }

  /** Encodes {@code value} big-endian into {@code byteLength(digits)} bytes. */
  public static byte[] encode(long value, int digits, boolean signed) {
    long max = maxMagnitude(digits);
    if (value > max || value < (signed ? -max : 0)) {
      throw new IllegalArgumentException(
          "Value " + value + " does not fit PIC " + (signed ? "S" : "") + "9(" + digits + ") COMP");
    }
    int length = byteLength(digits);
    byte[] out = new byte[length];
    long v = value;
    for (int i = length - 1; i >= 0; i--) {
      out[i] = (byte) (v & 0xFF);
      v >>= 8;
    }
    return out;
  }

  /** Decodes a big-endian binary field; unsigned fields are widened without sign extension. */
  public static long decode(byte[] bytes, boolean signed) {
    long v = signed && bytes.length > 0 && bytes[0] < 0 ? -1L : 0L;
    for (byte b : bytes) {
      v = (v << 8) | (b & 0xFF);
    }
    return v;
  }
}
