package com.cobolbenchmark.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * Batch Control Record - migrated from BCHCTL.cpy.
 * Composite key: BCT-JOB-NAME + BCT-PROCESS-DATE + BCT-SEQUENCE-NO.
 */
@Entity
@Table(name = "BATCH_CONTROL")
@IdClass(BatchControlKey.class)
public class BatchControlRecord {

    @Id
    @Column(name = "JOB_NAME", length = 8, nullable = false)
    private String jobName;

    @Id
    @Column(name = "PROCESS_DATE", length = 8, nullable = false)
    private String processDate;

    @Id
    @Column(name = "SEQUENCE_NO", nullable = false)
    private int sequenceNo;

    @Column(name = "STATUS", length = 1, nullable = false)
    private String status;

    @Column(name = "RETURN_CODE")
    private int returnCode;

    @Column(name = "RESTART_COUNT")
    private int restartCount;

    @Column(name = "MAX_RESTARTS")
    private int maxRestarts = 3;

    @Column(name = "START_TIME", length = 26)
    private String startTime;

    @Column(name = "END_TIME", length = 26)
    private String endTime;

    @Column(name = "ATTEMPT_TS", length = 26)
    private String attemptTs;

    @Column(name = "RECORDS_READ")
    private int recordsRead;

    @Column(name = "RECORDS_WRITTEN")
    private int recordsWritten;

    @Column(name = "ERROR_COUNT")
    private int errorCount;

    @Column(name = "ERROR_DESC", length = 80)
    private String errorDesc;

    public BatchControlRecord() {
    }

    public BatchControlRecord(BatchControlRecord other) {
        if (other != null) {
            copyFrom(other);
        }
    }

    public void copyFrom(BatchControlRecord other) {
        this.jobName = other.jobName;
        this.processDate = other.processDate;
        this.sequenceNo = other.sequenceNo;
        this.status = other.status;
        this.returnCode = other.returnCode;
        this.restartCount = other.restartCount;
        this.maxRestarts = other.maxRestarts;
        this.startTime = other.startTime;
        this.endTime = other.endTime;
        this.attemptTs = other.attemptTs;
        this.recordsRead = other.recordsRead;
        this.recordsWritten = other.recordsWritten;
        this.errorCount = other.errorCount;
        this.errorDesc = other.errorDesc;
    }

    // Getters and Setters

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

    public int getRestartCount() { return restartCount; }
    public void setRestartCount(int restartCount) { this.restartCount = restartCount; }

    public int getMaxRestarts() { return maxRestarts; }
    public void setMaxRestarts(int maxRestarts) { this.maxRestarts = maxRestarts; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getAttemptTs() { return attemptTs; }
    public void setAttemptTs(String attemptTs) { this.attemptTs = attemptTs; }

    public int getRecordsRead() { return recordsRead; }
    public void setRecordsRead(int recordsRead) { this.recordsRead = recordsRead; }

    public int getRecordsWritten() { return recordsWritten; }
    public void setRecordsWritten(int recordsWritten) { this.recordsWritten = recordsWritten; }

    public int getErrorCount() { return errorCount; }
    public void setErrorCount(int errorCount) { this.errorCount = errorCount; }

    public String getErrorDesc() { return errorDesc; }
    public void setErrorDesc(String errorDesc) { this.errorDesc = errorDesc; }
}
