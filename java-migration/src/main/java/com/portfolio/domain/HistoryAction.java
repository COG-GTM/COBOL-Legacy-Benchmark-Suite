package com.portfolio.domain;

public enum HistoryAction {
  ADD("A"),
  CHANGE("C"),
  DELETE("D");
  private final String code;

  HistoryAction(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }

  public static HistoryAction fromCode(String code) {
    for (var historyAction : values()) {
      if (historyAction.code.equals(code)) return historyAction;
    }
    throw new IllegalArgumentException("Unknown history action: " + code);
  }
}
