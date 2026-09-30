package com.portfolio.domain;

public enum AuditAction {
  CREATE("CREATE"),
  UPDATE("UPDATE"),
  DELETE("DELETE"),
  INQUIRE("INQUIRE"),
  LOGIN("LOGIN"),
  LOGOUT("LOGOUT"),
  STARTUP("STARTUP"),
  SHUTDOWN("SHUTDOWN");
  private final String code;

  AuditAction(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }

  public static AuditAction fromCode(String code) {
    for (var auditAction : values()) {
      if (auditAction.code.equals(code.trim())) return auditAction;
    }
    throw new IllegalArgumentException("Unknown audit action: " + code);
  }
}
