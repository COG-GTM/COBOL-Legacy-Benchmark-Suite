package com.clbs.portfolio.exception;

/** Record-level validation failure (PORTADD, PORTTRAN 2100-VALIDATE-TRANSACTION). */
public class InvalidRequestException extends PortfolioException {

    public InvalidRequestException(String message) {
        super(ErrorCode.VALIDATION, message);
    }
}
