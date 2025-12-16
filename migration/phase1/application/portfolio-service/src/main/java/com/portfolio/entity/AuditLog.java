package com.portfolio.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * AuditLog entity - migrated from AUDITLOG DB2 table.
 * 
 * Original COBOL structure from AUDITLOG.cpy:
 * - AUD-TIMESTAMP: Timestamp of the audit event
 * - AUD-SYSTEM-ID: System identifier
 * - AUD-USER-ID: User who performed the action
 * - AUD-PROGRAM: Program that generated the audit
 * - AUD-TERMINAL: Terminal ID (for CICS transactions)
 * - AUD-TYPE: Event type (TRAN, USER, SYST)
 * - AUD-ACTION: Action performed (CREATE, UPDATE, DELETE, etc.)
 * - AUD-STATUS: Result status (SUCC, FAIL, WARN)
 * 
 * @see src/copybook/common/AUDITLOG.cpy
 * @see src/programs/online/SECMGR.cbl - P300-LOG-ACCESS paragraph
 */
@Entity
@Table(name = "audit_log", schema = "portfolio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "audit_timestamp", nullable = false)
    @Builder.Default
    private OffsetDateTime auditTimestamp = OffsetDateTime.now();

    @Column(name = "system_id", length = 8)
    private String systemId;

    @Column(name = "user_id", nullable = false, length = 8)
    private String userId;

    @Column(name = "program_id", length = 8)
    private String programId;

    @Column(name = "terminal_id", length = 8)
    private String terminalId;

    @Column(name = "transaction_id", length = 4)
    private String transactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private AuditEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AuditStatus status;

    @Column(name = "portfolio_id", length = 8)
    private String portfolioId;

    @Column(name = "account_no", length = 10)
    private String accountNo;

    @Column(name = "resource_name", length = 50)
    private String resourceName;

    @Column(name = "access_type", length = 8)
    private String accessType;

    @Column(name = "before_image", columnDefinition = "TEXT")
    private String beforeImage;

    @Column(name = "after_image", columnDefinition = "TEXT")
    private String afterImage;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public enum AuditEventType {
        TRANSACTION,
        USER_ACTION,
        SYSTEM_EVENT
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
        SUCCESS,
        FAILURE,
        WARNING
    }
}
