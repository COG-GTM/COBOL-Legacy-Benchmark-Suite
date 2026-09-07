package com.portfolio.common;

public enum StatusCode {
  ACTIVE("A"),
  CLOSED("C"),
  PENDING("P"),
  SUSPENDED("S"),
  FAILED("F"),
  REVERSED("R");
  private final String code;

  StatusCode(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}
