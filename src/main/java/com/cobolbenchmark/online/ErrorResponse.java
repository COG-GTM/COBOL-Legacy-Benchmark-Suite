package com.cobolbenchmark.online;

/**
 * Error Response DTO - replaces ERRMAP BMS map output.
 * Maps to EXEC CICS SEND MAP('ERRMAP') output fields.
 */
public class ErrorResponse {

    private String errorCode;
    private String errorMessage;
    private String programId;
    private String timestamp;

    public ErrorResponse() {
    }

    public ErrorResponse(String errorCode, String errorMessage, String programId) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.programId = programId;
        this.timestamp = java.time.Instant.now().toString();
    }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getProgramId() { return programId; }
    public void setProgramId(String programId) { this.programId = programId; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
