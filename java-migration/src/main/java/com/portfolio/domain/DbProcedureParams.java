package com.portfolio.domain;

/**
 * DB2 procedure parameters - migrated from COBOL DBPROC.cpy.
 * In Spring, connection management is handled by HikariCP.
 * This DTO preserves retry configuration from the COBOL source.
 */
public class DbProcedureParams {

    private String errorMessage;
    private String saveStatus;
    private int retryCount;
    private int maxRetries;
    private int retryWaitMs;

    public DbProcedureParams() {
        this.retryCount = 0;
        this.maxRetries = 3;
        this.retryWaitMs = 100;
    }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getSaveStatus() { return saveStatus; }
    public void setSaveStatus(String saveStatus) { this.saveStatus = saveStatus; }
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    public int getRetryWaitMs() { return retryWaitMs; }
    public void setRetryWaitMs(int retryWaitMs) { this.retryWaitMs = retryWaitMs; }

    public boolean canRetry() {
        return retryCount < maxRetries;
    }
}
