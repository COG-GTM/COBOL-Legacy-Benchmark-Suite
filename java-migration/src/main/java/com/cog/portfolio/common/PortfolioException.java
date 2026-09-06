package com.cog.portfolio.common;

/** Typed exception carrying a COBOL-style return code (replaces RTNCDE00 / ERRPROC flows). */
public class PortfolioException extends RuntimeException {

    private final ReturnCode returnCode;
    private final ErrorCode errorCode;

    public PortfolioException(ReturnCode returnCode, ErrorCode errorCode, String message) {
        super(message);
        this.returnCode = returnCode;
        this.errorCode = errorCode;
    }

    public static PortfolioException notFound(String message) {
        return new PortfolioException(ReturnCode.ERROR, ErrorCode.NOT_FOUND, message);
    }

    public static PortfolioException duplicate(String message) {
        return new PortfolioException(ReturnCode.ERROR, ErrorCode.DUPLICATE, message);
    }

    public static PortfolioException validation(String message) {
        return new PortfolioException(ReturnCode.ERROR, ErrorCode.VALIDATION, message);
    }

    public static PortfolioException processing(String message) {
        return new PortfolioException(ReturnCode.ERROR, ErrorCode.PROCESSING, message);
    }

    public ReturnCode getReturnCode() {
        return returnCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
