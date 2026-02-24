package com.portfolio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Audit Log entity - replaces the COBOL SECMGR audit logging (AUDITLOG table).
 * Source: src/programs/online/SECMGR.cbl P300-LOG-ACCESS
 */
@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "audit_timestamp", nullable = false)
    private LocalDateTime auditTimestamp = LocalDateTime.now();

    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;

    @Column(name = "terminal_id", length = 4)
    private String terminalId;

    @Column(name = "transaction_id", length = 4)
    private String transactionId;

    @Column(name = "program_name", length = 8)
    private String programName;

    @Column(name = "access_type", length = 8)
    private String accessType;

    @Column(name = "resource_name", length = 50)
    private String resourceName;

    @Column(name = "action", length = 50)
    private String action;

    @Column(name = "details", length = 500)
    private String details;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    public AuditLog() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getAuditTimestamp() { return auditTimestamp; }
    public void setAuditTimestamp(LocalDateTime auditTimestamp) { this.auditTimestamp = auditTimestamp; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getTerminalId() { return terminalId; }
    public void setTerminalId(String terminalId) { this.terminalId = terminalId; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getProgramName() { return programName; }
    public void setProgramName(String programName) { this.programName = programName; }
    public String getAccessType() { return accessType; }
    public void setAccessType(String accessType) { this.accessType = accessType; }
    public String getResourceName() { return resourceName; }
    public void setResourceName(String resourceName) { this.resourceName = resourceName; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
}
