package com.cobol.benchmark.common.exception;

public class PortfolioException extends RuntimeException {
    
    private final String errorCode;
    
    public PortfolioException(String message) {
        super(message);
        this.errorCode = "PORTFOLIO_ERROR";
    }
    
    public PortfolioException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public PortfolioException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "PORTFOLIO_ERROR";
    }
    
    public PortfolioException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
