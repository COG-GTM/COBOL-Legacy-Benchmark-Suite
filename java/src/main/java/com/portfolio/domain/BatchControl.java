package com.portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;

/**
 * Relational model of the VSAM KSDS batch control file (BCHCTL), the file
 * HISTLD00 opens I-O for checkpointing. Record layout:
 * {@code src/copybook/batch/BCHCTL.cpy} (01 BATCH-CONTROL-RECORD).
 *
 * <p>VSAM→table convention: primary key = COBOL RECORD KEY BCT-KEY
 * (BCT-JOB-NAME, BCT-PROCESS-DATE, BCT-SEQUENCE-NO). The OCCURS 10 TIMES
 * prerequisite table is not needed by the HISTLD00 slice and is deferred.
 */
@Entity
@Table(name = "VSAM_BCHCTL")
public class BatchControl {

    @EmbeddedId
    private Key key;

    /** BCT-STATUS PIC X(1) — R/A/W/D/E. */
    @Column(name = "STATUS", length = 1, nullable = false)
    private String status;

    /** BCT-STEP-NAME PIC X(8). */
    @Column(name = "STEP_NAME", length = 8)
    private String stepName;

    /** BCT-PROGRAM-NAME PIC X(8). */
    @Column(name = "PROGRAM_NAME", length = 8)
    private String programName;

    /** BCT-START-TIME PIC X(8). */
    @Column(name = "START_TIME", length = 8)
    private String startTime;

    /** BCT-END-TIME PIC X(8). */
    @Column(name = "END_TIME", length = 8)
    private String endTime;

    /** Records read counter maintained by HISTLD00 checkpointing (2310-UPDATE-CHECKPOINT). */
    @Column(name = "RECORDS_READ", nullable = false)
    private long recordsRead;

    /** Records written counter maintained by HISTLD00 checkpointing. */
    @Column(name = "RECORDS_WRITTEN", nullable = false)
    private long recordsWritten;

    /** BCT-RETURN-CODE PIC S9(4) COMP. */
    @Column(name = "RETURN_CODE", nullable = false)
    private int returnCode;

    /** BCT-ERROR-DESC PIC X(80). */
    @Column(name = "ERROR_DESC", length = 80)
    private String errorDesc;

    /** BCT-RESTART-COUNT PIC 9(2) COMP. */
    @Column(name = "RESTART_COUNT", nullable = false)
    private int restartCount;

    /** BCT-ATTEMPT-TS PIC X(26). */
    @Column(name = "ATTEMPT_TS", length = 26)
    private String attemptTimestamp;

    /** BCT-COMPLETE-TS PIC X(26). */
    @Column(name = "COMPLETE_TS", length = 26)
    private String completeTimestamp;

    /** Composite primary key = BCT-KEY (job name + process date + sequence no). */
    @Embeddable
    public static class Key implements Serializable {

        /** BCT-JOB-NAME PIC X(8). */
        @Column(name = "JOB_NAME", length = 8, nullable = false)
        private String jobName;

        /** BCT-PROCESS-DATE PIC X(8) — YYYYMMDD. */
        @Column(name = "PROCESS_DATE", length = 8, nullable = false)
        private String processDate;

        /** BCT-SEQUENCE-NO PIC 9(4). */
        @Column(name = "SEQUENCE_NO", nullable = false)
        private int sequenceNo;

        public Key() {}

        public Key(String jobName, String processDate, int sequenceNo) {
            this.jobName = jobName;
            this.processDate = processDate;
            this.sequenceNo = sequenceNo;
        }

        public String getJobName() { return jobName; }
        public void setJobName(String jobName) { this.jobName = jobName; }
        public String getProcessDate() { return processDate; }
        public void setProcessDate(String processDate) { this.processDate = processDate; }
        public int getSequenceNo() { return sequenceNo; }
        public void setSequenceNo(int sequenceNo) { this.sequenceNo = sequenceNo; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key key)) return false;
            return sequenceNo == key.sequenceNo
                    && Objects.equals(jobName, key.jobName)
                    && Objects.equals(processDate, key.processDate);
        }

        @Override
        public int hashCode() {
            return Objects.hash(jobName, processDate, sequenceNo);
        }
    }

    public Key getKey() { return key; }
    public void setKey(Key key) { this.key = key; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getStepName() { return stepName; }
    public void setStepName(String stepName) { this.stepName = stepName; }
    public String getProgramName() { return programName; }
    public void setProgramName(String programName) { this.programName = programName; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public long getRecordsRead() { return recordsRead; }
    public void setRecordsRead(long recordsRead) { this.recordsRead = recordsRead; }
    public long getRecordsWritten() { return recordsWritten; }
    public void setRecordsWritten(long recordsWritten) { this.recordsWritten = recordsWritten; }
    public int getReturnCode() { return returnCode; }
    public void setReturnCode(int returnCode) { this.returnCode = returnCode; }
    public String getErrorDesc() { return errorDesc; }
    public void setErrorDesc(String errorDesc) { this.errorDesc = errorDesc; }
    public int getRestartCount() { return restartCount; }
    public void setRestartCount(int restartCount) { this.restartCount = restartCount; }
    public String getAttemptTimestamp() { return attemptTimestamp; }
    public void setAttemptTimestamp(String attemptTimestamp) { this.attemptTimestamp = attemptTimestamp; }
    public String getCompleteTimestamp() { return completeTimestamp; }
    public void setCompleteTimestamp(String completeTimestamp) { this.completeTimestamp = completeTimestamp; }
}
