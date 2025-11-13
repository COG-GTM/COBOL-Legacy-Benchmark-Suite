package com.portfolio.model;

public class AuditRequest {
    
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
    }
    
    public enum AuditAction {
        CREATE("CREATE  "),
        UPDATE("UPDATE  "),
        DELETE("DELETE  "),
        INQUIRE("INQUIRE "),
        LOGIN("LOGIN   "),
        LOGOUT("LOGOUT  "),
        STARTUP("STARTUP "),
        SHUTDOWN("SHUTDOWN");
        
        private final String code;
        
        AuditAction(String code) {
            this.code = code;
        }
        
        public String getCode() {
            return code;
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
    }
    
    private final String systemId;
    private final String userId;
    private final String program;
    private final String terminal;
    private final AuditType type;
    private final AuditAction action;
    private final AuditStatus status;
    private final String portfolioId;
    private final String accountNo;
    private final String beforeImage;
    private final String afterImage;
    private final String message;
    
    public AuditRequest(String systemId, String userId, String program, String terminal,
                       AuditType type, AuditAction action, AuditStatus status,
                       String portfolioId, String accountNo,
                       String beforeImage, String afterImage, String message) {
        this.systemId = systemId;
        this.userId = userId;
        this.program = program;
        this.terminal = terminal;
        this.type = type;
        this.action = action;
        this.status = status;
        this.portfolioId = portfolioId;
        this.accountNo = accountNo;
        this.beforeImage = beforeImage;
        this.afterImage = afterImage;
        this.message = message;
    }
    
    public String getSystemId() {
        return systemId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getProgram() {
        return program;
    }
    
    public String getTerminal() {
        return terminal;
    }
    
    public AuditType getType() {
        return type;
    }
    
    public AuditAction getAction() {
        return action;
    }
    
    public AuditStatus getStatus() {
        return status;
    }
    
    public String getPortfolioId() {
        return portfolioId;
    }
    
    public String getAccountNo() {
        return accountNo;
    }
    
    public String getBeforeImage() {
        return beforeImage;
    }
    
    public String getAfterImage() {
        return afterImage;
    }
    
    public String getMessage() {
        return message;
    }
}
