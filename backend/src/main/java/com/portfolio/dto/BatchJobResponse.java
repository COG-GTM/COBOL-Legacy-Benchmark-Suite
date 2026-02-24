package com.portfolio.dto;

import java.time.LocalDateTime;

public class BatchJobResponse {

    private Long jobId;
    private String jobName;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private long recordsProcessed;
    private long errorCount;
    private int returnCode;
    private String message;

    public BatchJobResponse() {
    }

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public String getJobName() { return jobName; }
    public void setJobName(String jobName) { this.jobName = jobName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public long getRecordsProcessed() { return recordsProcessed; }
    public void setRecordsProcessed(long recordsProcessed) { this.recordsProcessed = recordsProcessed; }
    public long getErrorCount() { return errorCount; }
    public void setErrorCount(long errorCount) { this.errorCount = errorCount; }
    public int getReturnCode() { return returnCode; }
    public void setReturnCode(int returnCode) { this.returnCode = returnCode; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
