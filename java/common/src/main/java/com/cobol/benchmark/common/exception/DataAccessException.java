package com.cobol.benchmark.common.exception;

public class DataAccessException extends PortfolioException {
    
    public DataAccessException(String message) {
        super(message, "DATA_ACCESS_ERROR");
    }
    
    public DataAccessException(String message, Throwable cause) {
        super(message, "DATA_ACCESS_ERROR", cause);
    }
}
