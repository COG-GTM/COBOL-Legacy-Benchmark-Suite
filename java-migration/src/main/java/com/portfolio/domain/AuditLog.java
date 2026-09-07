package com.portfolio.domain;

import com.portfolio.domain.converter.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "AUDITLOG")
public class AuditLog {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "AUDIT_ID")
  private Long auditId;

  @Column(name = "AUD_TIMESTAMP")
  private LocalDateTime audTimestamp;

  @Column(name = "SYSTEM_ID", columnDefinition = "char(8)")
  private String systemId;

  @Column(name = "USER_ID", columnDefinition = "char(8)")
  private String userId;

  @Column(name = "PROGRAM", columnDefinition = "char(8)")
  private String program;

  @Column(name = "TERMINAL", columnDefinition = "char(8)")
  private String terminal;

  @Convert(converter = AuditTypeConverter.class)
  @Column(name = "AUD_TYPE", columnDefinition = "char(4)")
  private AuditType audType;

  @Convert(converter = AuditActionConverter.class)
  @Column(name = "ACTION", columnDefinition = "char(8)")
  private AuditAction action;

  @Convert(converter = AuditStatusConverter.class)
  @Column(name = "STATUS", columnDefinition = "char(4)")
  private AuditStatus status;

  @Column(name = "PORTFOLIO_ID", columnDefinition = "char(8)")
  private String portfolioId;

  @Column(name = "ACCOUNT_NO", columnDefinition = "char(10)")
  private String accountNo;

  @Column(name = "BEFORE_IMAGE", length = 100)
  private String beforeImage;

  @Column(name = "AFTER_IMAGE", length = 100)
  private String afterImage;

  @Column(length = 100)
  private String message;

  @Column(name = "TRANS_ID", columnDefinition = "char(4)")
  private String transId;

  @Column(name = "ACCESS_TYPE", columnDefinition = "char(8)")
  private String accessType;

  public AuditLog() {}

  public Long getAuditId() {
    return auditId;
  }

  public void setAuditId(Long auditId) {
    this.auditId = auditId;
  }

  public LocalDateTime getAudTimestamp() {
    return audTimestamp;
  }

  public void setAudTimestamp(LocalDateTime audTimestamp) {
    this.audTimestamp = audTimestamp;
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

  public AuditType getAudType() {
    return audType;
  }

  public void setAudType(AuditType audType) {
    this.audType = audType;
  }

  public AuditAction getAction() {
    return action;
  }

  public void setAction(AuditAction action) {
    this.action = action;
  }

  public AuditStatus getStatus() {
    return status;
  }

  public void setStatus(AuditStatus status) {
    this.status = status;
  }

  public String getPortfolioId() {
    return portfolioId;
  }

  public void setPortfolioId(String portfolioId) {
    this.portfolioId = portfolioId;
  }

  public String getAccountNo() {
    return accountNo;
  }

  public void setAccountNo(String accountNo) {
    this.accountNo = accountNo;
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

  public String getTransId() {
    return transId;
  }

  public void setTransId(String transId) {
    this.transId = transId;
  }

  public String getAccessType() {
    return accessType;
  }

  public void setAccessType(String accessType) {
    this.accessType = accessType;
  }
}
