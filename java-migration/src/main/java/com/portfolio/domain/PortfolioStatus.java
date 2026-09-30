package com.portfolio.domain;

public enum PortfolioStatus {
  ACTIVE("A"),
  CLOSED("C"),
  SUSPENDED("S");
  private final String code;

  PortfolioStatus(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }

  public static PortfolioStatus fromCode(String code) {
    for (var portfolioStatus : values()) {
      if (portfolioStatus.code.equals(code)) return portfolioStatus;
    }
    throw new IllegalArgumentException("Unknown portfolio status: " + code);
  }
}
