package com.portfolio.domain;

public enum AuditStatus {
  SUCCESS("SUCC"),
  FAILURE("FAIL"),
  WARNING("WARN");
  private final String code;

  AuditStatus(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }

  public static AuditStatus fromCode(String code) {
    for (var auditStatus : values()) {
      if (auditStatus.code.equals(code.trim())) return auditStatus;
    }
    throw new IllegalArgumentException("Unknown audit status: " + code);
  }
}
