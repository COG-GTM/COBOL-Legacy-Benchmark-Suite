package com.benchmark.portfolio.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Audit log record, migrated from AUDITLOG.cpy (AUDIT-RECORD).
 * Insert-only audit trail with surrogate identity key AUDIT_LOG_ID (no VSAM key).
 */
@Entity
@Table(name = "AUDIT_LOG")
public class AuditLog {

    /** Surrogate identity key (log table; no copybook origin). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AUDIT_LOG_ID")
    private Long auditLogId;

    /** AUD-TIMESTAMP PIC X(26) (DB2 timestamp format). */
    @Column(name = "AUDIT_TIMESTAMP", nullable = false)
    private LocalDateTime auditTimestamp;

    /** AUD-SYSTEM-ID PIC X(8). */
    @Column(name = "SYSTEM_ID", length = 8, nullable = false)
    private String systemId;

    /** AUD-USER-ID PIC X(8). */
    @Column(name = "USER_ID", length = 8, nullable = false)
    private String userId;

    /** AUD-PROGRAM PIC X(8). */
    @Column(name = "PROGRAM_ID", length = 8, nullable = false)
    private String programId;

    /** AUD-TERMINAL PIC X(8). */
    @Column(name = "TERMINAL_ID", length = 8)
    private String terminalId;

    /** AUD-TYPE PIC X(4); 88-levels: 'TRAN', 'USER', 'SYST'. */
    @Column(name = "AUDIT_TYPE", columnDefinition = "CHAR(4)", length = 4, nullable = false)
    private String auditType;

    /**
     * AUD-ACTION PIC X(8); 88-levels: 'CREATE', 'UPDATE', 'DELETE', 'INQUIRE',
     * 'LOGIN', 'LOGOUT', 'STARTUP', 'SHUTDOWN' — space-padded to 8 chars in COBOL.
     */
    @Column(name = "AUDIT_ACTION", columnDefinition = "CHAR(8)", length = 8, nullable = false)
    private String auditAction;

    /** AUD-STATUS PIC X(4); 88-levels: 'SUCC', 'FAIL', 'WARN'. */
    @Column(name = "AUDIT_STATUS", columnDefinition = "CHAR(4)", length = 4, nullable = false)
    private String auditStatus;

    /** AUD-PORTFOLIO-ID PIC X(8) (optional reference to portfolio). */
    @Column(name = "PORTFOLIO_ID", columnDefinition = "CHAR(8)", length = 8)
    private String portfolioId;

    /** AUD-ACCOUNT-NO PIC X(10) (optional reference to account). */
    @Column(name = "ACCOUNT_NO", columnDefinition = "CHAR(10)", length = 10)
    private String accountNo;

    /** AUD-BEFORE-IMAGE PIC X(100). */
    @Column(name = "BEFORE_IMAGE", length = 100)
    private String beforeImage;

    /** AUD-AFTER-IMAGE PIC X(100). */
    @Column(name = "AFTER_IMAGE", length = 100)
    private String afterImage;

    /** AUD-MESSAGE PIC X(100). */
    @Column(name = "AUDIT_MESSAGE", length = 100)
    private String auditMessage;

    public Long getAuditLogId() {
        return auditLogId;
    }

    public void setAuditLogId(Long auditLogId) {
        this.auditLogId = auditLogId;
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

    public String getProgramId() {
        return programId;
    }

    public void setProgramId(String programId) {
        this.programId = programId;
    }

    public String getTerminalId() {
        return terminalId;
    }

    public void setTerminalId(String terminalId) {
        this.terminalId = terminalId;
    }

    public String getAuditType() {
        return auditType;
    }

    public void setAuditType(String auditType) {
        this.auditType = auditType;
    }

    public String getAuditAction() {
        return auditAction;
    }

    public void setAuditAction(String auditAction) {
        this.auditAction = auditAction;
    }

    public String getAuditStatus() {
        return auditStatus;
    }

    public void setAuditStatus(String auditStatus) {
        this.auditStatus = auditStatus;
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

    public String getAuditMessage() {
        return auditMessage;
    }

    public void setAuditMessage(String auditMessage) {
        this.auditMessage = auditMessage;
    }
}
