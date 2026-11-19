package com.clbs.portfolio.exception;

/** VSAM status 22 / SQLCODE -803. */
public class DuplicateException extends PortfolioException {

    public DuplicateException(String message) {
        super(ErrorCode.DUPLICATE, message);
    }
}
