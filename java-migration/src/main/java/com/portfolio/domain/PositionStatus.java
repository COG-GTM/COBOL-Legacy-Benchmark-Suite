package com.portfolio.domain;

public enum PositionStatus {
  ACTIVE("A"),
  CLOSED("C"),
  PENDING("P");
  private final String code;

  PositionStatus(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }

  public static PositionStatus fromCode(String code) {
    for (var positionStatus : values()) {
      if (positionStatus.code.equals(code)) return positionStatus;
    }
    throw new IllegalArgumentException("Unknown position status: " + code);
  }
}
