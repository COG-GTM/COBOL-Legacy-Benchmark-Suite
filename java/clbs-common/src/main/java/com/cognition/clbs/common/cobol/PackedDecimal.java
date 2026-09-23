package com.cognition.clbs.common.cobol;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * COMP-3 (packed decimal) codec. A {@code PIC S9(p)V9(s) COMP-3} field occupies {@code (p + s) / 2
 * + 1} bytes: two digits per byte, with the sign in the low nibble of the last byte ({@code 0xC}
 * positive, {@code 0xD} negative, {@code 0xF} unsigned).
 */
public final class PackedDecimal {

  private static final int POSITIVE = 0xC;
  private static final int NEGATIVE = 0xD;
  private static final int UNSIGNED = 0xF;

  private PackedDecimal() {}

  /** Number of bytes a packed field with the given total digit count occupies. */
  public static int byteLength(int digits) {
    if (digits < 1 || digits > 31) {
      throw new IllegalArgumentException("COMP-3 digits must be 1..31, got " + digits);
    }
    return digits / 2 + 1;
  }

  /**
   * Encodes {@code value} as a signed packed decimal with {@code digits} total digits, {@code
   * scale} of which are fractional. The value is rescaled with {@link
   * java.math.RoundingMode#UNNECESSARY}, so callers must supply values that already fit the
   * picture.
   */
  public static byte[] encode(BigDecimal value, int digits, int scale) {
    BigInteger unscaled = value.setScale(scale, java.math.RoundingMode.UNNECESSARY).unscaledValue();
    String magnitude = unscaled.abs().toString();
    if (magnitude.length() > digits) {
      throw new IllegalArgumentException(
          "Value " + value + " has " + magnitude.length() + " digits, picture allows " + digits);
    }
    int length = byteLength(digits);
    // Digit slots = 2 * length - 1; left-pad with zeros to fill the leading slots.
    StringBuilder padded = new StringBuilder(2 * length - 1);
    for (int i = magnitude.length(); i < 2 * length - 1; i++) {
      padded.append('0');
    }
    padded.append(magnitude);
    byte[] out = new byte[length];
    int pos = 0;
    for (int i = 0; i < length - 1; i++) {
      int hi = padded.charAt(pos++) - '0';
      int lo = padded.charAt(pos++) - '0';
      out[i] = (byte) ((hi << 4) | lo);
    }
    int last = padded.charAt(pos) - '0';
    int sign = unscaled.signum() < 0 ? NEGATIVE : POSITIVE;
    out[length - 1] = (byte) ((last << 4) | sign);
    return out;
  }

  /** Decodes a packed decimal into a {@link BigDecimal} with the given scale. */
  public static BigDecimal decode(byte[] packed, int scale) {
    if (packed.length == 0) {
      throw new IllegalArgumentException("Packed field must be at least one byte");
    }
    StringBuilder digits = new StringBuilder(packed.length * 2);
    for (int i = 0; i < packed.length - 1; i++) {
      digits.append(nibble(packed[i] >> 4)).append(nibble(packed[i]));
    }
    int last = packed[packed.length - 1];
    digits.append(nibble(last >> 4));
    int sign = last & 0x0F;
    if (sign != POSITIVE && sign != NEGATIVE && sign != UNSIGNED) {
      throw new IllegalArgumentException(String.format("Invalid COMP-3 sign nibble 0x%X", sign));
    }
    BigInteger unscaled = new BigInteger(digits.toString());
    if (sign == NEGATIVE) {
      unscaled = unscaled.negate();
    }
    return new BigDecimal(unscaled, scale);
  }

  private static char nibble(int value) {
    int digit = value & 0x0F;
    if (digit > 9) {
      throw new IllegalArgumentException(String.format("Invalid COMP-3 digit nibble 0x%X", digit));
    }
    return (char) ('0' + digit);
  }
}
