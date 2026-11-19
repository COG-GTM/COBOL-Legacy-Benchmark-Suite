package com.clbs.portfolio.exception;

/** VSAM status 23 / CICS NOTFND. */
public class NotFoundException extends PortfolioException {

    public NotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }
}
