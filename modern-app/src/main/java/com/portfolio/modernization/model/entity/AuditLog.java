package com.portfolio.modernization.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log", indexes = {
    @Index(name = "idx_audit_portfolio", columnList = "portfolio_id"),
    @Index(name = "idx_audit_user", columnList = "user_id, timestamp"),
    @Index(name = "idx_audit_action", columnList = "action_type, timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "system_id", length = 8, nullable = false)
    private String systemId;

    @Column(name = "user_id", length = 8, nullable = false)
    private String userId;

    @Column(name = "program_id", length = 8, nullable = false)
    private String programId;

    @Column(name = "terminal_id", length = 8)
    private String terminalId;

    @Column(name = "audit_type", length = 4, nullable = false)
    @Enumerated(EnumType.STRING)
    private AuditType auditType;

    @Column(name = "action_type", length = 8, nullable = false)
    @Enumerated(EnumType.STRING)
    private ActionType actionType;

    @Column(name = "status", length = 4, nullable = false)
    @Enumerated(EnumType.STRING)
    private AuditStatus status;

    @Column(name = "portfolio_id", length = 8)
    private String portfolioId;

    @Column(name = "account_number", length = 10)
    private String accountNumber;

    @Column(name = "before_image", length = 100)
    private String beforeImage;

    @Column(name = "after_image", length = 100)
    private String afterImage;

    @Column(name = "message", length = 100)
    private String message;

    public enum AuditType {
        TRAN, // Transaction
        USER, // User Action
        SYST  // System Event
    }

    public enum ActionType {
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
