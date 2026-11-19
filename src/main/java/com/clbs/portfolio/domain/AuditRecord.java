package com.clbs.portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Audit trail record (copybook AUDITLOG, written by AUDPROC / PORTDEL). */
@Entity
@Table(name = "AUDIT_LOG")
public class AuditRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "AUDIT_TS", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "SYSTEM_ID", length = 16)
    private String systemId;

    @Column(name = "USER_ID", length = 64)
    private String userId;

    @Column(name = "PROGRAM", length = 8)
    private String program;

    /** TRAN, USER or SYST. */
    @Column(name = "AUDIT_TYPE", length = 4, nullable = false)
    private String type;

    /** CREATE, UPDATE, DELETE, INQUIRE, ... */
    @Column(name = "ACTION", length = 8, nullable = false)
    private String action;

    /** SUCC, FAIL or WARN. */
    @Column(name = "STATUS", length = 4, nullable = false)
    private String status;

    @Column(name = "PORTFOLIO_ID", length = 8)
    private String portfolioId;

    @Column(name = "ACCOUNT_NO", length = 10)
    private String accountNo;

    @Column(name = "REASON_CODE", length = 2)
    private String reasonCode;

    @Column(name = "BEFORE_IMAGE", length = 400)
    private String beforeImage;

    @Column(name = "AFTER_IMAGE", length = 400)
    private String afterImage;

    @Column(name = "MESSAGE", length = 100)
    private String message;

    protected AuditRecord() {
    }

    public AuditRecord(LocalDateTime timestamp, String systemId, String userId, String program, String type,
                       String action, String status, String portfolioId, String accountNo, String reasonCode,
                       String beforeImage, String afterImage, String message) {
        this.timestamp = timestamp;
        this.systemId = systemId;
        this.userId = userId;
        this.program = program;
        this.type = type;
        this.action = action;
        this.status = status;
        this.portfolioId = portfolioId;
        this.accountNo = accountNo;
        this.reasonCode = reasonCode;
        this.beforeImage = truncate(beforeImage, 400);
        this.afterImage = truncate(afterImage, 400);
        this.message = truncate(message, 100);
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }

    public Long getId() { return id; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getSystemId() { return systemId; }
    public String getUserId() { return userId; }
    public String getProgram() { return program; }
    public String getType() { return type; }
    public String getAction() { return action; }
    public String getStatus() { return status; }
    public String getPortfolioId() { return portfolioId; }
    public String getAccountNo() { return accountNo; }
    public String getReasonCode() { return reasonCode; }
    public String getBeforeImage() { return beforeImage; }
    public String getAfterImage() { return afterImage; }
    public String getMessage() { return message; }
}
