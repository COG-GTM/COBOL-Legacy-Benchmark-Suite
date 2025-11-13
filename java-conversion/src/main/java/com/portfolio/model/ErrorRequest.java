package com.portfolio.model;

import java.time.LocalDateTime;

public class ErrorRequest {
    
    private final String programId;
    private final String category;
    private final String errorCode;
    private final int severity;
    private final String errorText;
    private final String errorDetails;
    
    public ErrorRequest(String programId, String category, String errorCode, 
                       int severity, String errorText, String errorDetails) {
        this.programId = programId;
        this.category = category;
        this.errorCode = errorCode;
        this.severity = severity;
        this.errorText = errorText;
        this.errorDetails = errorDetails;
    }
    
    public String getProgramId() {
        return programId;
    }
    
    public String getCategory() {
        return category;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public int getSeverity() {
        return severity;
    }
    
    public String getErrorText() {
        return errorText;
    }
    
    public String getErrorDetails() {
        return errorDetails;
    }
}
