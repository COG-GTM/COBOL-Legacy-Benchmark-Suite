package com.clbs.portfolio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Audit trail record.
 * From COBOL copybook: src/copybook/common/AUDITLOG.cpy (AUDIT-RECORD).
 */
@Entity
@Table(name = "audit_record")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** AUD-TIMESTAMP — PIC X(26) */
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    /** AUD-SYSTEM-ID — PIC X(8) */
    @Column(name = "system_id", length = 8)
    private String systemId;

    /** AUD-USER-ID — PIC X(8) */
    @Column(name = "user_id", length = 8)
    private String userId;

    /** AUD-PROGRAM — PIC X(8) */
    @Column(name = "program", length = 8)
    private String program;

    /** AUD-TERMINAL — PIC X(8) */
    @Column(name = "terminal", length = 8)
    private String terminal;

    /** AUD-TYPE — PIC X(4): TRAN, USER, SYST */
    @Enumerated(EnumType.STRING)
    @Column(name = "audit_type", length = 20, nullable = false)
    private AuditType auditType;

    /** AUD-ACTION — PIC X(8): CREATE, UPDATE, DELETE, INQUIRE, LOGIN, LOGOUT, STARTUP, SHUTDOWN */
    @Enumerated(EnumType.STRING)
    @Column(name = "action", length = 10, nullable = false)
    private AuditAction action;

    /** AUD-STATUS — PIC X(4): SUCC, FAIL, WARN */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 10, nullable = false)
    private AuditStatus status;

    /** AUD-PORTFOLIO-ID — PIC X(8) */
    @Column(name = "portfolio_id", length = 8)
    private String portfolioId;

    /** AUD-ACCOUNT-NO — PIC X(10) */
    @Column(name = "account_no", length = 10)
    private String accountNo;

    /** AUD-BEFORE-IMAGE — PIC X(100) */
    @Column(name = "before_image", length = 100)
    private String beforeImage;

    /** AUD-AFTER-IMAGE — PIC X(100) */
    @Column(name = "after_image", length = 100)
    private String afterImage;

    /** AUD-MESSAGE — PIC X(100) */
    @Column(name = "message", length = 100)
    private String message;

    public enum AuditType {
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
