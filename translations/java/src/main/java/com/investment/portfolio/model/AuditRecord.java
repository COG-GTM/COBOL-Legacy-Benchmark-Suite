package com.investment.portfolio.model;

import java.time.LocalDateTime;

/**
 * Audit Record - Java equivalent of AUDITLOG.cpy
 * Maps the COBOL AUDIT-RECORD copybook structure.
 */
public class AuditRecord {

    /** Header fields */
    private LocalDateTime timestamp;   // AUD-TIMESTAMP: PIC X(26)
    private String systemId;           // AUD-SYSTEM-ID: PIC X(8)
    private String userId;             // AUD-USER-ID: PIC X(8)
    private String program;            // AUD-PROGRAM: PIC X(8)
    private String terminal;           // AUD-TERMINAL: PIC X(8)

    /** Audit classification */
    private AuditType type;            // AUD-TYPE: PIC X(4)
    private AuditAction action;        // AUD-ACTION: PIC X(8)
    private AuditStatus status;        // AUD-STATUS: PIC X(4)

    /** Key info */
    private String portfolioId;        // AUD-PORTFOLIO-ID: PIC X(8)
    private String accountNumber;      // AUD-ACCOUNT-NO: PIC X(10)

    /** Change tracking */
    private String beforeImage;        // AUD-BEFORE-IMAGE: PIC X(100)
    private String afterImage;         // AUD-AFTER-IMAGE: PIC X(100)
    private String message;            // AUD-MESSAGE: PIC X(100)

    public enum AuditType {
        TRANSACTION("TRAN"),
        USER_ACTION("USER"),
        SYSTEM_EVENT("SYST");

        private final String code;

        AuditType(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        public static AuditType fromCode(String code) {
            for (AuditType t : values()) {
                if (t.code.equals(code)) return t;
            }
            throw new IllegalArgumentException("Invalid audit type: " + code);
        }
    }

    public enum AuditAction {
        CREATE("CREATE"),
        UPDATE("UPDATE"),
        DELETE("DELETE"),
        INQUIRE("INQUIRE"),
        LOGIN("LOGIN"),
        LOGOUT("LOGOUT"),
        STARTUP("STARTUP"),
        SHUTDOWN("SHUTDOWN");

        private final String code;

        AuditAction(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        public static AuditAction fromCode(String code) {
            if (code == null) throw new IllegalArgumentException("Null audit action");
            String trimmed = code.trim();
            for (AuditAction a : values()) {
                if (a.code.equals(trimmed)) return a;
            }
            throw new IllegalArgumentException("Invalid audit action: " + code);
        }
    }

    public enum AuditStatus {
        SUCCESS("SUCC"),
        FAILURE("FAIL"),
        WARNING("WARN");

        private final String code;

        AuditStatus(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        public static AuditStatus fromCode(String code) {
            for (AuditStatus s : values()) {
                if (s.code.equals(code)) return s;
            }
            throw new IllegalArgumentException("Invalid audit status: " + code);
        }
    }

    // --- Getters and Setters ---

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getSystemId() { return systemId; }
    public void setSystemId(String systemId) { this.systemId = systemId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getProgram() { return program; }
    public void setProgram(String program) { this.program = program; }

    public String getTerminal() { return terminal; }
    public void setTerminal(String terminal) { this.terminal = terminal; }

    public AuditType getType() { return type; }
    public void setType(AuditType type) { this.type = type; }

    public AuditAction getAction() { return action; }
    public void setAction(AuditAction action) { this.action = action; }

    public AuditStatus getStatus() { return status; }
    public void setStatus(AuditStatus status) { this.status = status; }

    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getBeforeImage() { return beforeImage; }
    public void setBeforeImage(String beforeImage) { this.beforeImage = beforeImage; }

    public String getAfterImage() { return afterImage; }
    public void setAfterImage(String afterImage) { this.afterImage = afterImage; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    @Override
    public String toString() {
        return "AuditRecord{" +
                "timestamp=" + timestamp +
                ", program='" + program + '\'' +
                ", type=" + type +
                ", action=" + action +
                ", status=" + status +
                '}';
    }
}
