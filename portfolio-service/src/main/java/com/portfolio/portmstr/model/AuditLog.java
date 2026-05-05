package com.portfolio.portmstr.model;

import com.portfolio.portmstr.model.enums.AuditAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Audit Log entity.
 * Mapped from COBOL copybook AUDITLOG.cpy (AUDIT-RECORD).
 */
@Entity
@Table(name = "AUDIT_LOG")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "AUDIT_TIMESTAMP", nullable = false)
    private LocalDateTime auditTimestamp;

    @Column(name = "SYSTEM_ID", length = 8)
    private String systemId;

    @Column(name = "USER_ID", length = 8)
    private String userId;

    @Column(name = "PROGRAM_NAME", length = 8)
    private String programName;

    @Column(name = "TERMINAL_ID", length = 8)
    private String terminalId;

    @Column(name = "AUDIT_TYPE", length = 4)
    private String auditType;

    @Column(name = "AUDIT_ACTION", length = 8)
    @Enumerated(EnumType.STRING)
    private AuditAction auditAction;

    @Column(name = "AUDIT_STATUS", length = 4)
    private String auditStatus;

    @Column(name = "PORTFOLIO_ID", length = 8)
    private String portfolioId;

    @Column(name = "ACCOUNT_NO", length = 10)
    private String accountNo;

    @Column(name = "BEFORE_IMAGE", length = 500)
    private String beforeImage;

    @Column(name = "AFTER_IMAGE", length = 500)
    private String afterImage;

    @Column(name = "MESSAGE", length = 200)
    private String message;

    public AuditLog() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getProgramName() {
        return programName;
    }

    public void setProgramName(String programName) {
        this.programName = programName;
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

    public AuditAction getAuditAction() {
        return auditAction;
    }

    public void setAuditAction(AuditAction auditAction) {
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
