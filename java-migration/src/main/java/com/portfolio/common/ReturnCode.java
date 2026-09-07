package com.portfolio.common;

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

  public int value() {
    return value;
  }

  public static ReturnCode fromValue(int value) {
    for (ReturnCode code : values()) if (code.value == value) return code;
    throw new IllegalArgumentException("Unknown return code: " + value);
  }

  public static ReturnCode max(ReturnCode firstCode, ReturnCode secondCode) {
    return firstCode.value >= secondCode.value ? firstCode : secondCode;
  }
}
