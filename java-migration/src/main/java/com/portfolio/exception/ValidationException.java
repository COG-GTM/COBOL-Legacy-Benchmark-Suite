package com.portfolio.exception;

import com.portfolio.domain.enums.ErrorType;
import com.portfolio.domain.enums.ReturnCode;

/**
 * Validation exception - maps to ERR-VALIDATION / 'V' from RETHND.cpy.
 */
public class ValidationException extends ApplicationException {

    public ValidationException(String message) {
        super(message, ReturnCode.ERROR, ErrorType.VALIDATION, "E008");
    }

    public ValidationException(String message, String errorCode) {
        super(message, ReturnCode.ERROR, ErrorType.VALIDATION, errorCode);
    }
}
