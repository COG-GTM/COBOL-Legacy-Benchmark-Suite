package com.coggtm.migration.phase1.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "log_timestamp", nullable = false)
    private LocalDateTime logTimestamp;

    @Column(name = "system_id", length = 8, nullable = false)
    private String systemId;

    @Column(name = "user_id", length = 8, nullable = false)
    private String userId;

    @Column(name = "program", length = 8, nullable = false)
    private String program;

    @Column(name = "terminal", length = 8, nullable = false)
    private String terminal;

    @Column(name = "type", length = 4, nullable = false)
    private String type;

    @Column(name = "action", length = 8, nullable = false)
    private String action;

    @Column(name = "status", length = 4, nullable = false)
    private String status;

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

    public AuditLog() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getLogTimestamp() { return logTimestamp; }
    public void setLogTimestamp(LocalDateTime logTimestamp) { this.logTimestamp = logTimestamp; }

    public String getSystemId() { return systemId; }
    public void setSystemId(String systemId) { this.systemId = systemId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getProgram() { return program; }
    public void setProgram(String program) { this.program = program; }

    public String getTerminal() { return terminal; }
    public void setTerminal(String terminal) { this.terminal = terminal; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }

    public String getAccountNo() { return accountNo; }
    public void setAccountNo(String accountNo) { this.accountNo = accountNo; }

    public String getBeforeImage() { return beforeImage; }
    public void setBeforeImage(String beforeImage) { this.beforeImage = beforeImage; }

    public String getAfterImage() { return afterImage; }
    public void setAfterImage(String afterImage) { this.afterImage = afterImage; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
