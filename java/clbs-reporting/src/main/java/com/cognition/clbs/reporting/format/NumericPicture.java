package com.cognition.clbs.reporting.format;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * A COBOL numeric-edited {@code PICTURE} clause and the semantics of {@code MOVE}-ing a numeric
 * value into it.
 *
 * <p>Supported symbols are the ones used by the report programs: {@code 9} (digit), {@code Z}
 * (zero-suppressed digit), floating {@code $}, a single leading {@code +} or {@code -} sign, and
 * the insertion characters {@code ,} and {@code .}. Values are truncated (never rounded) to the
 * picture's scale and high-order digits that do not fit are dropped, exactly as a COBOL {@code
 * MOVE} does. Unsigned pictures print the absolute value.
 */
public final class NumericPicture {

  private final String picture;
  private final char sign;
  private final int integerDigits;
  private final int scale;
  private final boolean floatingDollar;
  private final int decimalPoint;

  private NumericPicture(String picture) {
    if (picture == null || picture.isEmpty()) {
      throw new IllegalArgumentException("picture must not be empty");
    }
    this.picture = picture;
    int start = 0;
    char first = picture.charAt(0);
    if (first == '+' || first == '-') {
      sign = first;
      start = 1;
    } else {
      sign = 0;
    }
    int point = picture.indexOf('.');
    decimalPoint = point;
    int integerEnd = point < 0 ? picture.length() : point;
    int digits = 0;
    int dollars = 0;
    for (int i = start; i < integerEnd; i++) {
      char c = picture.charAt(i);
      switch (c) {
        case '9', 'Z' -> digits++;
        case '$' -> dollars++;
        case ',' -> {
          // insertion only
        }
        default -> throw new IllegalArgumentException("Unsupported picture symbol: " + picture);
      }
    }
    int fraction = 0;
    for (int i = integerEnd + 1; point >= 0 && i < picture.length(); i++) {
      if (picture.charAt(i) != '9') {
        throw new IllegalArgumentException("Only 9 is supported after the point: " + picture);
      }
      fraction++;
    }
    floatingDollar = dollars > 0;
    integerDigits = digits + (floatingDollar ? dollars - 1 : 0);
    scale = fraction;
  }

  /** Parses a picture such as {@code ZZZ,ZZZ,ZZ9.99} or {@code $$$$,$$$,$$9.99}. */
  public static NumericPicture of(String picture) {
    return new NumericPicture(picture);
  }

  /** Width of the edited field in characters. */
  public int length() {
    return picture.length();
  }

  public String picture() {
    return picture;
  }

  /** Edits {@code value} into this picture following COBOL MOVE rules. */
  public String format(BigDecimal value) {
    BigDecimal truncated =
        (value == null ? BigDecimal.ZERO : value).setScale(scale, RoundingMode.DOWN);
    boolean negative = truncated.signum() < 0;
    BigInteger unscaled = truncated.abs().unscaledValue();
    BigInteger modulus = BigInteger.TEN.pow(integerDigits + scale);
    String digits = unscaled.mod(modulus).toString();
    digits = "0".repeat(integerDigits + scale - digits.length()) + digits;

    char[] out = new char[picture.length()];
    int digitIndex = 0;
    boolean significant = false;
    boolean seenDollar = false;
    int firstSignificant = -1;
    for (int i = 0; i < picture.length(); i++) {
      char c = picture.charAt(i);
      switch (c) {
        case '+' -> out[i] = negative ? '-' : '+';
        case '-' -> out[i] = negative ? '-' : ' ';
        case '9' -> {
          out[i] = digits.charAt(digitIndex++);
          significant = true;
        }
        case 'Z' -> {
          char d = digits.charAt(digitIndex++);
          significant |= d != '0';
          out[i] = significant ? d : ' ';
        }
        case '$' -> {
          if (!seenDollar) {
            seenDollar = true;
            out[i] = ' ';
          } else {
            char d = digits.charAt(digitIndex++);
            significant |= d != '0';
            out[i] = significant ? d : ' ';
          }
        }
        case ',' -> out[i] = significant ? ',' : ' ';
        case '.' -> {
          out[i] = '.';
          significant = true;
        }
        default -> throw new IllegalStateException("Unexpected picture symbol " + c);
      }
      if (significant && firstSignificant < 0 && c != '+' && c != '-') {
        firstSignificant = i;
      }
    }
    if (floatingDollar) {
      int at = firstSignificant >= 0 ? firstSignificant - 1 : decimalPoint - 1;
      out[at] = '$';
    }
    return new String(out);
  }

  /** Convenience overload for integral counters. */
  public String format(long value) {
    return format(BigDecimal.valueOf(value));
  }

  @Override
  public String toString() {
    return "PIC " + picture;
  }
}
