package com.portfolio.domain;

public enum ClientType {
  INDIVIDUAL("I"),
  CORPORATE("C"),
  TRUST("T");
  private final String code;

  ClientType(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }

  public static ClientType fromCode(String code) {
    for (var clientType : values()) {
      if (clientType.code.equals(code)) return clientType;
    }
    throw new IllegalArgumentException("Unknown client type: " + code);
  }
}
