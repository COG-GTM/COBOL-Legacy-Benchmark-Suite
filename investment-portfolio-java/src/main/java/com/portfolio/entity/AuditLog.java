package com.portfolio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "audit_timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "system_id", length = 8)
    @Size(max = 8)
    private String systemId;

    @Column(name = "user_id", length = 8)
    @Size(max = 8)
    private String userId;

    @Column(name = "program", length = 8)
    @Size(max = 8)
    private String program;

    @Column(name = "terminal", length = 8)
    @Size(max = 8)
    private String terminal;

    @Enumerated(EnumType.STRING)
    @Column(name = "audit_type", length = 20)
    private AuditType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "audit_action", length = 20)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "audit_status", length = 10)
    private AuditStatus status;

    @Column(name = "portfolio_id", length = 8)
    @Size(max = 8)
    private String portfolioId;

    @Column(name = "account_no", length = 10)
    @Size(max = 10)
    private String accountNo;

    @Column(name = "before_image", length = 100)
    @Size(max = 100)
    private String beforeImage;

    @Column(name = "after_image", length = 100)
    @Size(max = 100)
    private String afterImage;

    @Column(name = "message", length = 100)
    @Size(max = 100)
    private String message;

    public AuditLog() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
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

    public void setType(AuditType type) {
        this.type = type;
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
}
