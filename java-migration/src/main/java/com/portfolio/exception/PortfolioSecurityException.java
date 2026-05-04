package com.portfolio.exception;

import com.portfolio.domain.enums.ErrorType;
import com.portfolio.domain.enums.ReturnCode;

/**
 * Security exception - maps to ERR-SECURITY / 'S' from RETHND.cpy.
 * Named PortfolioSecurityException to avoid conflict with java.lang.SecurityException.
 */
public class PortfolioSecurityException extends ApplicationException {

    public PortfolioSecurityException(String message) {
        super(message, ReturnCode.SEVERE, ErrorType.SECURITY, "E006");
    }
}
