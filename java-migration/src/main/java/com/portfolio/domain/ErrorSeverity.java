package com.portfolio.domain;

public enum ErrorSeverity {
  INFO(1),
  WARNING(2),
  ERROR(3),
  SEVERE(4);
  private final int code;

  ErrorSeverity(int code) {
    this.code = code;
  }

  public int getCode() {
    return code;
  }

  public static ErrorSeverity fromCode(int code) {
    for (var errorSeverity : values()) {
      if (errorSeverity.code == code) return errorSeverity;
    }
    throw new IllegalArgumentException("Unknown severity: " + code);
  }
}
