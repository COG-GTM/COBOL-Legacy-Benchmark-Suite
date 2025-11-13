package com.portfolio.model;

public class ValidationResult {
    
    private final int returnCode;
    private final String errorMessage;
    
    public ValidationResult(int returnCode, String errorMessage) {
        this.returnCode = returnCode;
        this.errorMessage = errorMessage;
    }
    
    public static ValidationResult success() {
        return new ValidationResult(ValidationConstants.ReturnCodes.SUCCESS, "");
    }
    
    public static ValidationResult error(int returnCode, String errorMessage) {
        return new ValidationResult(returnCode, errorMessage);
    }
    
    public int getReturnCode() {
        return returnCode;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public boolean isSuccess() {
        return returnCode == ValidationConstants.ReturnCodes.SUCCESS;
    }
}
