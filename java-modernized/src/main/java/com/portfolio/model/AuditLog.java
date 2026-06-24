package com.portfolio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * JPA entity mapped from COBOL copybook AUDITLOG.cpy (Audit Trail Record).
 * <p>
 * COBOL record layout:
 * <pre>
 * 01  AUDIT-RECORD.
 *     05  AUD-HEADER.
 *         10  AUD-TIMESTAMP     PIC X(26)
 *         10  AUD-SYSTEM-ID     PIC X(8)
 *         10  AUD-USER-ID       PIC X(8)
 *         10  AUD-PROGRAM       PIC X(8)
 *         10  AUD-TERMINAL      PIC X(8)
 *     05  AUD-TYPE             PIC X(4)   [TRAN, USER, SYST]
 *     05  AUD-ACTION           PIC X(8)   [CREATE, UPDATE, DELETE, INQUIRE, ...]
 *     05  AUD-STATUS           PIC X(4)   [SUCC, FAIL, WARN]
 *     05  AUD-KEY-INFO.
 *         10  AUD-PORTFOLIO-ID  PIC X(8)
 *         10  AUD-ACCOUNT-NO    PIC X(10)
 *     05  AUD-BEFORE-IMAGE     PIC X(100)
 *     05  AUD-AFTER-IMAGE      PIC X(100)
 *     05  AUD-MESSAGE          PIC X(100)
 * </pre>
 */
@Entity
@Table(name = "audit_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** AUD-TIMESTAMP — PIC X(26). Audit event timestamp. */
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    /** AUD-SYSTEM-ID — PIC X(8). Originating system identifier. */
    @Column(name = "system_id", length = 8)
    private String systemId;

    /** AUD-USER-ID — PIC X(8). User who triggered the action. */
    @Column(name = "user_id", length = 8)
    private String userId;

    /** AUD-PROGRAM — PIC X(8). COBOL program name (or Java class equivalent). */
    @Column(name = "program", length = 8)
    private String program;

    /** AUD-TYPE — PIC X(4). TRAN=Transaction, USER=User action, SYST=System event. */
    @Column(name = "audit_type", length = 4, nullable = false)
    private String auditType;

    /** AUD-ACTION — PIC X(8). CREATE, UPDATE, DELETE, INQUIRE, etc. */
    @Column(name = "action", length = 8, nullable = false)
    private String action;

    /** AUD-STATUS — PIC X(4). SUCC=Success, FAIL=Failure, WARN=Warning. */
    @Column(name = "status", length = 4, nullable = false)
    private String auditStatus;

    /** AUD-PORTFOLIO-ID — PIC X(8). Portfolio referenced by this audit entry. */
    @Column(name = "portfolio_id", length = 8)
    private String portfolioId;

    /** AUD-ACCOUNT-NO — PIC X(10). Account referenced by this audit entry. */
    @Column(name = "account_no", length = 10)
    private String accountNo;

    /** AUD-BEFORE-IMAGE — PIC X(100). Record state before change (JSON serialized). */
    @Column(name = "before_image", length = 500)
    private String beforeImage;

    /** AUD-AFTER-IMAGE — PIC X(100). Record state after change (JSON serialized). */
    @Column(name = "after_image", length = 500)
    private String afterImage;

    /** AUD-MESSAGE — PIC X(100). Human-readable audit message. */
    @Column(name = "message", length = 255)
    private String message;
}
