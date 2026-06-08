package com.portfolio.infrastructure.audit;

import com.portfolio.domain.model.AuditAction;
import com.portfolio.domain.model.AuditType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA entity — maps COBOL AUDITLOG.cpy AUDIT-RECORD.
 * To be fully implemented by Child Session 4.
 */
@Entity
@Table(name = "audit_record")
public class AuditRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "timestamp")
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
    @Column(name = "audit_type", length = 12)
    private AuditType auditType;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", length = 10)
    private AuditAction action;

    @Column(name = "status", length = 4)
    private String status;

    @Column(name = "portfolio_id", length = 8)
    private String portfolioId;

    @Column(name = "account_no", length = 10)
    private String accountNumber;

    @Column(name = "before_image", length = 100)
    private String beforeImage;

    @Column(name = "after_image", length = 100)
    private String afterImage;

    @Column(name = "message", length = 100)
    private String message;

    protected AuditRecord() { /* JPA */ }

    public Long getId() { return id; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public AuditType getAuditType() { return auditType; }
    public AuditAction getAction() { return action; }
    public String getPortfolioId() { return portfolioId; }
}
