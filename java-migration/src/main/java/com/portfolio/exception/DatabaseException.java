package com.portfolio.exception;

import com.portfolio.domain.enums.ErrorType;
import com.portfolio.domain.enums.ReturnCode;

/**
 * Database exception - maps to ERR-DATABASE / 'D' from RETHND.cpy.
 * Replaces DB2ERR.cbl SQLCODE checking.
 */
public class DatabaseException extends ApplicationException {

    public DatabaseException(String message) {
        super(message, ReturnCode.ERROR, ErrorType.DATABASE, "E005");
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
