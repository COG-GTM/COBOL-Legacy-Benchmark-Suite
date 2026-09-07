package com.clbs.domain;

/** AUDITLOG.cpy AUDIT-RECORD. */
public class AuditRecord {

    /** AUD-TYPE 88-levels. */
    public static final String TYPE_TRANSACTION = "TRAN";
    public static final String TYPE_USER_ACTION = "USER";
    public static final String TYPE_SYSTEM_EVENT = "SYST";

    /** AUD-ACTION 88-levels, PIC X(8). */
    public static final String ACTION_CREATE = "CREATE  ";
    public static final String ACTION_UPDATE = "UPDATE  ";
    public static final String ACTION_DELETE = "DELETE  ";
    public static final String ACTION_INQUIRE = "INQUIRE ";
    public static final String ACTION_LOGIN = "LOGIN   ";
    public static final String ACTION_LOGOUT = "LOGOUT  ";
    public static final String ACTION_STARTUP = "STARTUP ";
    public static final String ACTION_SHUTDOWN = "SHUTDOWN";

    /** AUD-STATUS 88-levels. */
    public static final String STATUS_SUCCESS = "SUCC";
    public static final String STATUS_FAILURE = "FAIL";
    public static final String STATUS_WARNING = "WARN";

    private String timestamp = "";
    private String systemId = "CLBS";
    private String userId = "";
    private String programId = "";
    private String terminalId = "";
    private String type = "TRAN";
    private String action = "";
    private String status = STATUS_SUCCESS;
    private String portfolioId = "";
    private String accountNo = "";
    private String beforeImage = "";
    private String afterImage = "";
    private String message = "";

    public AuditRecord copy() {
        AuditRecord c = new AuditRecord();
        c.timestamp = timestamp;
        c.systemId = systemId;
        c.userId = userId;
        c.programId = programId;
        c.terminalId = terminalId;
        c.type = type;
        c.action = action;
        c.status = status;
        c.portfolioId = portfolioId;
        c.accountNo = accountNo;
        c.beforeImage = beforeImage;
        c.afterImage = afterImage;
        c.message = message;
        return c;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getSystemId() {
        return systemId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getProgramId() {
        return programId;
    }

    public void setProgramId(String programId) {
        this.programId = programId;
    }

    public String getTerminalId() {
        return terminalId;
    }

    public void setTerminalId(String terminalId) {
        this.terminalId = terminalId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(String portfolioId) {
        this.portfolioId = portfolioId;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public String getBeforeImage() {
        return beforeImage;
    }

    public void setBeforeImage(String beforeImage) {
        this.beforeImage = beforeImage;
    }

    public String getAfterImage() {
        return afterImage;
    }

    public void setAfterImage(String afterImage) {
        this.afterImage = afterImage;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
