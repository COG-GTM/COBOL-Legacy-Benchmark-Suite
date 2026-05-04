package com.portfolio.exception;

import com.portfolio.domain.enums.ErrorType;
import com.portfolio.domain.enums.ReturnCode;

/**
 * Processing exception - maps to ERR-PROCESSING / 'P' from RETHND.cpy.
 */
public class ProcessingException extends ApplicationException {

    public ProcessingException(String message) {
        super(message, ReturnCode.ERROR, ErrorType.PROCESSING, "E007");
    }

    public ProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
