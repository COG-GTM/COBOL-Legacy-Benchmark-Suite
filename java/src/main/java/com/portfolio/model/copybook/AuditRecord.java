package com.portfolio.model.copybook;

/**
 * Migrated from copybook {@code src/copybook/common/AUDITLOG.cpy} (01 AUDIT-RECORD).
 */
public class AuditRecord {

    /** AUD-TIMESTAMP PIC X(26). */
    private String timestamp;

    /** AUD-SYSTEM-ID PIC X(8). */
    private String systemId;

    /** AUD-USER-ID PIC X(8). */
    private String userId;

    /** AUD-PROGRAM PIC X(8). */
    private String program;

    /** AUD-TERMINAL PIC X(8). */
    private String terminal;

    /** AUD-TYPE PIC X(4) — TRAN/USER/SYST (level-88s). */
    private String type;

    /** AUD-ACTION PIC X(8) — CREATE/UPDATE/DELETE/INQUIRE/LOGIN/LOGOUT/STARTUP/SHUTDOWN (level-88s). */
    private String action;

    /** AUD-STATUS PIC X(4) — SUCC/FAIL/WARN (level-88s). */
    private String status;

    /** AUD-PORTFOLIO-ID PIC X(8). */
    private String portfolioId;

    /** AUD-ACCOUNT-NO PIC X(10). */
    private String accountNo;

    /** AUD-BEFORE-IMAGE PIC X(100). */
    private String beforeImage;

    /** AUD-AFTER-IMAGE PIC X(100). */
    private String afterImage;

    /** AUD-MESSAGE PIC X(100). */
    private String message;

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
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
