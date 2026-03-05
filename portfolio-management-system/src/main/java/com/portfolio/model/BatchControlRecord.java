package com.portfolio.model;

import java.time.LocalDateTime;

/**
 * Batch Control Record.
 * Migrated from COBOL BCHCON copybook.
 * Used for tracking batch process execution status.
 */
public class BatchControlRecord {

    private String jobName;
    private String processDate;
    private int sequenceNo;
    private String status;
    private int returnCode;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // Status values from BCHCON copybook
    public static final String STATUS_READY = "R";
    public static final String STATUS_ACTIVE = "A";
    public static final String STATUS_WAITING = "W";
    public static final String STATUS_DONE = "D";
    public static final String STATUS_ERROR = "E";

    // Return code thresholds from BCHCON copybook
    public static final int RC_SUCCESS = 0;
    public static final int RC_WARNING = 4;
    public static final int RC_ERROR = 8;
    public static final int RC_SEVERE = 12;
    public static final int RC_CRITICAL = 16;

    // Process types from BCHCON copybook
    public static final String TYPE_INITIAL = "INI";
    public static final String TYPE_UPDATE = "UPD";
    public static final String TYPE_REPORT = "RPT";
    public static final String TYPE_CLEANUP = "CLN";

    public BatchControlRecord() {}

    // Getters and setters
    public String getJobName() { return jobName; }
    public void setJobName(String jobName) { this.jobName = jobName; }

    public String getProcessDate() { return processDate; }
    public void setProcessDate(String processDate) { this.processDate = processDate; }

    public int getSequenceNo() { return sequenceNo; }
    public void setSequenceNo(int sequenceNo) { this.sequenceNo = sequenceNo; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getReturnCode() { return returnCode; }
    public void setReturnCode(int returnCode) { this.returnCode = returnCode; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
}
