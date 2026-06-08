package com.portfolio.infrastructure.audit;

import com.portfolio.domain.model.AuditAction;
import com.portfolio.domain.model.AuditType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA entity -- maps COBOL AUDITLOG.cpy AUDIT-RECORD.
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

    private AuditRecord(Builder builder) {
        this.timestamp = builder.timestamp;
        this.systemId = builder.systemId;
        this.userId = builder.userId;
        this.program = builder.program;
        this.terminal = builder.terminal;
        this.auditType = builder.auditType;
        this.action = builder.action;
        this.status = builder.status;
        this.portfolioId = builder.portfolioId;
        this.accountNumber = builder.accountNumber;
        this.beforeImage = builder.beforeImage;
        this.afterImage = builder.afterImage;
        this.message = builder.message;
    }

    public Long getId() { return id; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getSystemId() { return systemId; }
    public String getUserId() { return userId; }
    public String getProgram() { return program; }
    public String getTerminal() { return terminal; }
    public AuditType getAuditType() { return auditType; }
    public AuditAction getAction() { return action; }
    public String getStatus() { return status; }
    public String getPortfolioId() { return portfolioId; }
    public String getAccountNumber() { return accountNumber; }
    public String getBeforeImage() { return beforeImage; }
    public String getAfterImage() { return afterImage; }
    public String getMessage() { return message; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private LocalDateTime timestamp;
        private String systemId;
        private String userId;
        private String program;
        private String terminal;
        private AuditType auditType;
        private AuditAction action;
        private String status;
        private String portfolioId;
        private String accountNumber;
        private String beforeImage;
        private String afterImage;
        private String message;

        public Builder timestamp(LocalDateTime timestamp) { this.timestamp = timestamp; return this; }
        public Builder systemId(String systemId) { this.systemId = systemId; return this; }
        public Builder userId(String userId) { this.userId = userId; return this; }
        public Builder program(String program) { this.program = program; return this; }
        public Builder terminal(String terminal) { this.terminal = terminal; return this; }
        public Builder auditType(AuditType auditType) { this.auditType = auditType; return this; }
        public Builder action(AuditAction action) { this.action = action; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder portfolioId(String portfolioId) { this.portfolioId = portfolioId; return this; }
        public Builder accountNumber(String accountNumber) { this.accountNumber = accountNumber; return this; }
        public Builder beforeImage(String beforeImage) { this.beforeImage = beforeImage; return this; }
        public Builder afterImage(String afterImage) { this.afterImage = afterImage; return this; }
        public Builder message(String message) { this.message = message; return this; }

        public AuditRecord build() {
            return new AuditRecord(this);
        }
    }
}
