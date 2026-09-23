package com.cognition.clbs.common.copybook.common;

import java.math.BigDecimal;

/**
 * Constants from {@code PORTVAL.cpy}: validation return codes, messages and limits used by the
 * portfolio maintenance programs.
 */
public final class PortfolioValidation {

  /** {@code VAL-MIN-AMOUNT PIC S9(13)V99 VALUE -9999999999999.99}. */
  public static final BigDecimal MIN_AMOUNT = new BigDecimal("-9999999999999.99");

  /** {@code VAL-MAX-AMOUNT PIC S9(13)V99 VALUE +9999999999999.99}. */
  public static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999999.99");

  /** {@code VAL-ID-PREFIX PIC X(4) VALUE 'PORT'}. */
  public static final String ID_PREFIX = "PORT";

  /** Width of {@code VAL-ERROR-MSG} and the {@code VAL-ERROR-MESSAGES} literals. */
  public static final int MESSAGE_LENGTH = 50;

  private PortfolioValidation() {}

  /** {@code VAL-RETURN-CODES} paired with their {@code VAL-ERROR-MESSAGES}. */
  public enum Result {
    SUCCESS(0, ""),
    INVALID_ID(1, "Invalid Portfolio ID format"),
    INVALID_ACCOUNT(2, "Invalid Account Number format"),
    INVALID_TYPE(3, "Invalid Investment Type"),
    INVALID_AMOUNT(4, "Amount outside valid range");

    private final int code;
    private final String message;

    Result(int code, String message) {
      this.code = code;
      this.message = message;
    }

    /** {@code PIC S9(4)} validation return code. */
    public int code() {
      return code;
    }

    /** {@code PIC X(50)} message text. */
    public String message() {
      return message;
    }

    /** Resolves a validation return code. */
    public static Result fromCode(int code) {
      for (Result result : values()) {
        if (result.code == code) {
          return result;
        }
      }
      throw new IllegalArgumentException("Unknown validation return code " + code);
    }
  }

  /** {@code VAL-MIN-AMOUNT <= amount <= VAL-MAX-AMOUNT}. */
  public static boolean isAmountInRange(BigDecimal amount) {
    return amount != null && amount.compareTo(MIN_AMOUNT) >= 0 && amount.compareTo(MAX_AMOUNT) <= 0;
  }
}
