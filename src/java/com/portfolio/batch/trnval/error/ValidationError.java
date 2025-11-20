package com.portfolio.batch.trnval.error;

/**
 * Validation Error - Represents validation errors found during transaction processing
 * 
 * Corresponds to COBOL error handling patterns from ERRHAND copybook
 * Error codes based on data-dictionary.md:
 * - E001: Invalid Account Number
 * - E002: Invalid Fund ID
 * - E003: Invalid Transaction Type
 * - E004: Insufficient Position Balance
 * - W001: Zero Dollar Transaction
 * - W002: Duplicate Transaction ID
 */
public class ValidationError {
    
    private final ErrorSeverity severity;
    private final String errorCode;
    private final String errorMessage;
    private final String fieldName;
    private final String fieldValue;
    private final int lineNumber;
    private final String transactionKey;
    
    public enum ErrorSeverity {
        ERROR("E", 8),
        WARNING("W", 4),
        INFO("I", 0);
        
        private final String code;
        private final int returnCode;
        
        ErrorSeverity(String code, int returnCode) {
            this.code = code;
            this.returnCode = returnCode;
        }
        
        public String getCode() {
            return code;
        }
        
        public int getReturnCode() {
            return returnCode;
        }
    }
    
    private ValidationError(Builder builder) {
        this.severity = builder.severity;
        this.errorCode = builder.errorCode;
        this.errorMessage = builder.errorMessage;
        this.fieldName = builder.fieldName;
        this.fieldValue = builder.fieldValue;
        this.lineNumber = builder.lineNumber;
        this.transactionKey = builder.transactionKey;
    }
    
    public ErrorSeverity getSeverity() {
        return severity;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public String getFieldName() {
        return fieldName;
    }
    
    public String getFieldValue() {
        return fieldValue;
    }
    
    public int getLineNumber() {
        return lineNumber;
    }
    
    public String getTransactionKey() {
        return transactionKey;
    }
    
    public boolean isError() {
        return severity == ErrorSeverity.ERROR;
    }
    
    public boolean isWarning() {
        return severity == ErrorSeverity.WARNING;
    }
    
    public String formatErrorLine() {
        return String.format("%-8s %-6s Line %06d: %-20s %-15s %s",
                severity.getCode() + errorCode,
                severity.name(),
                lineNumber,
                fieldName != null ? fieldName : "",
                fieldValue != null ? fieldValue : "",
                errorMessage);
    }
    
    @Override
    public String toString() {
        return formatErrorLine();
    }
    
    public static class Builder {
        private ErrorSeverity severity;
        private String errorCode;
        private String errorMessage;
        private String fieldName;
        private String fieldValue;
        private int lineNumber;
        private String transactionKey;
        
        public Builder severity(ErrorSeverity severity) {
            this.severity = severity;
            return this;
        }
        
        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }
        
        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }
        
        public Builder fieldName(String fieldName) {
            this.fieldName = fieldName;
            return this;
        }
        
        public Builder fieldValue(String fieldValue) {
            this.fieldValue = fieldValue;
            return this;
        }
        
        public Builder lineNumber(int lineNumber) {
            this.lineNumber = lineNumber;
            return this;
        }
        
        public Builder transactionKey(String transactionKey) {
            this.transactionKey = transactionKey;
            return this;
        }
        
        public ValidationError build() {
            return new ValidationError(this);
        }
    }
}
