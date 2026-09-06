package com.cog.portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** AUDITLOG.cpy AUDIT-RECORD (sequential AUDFILE written by AUDPROC). */
@Entity
@Table(name = "AUDIT_LOG")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AUDIT_ID")
    private Long id;

    @Column(name = "AUD_TIMESTAMP", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "SYSTEM_ID", length = 8)
    private String systemId;

    @Column(name = "USER_ID", length = 8)
    private String userId;

    @Column(name = "PROGRAM", length = 8)
    private String program;

    @Column(name = "TERMINAL", length = 8)
    private String terminal;

    @Convert(converter = AuditType.Converter.class)
    @Column(name = "AUD_TYPE", length = 4, nullable = false)
    private AuditType type;

    @Convert(converter = AuditAction.Converter.class)
    @Column(name = "AUD_ACTION", length = 8, nullable = false)
    private AuditAction action;

    @Convert(converter = AuditStatus.Converter.class)
    @Column(name = "AUD_STATUS", length = 4, nullable = false)
    private AuditStatus status;

    @Column(name = "PORTFOLIO_ID", length = 8)
    private String portfolioId;

    @Column(name = "ACCOUNT_NO", length = 10)
    private String accountNo;

    @Column(name = "BEFORE_IMAGE", length = 100)
    private String beforeImage;

    @Column(name = "AFTER_IMAGE", length = 100)
    private String afterImage;

    @Column(name = "MESSAGE", length = 100)
    private String message;

    protected AuditLog() {
    }

    public AuditLog(LocalDateTime timestamp, AuditType type, AuditAction action, AuditStatus status) {
        this.timestamp = timestamp;
        this.type = type;
        this.action = action;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
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

    public AuditType getType() {
        return type;
    }

    public AuditAction getAction() {
        return action;
    }

    public AuditStatus getStatus() {
        return status;
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
}
