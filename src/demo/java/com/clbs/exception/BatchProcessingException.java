package com.clbs.exception;

/**
 * Java equivalent of COBOL error handling from ERRHAND.cpy
 * 
 * COBOL Original:
 * <pre>
 *  01  ERR-RETURN-CODES.
 *      05  ERR-SUCCESS         PIC S9(4) COMP VALUE +0.
 *      05  ERR-WARNING         PIC S9(4) COMP VALUE +4.
 *      05  ERR-ERROR           PIC S9(4) COMP VALUE +8.
 *      05  ERR-SEVERE          PIC S9(4) COMP VALUE +12.
 *      05  ERR-TERMINAL        PIC S9(4) COMP VALUE +16.
 * </pre>
 * 
 * Migration Notes:
 * - COBOL return codes converted to exception with severity levels
 * - ERR-MESSAGE structure captured in exception fields
 * - PERFORM 9000-ERROR-ROUTINE converted to throw exception
 */
public class BatchProcessingException extends RuntimeException {

    public enum Severity {
        SUCCESS(0),
        WARNING(4),
        ERROR(8),
        SEVERE(12),
        TERMINAL(16);

        private final int returnCode;

        Severity(int returnCode) {
            this.returnCode = returnCode;
        }

        public int getReturnCode() {
            return returnCode;
        }
    }

    public enum Category {
        VSAM("VS"),
        VALIDATION("VL"),
        PROCESSING("PR"),
        SYSTEM("SY"),
        DATABASE("DB");

        private final String code;

        Category(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    private final String programId;
    private final Category category;
    private final Severity severity;
    private final String errorCode;
    private final String details;

    public BatchProcessingException(String message, String programId, Category category, 
                                     Severity severity, String errorCode) {
        super(message);
        this.programId = programId;
        this.category = category;
        this.severity = severity;
        this.errorCode = errorCode;
        this.details = null;
    }

    public BatchProcessingException(String message, String programId, Category category, 
                                     Severity severity, String errorCode, String details) {
        super(message);
        this.programId = programId;
        this.category = category;
        this.severity = severity;
        this.errorCode = errorCode;
        this.details = details;
    }

    public BatchProcessingException(String message, String programId, Category category, 
                                     Severity severity, String errorCode, Throwable cause) {
        super(message, cause);
        this.programId = programId;
        this.category = category;
        this.severity = severity;
        this.errorCode = errorCode;
        this.details = cause != null ? cause.getMessage() : null;
    }

    public String getProgramId() {
        return programId;
    }

    public Category getCategory() {
        return category;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getDetails() {
        return details;
    }

    public int getReturnCode() {
        return severity.getReturnCode();
    }

    @Override
    public String toString() {
        return String.format("[%s] %s-%s-%s: %s%s",
                programId,
                category.getCode(),
                severity.name(),
                errorCode,
                getMessage(),
                details != null ? " - " + details : "");
    }
}
