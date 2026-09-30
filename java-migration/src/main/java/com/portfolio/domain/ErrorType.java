package com.portfolio.domain;

public enum ErrorType {
  SYSTEM("S"),
  APPLICATION("A"),
  DATA("D");
  private final String code;

  ErrorType(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }

  public static ErrorType fromCode(String code) {
    for (var errorType : values()) {
      if (errorType.code.equals(code)) return errorType;
    }
    throw new IllegalArgumentException("Unknown error type: " + code);
  }
}
