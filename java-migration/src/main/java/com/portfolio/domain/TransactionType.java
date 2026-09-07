package com.portfolio.domain;

public enum TransactionType {
  BUY("BU"),
  SELL("SL"),
  TRANSFER("TR"),
  FEE("FE");
  private final String code;

  TransactionType(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }

  public static TransactionType fromCode(String code) {
    for (var transactionType : values()) {
      if (transactionType.code.equals(code)) return transactionType;
    }
    throw new IllegalArgumentException("Unknown transaction type: " + code);
  }
}
