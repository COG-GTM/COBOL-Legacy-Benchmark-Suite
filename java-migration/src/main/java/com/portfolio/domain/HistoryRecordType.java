package com.portfolio.domain;

public enum HistoryRecordType {
  PORTFOLIO("PT"),
  POSITION("PS"),
  TRANSACTION("TR");
  private final String code;

  HistoryRecordType(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }

  public static HistoryRecordType fromCode(String code) {
    for (var historyRecordType : values()) {
      if (historyRecordType.code.equals(code)) return historyRecordType;
    }
    throw new IllegalArgumentException("Unknown history record type: " + code);
  }
}
