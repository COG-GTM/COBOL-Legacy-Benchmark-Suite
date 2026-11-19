package com.clbs.portfolio.exception;

/** Base class for business errors raised by the service layer. */
public abstract class PortfolioException extends RuntimeException {

    private final ErrorCode errorCode;

    protected PortfolioException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
