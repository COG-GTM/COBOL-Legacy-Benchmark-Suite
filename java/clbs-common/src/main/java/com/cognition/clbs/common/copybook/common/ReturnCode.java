package com.cognition.clbs.common.copybook.common;

/**
 * {@code RETURN-CODES} from {@code COMMON.cpy} (also repeated as 88-levels in RETHND, ERRHAND and
 * BCHCON): the standard z/OS step condition codes.
 */
public enum ReturnCode {
  SUCCESS(0),
  WARNING(4),
  ERROR(8),
  SEVERE(12),
  CRITICAL(16);

  private final int value;

  ReturnCode(int value) {
    this.value = value;
  }

  /** The numeric condition code ({@code PIC S9(4)}). */
  public int value() {
    return value;
  }

  /** Resolves a numeric condition code; anything other than 0/4/8/12/16 is rejected. */
  public static ReturnCode fromValue(int value) {
    for (ReturnCode code : values()) {
      if (code.value == value) {
        return code;
      }
    }
    throw new IllegalArgumentException("Unknown return code " + value);
  }

  /** The more severe of two codes, mirroring the highest-return-code tracking in RTNCODE. */
  public ReturnCode max(ReturnCode other) {
    return value >= other.value ? this : other;
  }
}
