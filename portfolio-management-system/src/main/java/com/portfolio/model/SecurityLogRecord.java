package com.portfolio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Security/Audit Log Record entity.
 * Migrated from COBOL SECMGR.cbl (P300-LOG-ACCESS) and AUDITLOG table.
 */
@Entity
@Table(name = "AUDITLOG")
public class SecurityLogRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AUDIT_ID")
    private Long auditId;

    @Column(name = "AUDIT_TIMESTAMP", nullable = false)
    private LocalDateTime auditTimestamp;

    @Column(name = "USER_ID", length = 8, nullable = false)
    private String userId;

    @Column(name = "TERMINAL_ID", length = 4)
    private String terminalId;

    @Column(name = "TRANS_ID", length = 4)
    private String transId;

    @Column(name = "PROGRAM", length = 8)
    private String program;

    @Column(name = "ACCESS_TYPE", length = 8)
    private String accessType;

    @Column(name = "RESOURCE_NAME", length = 50)
    private String resourceName;

    @Column(name = "RESPONSE_CODE")
    private Integer responseCode;

    @Column(name = "DETAILS", length = 500)
    private String details;

    public SecurityLogRecord() {}

    // Getters and setters
    public Long getAuditId() { return auditId; }
    public void setAuditId(Long auditId) { this.auditId = auditId; }

    public LocalDateTime getAuditTimestamp() { return auditTimestamp; }
    public void setAuditTimestamp(LocalDateTime auditTimestamp) { this.auditTimestamp = auditTimestamp; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTerminalId() { return terminalId; }
    public void setTerminalId(String terminalId) { this.terminalId = terminalId; }

    public String getTransId() { return transId; }
    public void setTransId(String transId) { this.transId = transId; }

    public String getProgram() { return program; }
    public void setProgram(String program) { this.program = program; }

    public String getAccessType() { return accessType; }
    public void setAccessType(String accessType) { this.accessType = accessType; }

    public String getResourceName() { return resourceName; }
    public void setResourceName(String resourceName) { this.resourceName = resourceName; }

    public Integer getResponseCode() { return responseCode; }
    public void setResponseCode(Integer responseCode) { this.responseCode = responseCode; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
