package com.cobol.benchmark.common.exception;

public class BusinessException extends PortfolioException {
    
    public BusinessException(String message) {
        super(message, "BUSINESS_ERROR");
    }
    
    public BusinessException(String message, String errorCode) {
        super(message, errorCode);
    }
    
    public BusinessException(String message, Throwable cause) {
        super(message, "BUSINESS_ERROR", cause);
    }
}
