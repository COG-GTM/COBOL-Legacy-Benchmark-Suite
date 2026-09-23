package com.cognition.clbs.common.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** Sequential AUDFILE as a table (V6); record layout {@code AUDITLOG.cpy}. */
@Entity
@Table(name = "AUDIT_LOG")
public class AuditLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "AUDIT_ID")
  private Long auditId;

  @Column(name = "AUDIT_TIMESTAMP", nullable = false)
  private LocalDateTime auditTimestamp;

  @Column(name = "SYSTEM_ID", nullable = false, length = 8)
  private String systemId;

  @Column(name = "USER_ID", nullable = false, length = 8)
  private String userId;

  @Column(name = "PROGRAM_ID", nullable = false, length = 8)
  private String program;

  @Column(name = "TERMINAL_ID", length = 8)
  private String terminal;

  @Column(name = "AUDIT_TYPE", nullable = false, length = 4)
  private String type;

  @Column(name = "AUDIT_ACTION", nullable = false, length = 8)
  private String action;

  @Column(name = "AUDIT_STATUS", nullable = false, length = 4)
  private String status;

  @Column(name = "PORTFOLIO_ID", length = 8)
  private String portfolioId;

  @Column(name = "ACCOUNT_NO", length = 10)
  private String accountNumber;

  @Column(name = "BEFORE_IMAGE", length = 100)
  private String beforeImage;

  @Column(name = "AFTER_IMAGE", length = 100)
  private String afterImage;

  @Column(name = "MESSAGE_TEXT", length = 100)
  private String message;

  protected AuditLog() {}

  public AuditLog(
      LocalDateTime auditTimestamp,
      String systemId,
      String userId,
      String program,
      String terminal,
      String type,
      String action,
      String status,
      String portfolioId,
      String accountNumber,
      String beforeImage,
      String afterImage,
      String message) {
    this.auditTimestamp = auditTimestamp;
    this.systemId = systemId;
    this.userId = userId;
    this.program = program;
    this.terminal = terminal;
    this.type = type;
    this.action = action;
    this.status = status;
    this.portfolioId = portfolioId;
    this.accountNumber = accountNumber;
    this.beforeImage = beforeImage;
    this.afterImage = afterImage;
    this.message = message;
  }

  public Long getAuditId() {
    return auditId;
  }

  public LocalDateTime getAuditTimestamp() {
    return auditTimestamp;
  }

  public void setAuditTimestamp(LocalDateTime auditTimestamp) {
    this.auditTimestamp = auditTimestamp;
  }

  public String getSystemId() {
    return systemId;
  }

  public void setSystemId(String systemId) {
    this.systemId = systemId;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getProgram() {
    return program;
  }

  public void setProgram(String program) {
    this.program = program;
  }

  public String getTerminal() {
    return terminal;
  }

  public void setTerminal(String terminal) {
    this.terminal = terminal;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getPortfolioId() {
    return portfolioId;
  }

  public void setPortfolioId(String portfolioId) {
    this.portfolioId = portfolioId;
  }

  public String getAccountNumber() {
    return accountNumber;
  }

  public void setAccountNumber(String accountNumber) {
    this.accountNumber = accountNumber;
  }

  public String getBeforeImage() {
    return beforeImage;
  }

  public void setBeforeImage(String beforeImage) {
    this.beforeImage = beforeImage;
  }

  public String getAfterImage() {
    return afterImage;
  }

  public void setAfterImage(String afterImage) {
    this.afterImage = afterImage;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
