package com.clbs.portfolio.exception;

/** Processing rejected after validation (e.g. insufficient units for sale). */
public class BusinessRuleException extends PortfolioException {

    public BusinessRuleException(String message) {
        super(ErrorCode.PROCESSING, message);
    }
}
