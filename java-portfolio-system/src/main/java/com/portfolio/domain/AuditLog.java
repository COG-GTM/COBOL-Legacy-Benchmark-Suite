package com.portfolio.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Audit Log entity - migrated from COBOL copybook AUDITLOG.cpy
 * Represents security and access audit trail
 */
@Entity
@Table(name = "audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "system_id", length = 8)
    private String systemId;

    @Column(name = "user_id", length = 8)
    private String userId;

    @Column(name = "program", length = 8)
    private String program;

    @Column(name = "terminal", length = 8)
    private String terminal;

    @Enumerated(EnumType.STRING)
    @Column(name = "audit_type", length = 4)
    private AuditType auditType;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", length = 8)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 4)
    private AuditStatus status;

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

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }

    public enum AuditType {
        TRAN, // Transaction
        USER, // User Action
        SYST  // System Event
    }

    public enum AuditAction {
        CREATE,
        UPDATE,
        DELETE,
        INQUIRE,
        LOGIN,
        LOGOUT,
        STARTUP,
        SHUTDOWN
    }

    public enum AuditStatus {
        SUCC, // Success
        FAIL, // Failure
        WARN  // Warning
    }
}
