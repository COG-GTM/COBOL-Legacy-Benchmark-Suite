package com.investment.portfolio.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Standard Error Handler - Java equivalent of ERRHAND.cpy and ERRPROC.cbl
 *
 * Provides centralized error handling following the COBOL ERRPROC pattern:
 * categorized errors, severity levels, and structured error messages.
 */
public class ErrorHandler {

    private static final Logger LOGGER = Logger.getLogger(ErrorHandler.class.getName());
    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

    /** Error categories matching ERR-CATEGORIES from ERRHAND.cpy */
    public enum ErrorCategory {
        VSAM("VS"),         // File/data store errors
        VALIDATION("VL"),   // Data validation errors
        PROCESSING("PR"),   // Business logic errors
        SYSTEM("SY");       // System/environment errors

        private final String code;

        ErrorCategory(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    /**
     * Error message structure matching ERR-MESSAGE from ERRHAND.cpy
     */
    public static class ErrorMessage {
        private final LocalDateTime timestamp;
        private final String program;
        private final ErrorCategory category;
        private final String errorCode;
        private final int severity;
        private final String text;
        private final String details;

        public ErrorMessage(String program, ErrorCategory category, String errorCode,
                            int severity, String text, String details) {
            this.timestamp = LocalDateTime.now();
            this.program = program;
            this.category = category;
            this.errorCode = errorCode;
            this.severity = severity;
            this.text = text;
            this.details = details;
        }

        public LocalDateTime getTimestamp() { return timestamp; }
        public String getProgram() { return program; }
        public ErrorCategory getCategory() { return category; }
        public String getErrorCode() { return errorCode; }
        public int getSeverity() { return severity; }
        public String getText() { return text; }
        public String getDetails() { return details; }

        @Override
        public String toString() {
            return String.format("[%s] %s %s-%s (Sev %d): %s",
                    timestamp.format(TIMESTAMP_FMT),
                    program, category.getCode(), errorCode,
                    severity, text);
        }
    }

    private final String programId;

    public ErrorHandler(String programId) {
        this.programId = programId;
    }

    /**
     * Handles a file/VSAM error.
     * Maps to COBOL's VSAM status handling in ERRHAND.cpy.
     */
    public void handleFileError(String fileStatus, String fileName) {
        String message;
        switch (fileStatus) {
            case "22": message = "Duplicate record key"; break;
            case "23": message = "Record not found"; break;
            case "10": message = "End of file reached"; break;
            default:   message = "Unexpected file error: status=" + fileStatus; break;
        }
        ErrorMessage err = new ErrorMessage(programId, ErrorCategory.VSAM,
                "F" + fileStatus, ReturnCode.ERROR, message, "File: " + fileName);
        logError(err);
    }

    /**
     * Handles a validation error.
     */
    public void handleValidationError(String errorCode, String message, String details) {
        ErrorMessage err = new ErrorMessage(programId, ErrorCategory.VALIDATION,
                errorCode, ReturnCode.ERROR, message, details);
        logError(err);
    }

    /**
     * Handles a processing error.
     */
    public void handleProcessingError(String errorCode, String message, String details) {
        ErrorMessage err = new ErrorMessage(programId, ErrorCategory.PROCESSING,
                errorCode, ReturnCode.ERROR, message, details);
        logError(err);
    }

    /**
     * Handles a system-level error.
     */
    public void handleSystemError(String errorCode, String message, Exception cause) {
        ErrorMessage err = new ErrorMessage(programId, ErrorCategory.SYSTEM,
                errorCode, ReturnCode.SEVERE, message,
                cause != null ? cause.getMessage() : "");
        logError(err);
        if (cause != null) {
            LOGGER.log(Level.SEVERE, err.toString(), cause);
        }
    }

    /**
     * Handles a SQL/database error.
     * Maps to the DB2 error handling in DBPROC.cpy.
     */
    public void handleDatabaseError(int sqlCode, String sqlState, String operation) {
        String message = String.format("SQLCODE: %d STATE: %s ERROR: %s",
                sqlCode, sqlState, operation);
        ErrorMessage err = new ErrorMessage(programId, ErrorCategory.SYSTEM,
                "D" + Math.abs(sqlCode), ReturnCode.SEVERE, message, "");
        logError(err);
    }

    private void logError(ErrorMessage err) {
        Level level;
        if (err.getSeverity() >= ReturnCode.SEVERE) {
            level = Level.SEVERE;
        } else if (err.getSeverity() >= ReturnCode.ERROR) {
            level = Level.WARNING;
        } else {
            level = Level.INFO;
        }
        LOGGER.log(level, err.toString());
    }
}
