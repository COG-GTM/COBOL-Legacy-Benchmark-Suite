package com.portfolio.exception;

import com.portfolio.domain.enums.ErrorType;
import com.portfolio.domain.enums.ReturnCode;

/**
 * Base application exception - replaces COBOL RETHND.cpy return-code pattern.
 */
public class ApplicationException extends RuntimeException {

    private final ReturnCode returnCode;
    private final ErrorType errorType;
    private final String errorCode;

    public ApplicationException(String message) {
        super(message);
        this.returnCode = ReturnCode.ERROR;
        this.errorType = ErrorType.PROCESSING;
        this.errorCode = "E007";
    }

    public ApplicationException(String message, ReturnCode returnCode, ErrorType errorType, String errorCode) {
        super(message);
        this.returnCode = returnCode;
        this.errorType = errorType;
        this.errorCode = errorCode;
    }

    public ApplicationException(String message, Throwable cause) {
        super(message, cause);
        this.returnCode = ReturnCode.ERROR;
        this.errorType = ErrorType.PROCESSING;
        this.errorCode = "E007";
    }

    public ReturnCode getReturnCode() { return returnCode; }
    public ErrorType getErrorType() { return errorType; }
    public String getErrorCode() { return errorCode; }
}
