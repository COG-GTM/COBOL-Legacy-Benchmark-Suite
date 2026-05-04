package com.portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Audit Log entity - migrated from COBOL AUDITLOG.cpy.
 *
 * COBOL level-88 mappings:
 * - AUD-TYPE: TRAN=Transaction, USER=UserAction, SYST=SystemEvent
 * - AUD-ACTION: CREATE, UPDATE, DELETE, INQUIRE, LOGIN, LOGOUT, STARTUP, SHUTDOWN
 * - AUD-STATUS: SUCC=Success, FAIL=Failure, WARN=Warning
 */
@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "audit_timestamp", nullable = false)
    private LocalDateTime auditTimestamp;

    @Column(name = "system_id", length = 8, nullable = false)
    private String systemId;

    @Column(name = "user_id", length = 8, nullable = false)
    private String userId;

    @Column(name = "program", length = 8, nullable = false)
    private String program;

    @Column(name = "terminal", length = 8)
    private String terminal;

    @Column(name = "audit_type", length = 4, nullable = false)
    private String auditType;

    @Column(name = "action", length = 8, nullable = false)
    private String action;

    @Column(name = "status", length = 4, nullable = false)
    private String status;

    @Column(name = "portfolio_id", length = 8)
    private String portfolioId;

    @Column(name = "account_no", length = 10)
    private String accountNo;

    @Column(name = "before_image", length = 100)
    private String beforeImage;

    @Column(name = "after_image", length = 100)
    private String afterImage;

    @Column(name = "message", length = 100)
    private String message;

    public AuditLog() {
        this.auditTimestamp = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getAuditTimestamp() { return auditTimestamp; }
    public void setAuditTimestamp(LocalDateTime auditTimestamp) { this.auditTimestamp = auditTimestamp; }
    public String getSystemId() { return systemId; }
    public void setSystemId(String systemId) { this.systemId = systemId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getProgram() { return program; }
    public void setProgram(String program) { this.program = program; }
    public String getTerminal() { return terminal; }
    public void setTerminal(String terminal) { this.terminal = terminal; }
    public String getAuditType() { return auditType; }
    public void setAuditType(String auditType) { this.auditType = auditType; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }
    public String getAccountNo() { return accountNo; }
    public void setAccountNo(String accountNo) { this.accountNo = accountNo; }
    public String getBeforeImage() { return beforeImage; }
    public void setBeforeImage(String beforeImage) { this.beforeImage = beforeImage; }
    public String getAfterImage() { return afterImage; }
    public void setAfterImage(String afterImage) { this.afterImage = afterImage; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
