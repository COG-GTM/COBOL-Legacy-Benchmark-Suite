package com.portfolio.exception;

/**
 * Base exception for portfolio system
 * Migrated from COBOL ERRHAND.cpy error handling
 */
public class PortfolioException extends RuntimeException {

    private final ErrorCode errorCode;
    private final int returnCode;

    public PortfolioException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.returnCode = errorCode.getReturnCode();
    }

    public PortfolioException(String message, ErrorCode errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.returnCode = errorCode.getReturnCode();
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public int getReturnCode() {
        return returnCode;
    }

    public enum ErrorCode {
        SUCCESS(0, "VS"),
        WARNING(4, "VL"),
        ERROR(8, "PR"),
        SEVERE(12, "SY"),
        TERMINAL(16, "SY"),
        
        VALIDATION_ERROR(8, "VL"),
        NOT_FOUND(8, "VS"),
        DUPLICATE_KEY(8, "VS"),
        DATABASE_ERROR(12, "SY"),
        SECURITY_ERROR(12, "SY"),
        BATCH_ERROR(8, "PR");

        private final int returnCode;
        private final String category;

        ErrorCode(int returnCode, String category) {
            this.returnCode = returnCode;
            this.category = category;
        }

        public int getReturnCode() {
            return returnCode;
        }

        public String getCategory() {
            return category;
        }
    }
}
