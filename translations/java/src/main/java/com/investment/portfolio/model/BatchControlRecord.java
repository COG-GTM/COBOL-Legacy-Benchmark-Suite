package com.investment.portfolio.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Batch Control Record - Java equivalent of BCHCTL.cpy
 * Maps the COBOL BATCH-CONTROL-RECORD copybook structure.
 */
public class BatchControlRecord {

    /** Key fields */
    private String jobName;            // BCT-JOB-NAME: PIC X(8)
    private String processDate;        // BCT-PROCESS-DATE: PIC X(8)
    private int sequenceNumber;        // BCT-SEQUENCE-NO: PIC 9(4)

    /** Status and process control */
    private BatchStatus status;        // BCT-STATUS: PIC X(1)
    private String stepName;           // BCT-STEP-NAME: PIC X(8)
    private String programName;        // BCT-PROGRAM-NAME: PIC X(8)
    private String startTime;          // BCT-START-TIME: PIC X(8)
    private String endTime;            // BCT-END-TIME: PIC X(8)

    /** Dependencies */
    private int prerequisiteCount;     // BCT-PREREQ-COUNT: PIC 9(2) COMP
    private List<PrerequisiteJob> prerequisites; // BCT-PREREQ-JOBS OCCURS 10 TIMES

    /** Return information */
    private int returnCode;            // BCT-RETURN-CODE: PIC S9(4) COMP
    private String errorDescription;   // BCT-ERROR-DESC: PIC X(80)

    /** Statistics */
    private int restartCount;          // BCT-RESTART-COUNT: PIC 9(2) COMP
    private LocalDateTime attemptTimestamp;  // BCT-ATTEMPT-TS: PIC X(26)
    private LocalDateTime completeTimestamp; // BCT-COMPLETE-TS: PIC X(26)

    public enum BatchStatus {
        READY('R'),
        ACTIVE('A'),
        WAITING('W'),
        DONE('D'),
        ERROR('E');

        private final char code;

        BatchStatus(char code) {
            this.code = code;
        }

        public char getCode() {
            return code;
        }

        public static BatchStatus fromCode(char code) {
            for (BatchStatus s : values()) {
                if (s.code == code) return s;
            }
            throw new IllegalArgumentException("Invalid batch status: " + code);
        }
    }

    /**
     * Represents a prerequisite job entry.
     * Maps BCT-PREREQ-JOBS OCCURS 10 TIMES.
     */
    public static class PrerequisiteJob {
        private String jobName;        // BCT-PREREQ-NAME: PIC X(8)
        private int sequenceNumber;    // BCT-PREREQ-SEQ: PIC 9(4)
        private int requiredReturnCode; // BCT-PREREQ-RC: PIC S9(4) COMP

        public PrerequisiteJob() {}

        public PrerequisiteJob(String jobName, int sequenceNumber, int requiredReturnCode) {
            this.jobName = jobName;
            this.sequenceNumber = sequenceNumber;
            this.requiredReturnCode = requiredReturnCode;
        }

        public String getJobName() { return jobName; }
        public void setJobName(String jobName) { this.jobName = jobName; }

        public int getSequenceNumber() { return sequenceNumber; }
        public void setSequenceNumber(int sequenceNumber) { this.sequenceNumber = sequenceNumber; }

        public int getRequiredReturnCode() { return requiredReturnCode; }
        public void setRequiredReturnCode(int requiredReturnCode) { this.requiredReturnCode = requiredReturnCode; }
    }

    public BatchControlRecord() {
        this.prerequisites = new ArrayList<>();
    }

    // --- Getters and Setters ---

    public String getJobName() { return jobName; }
    public void setJobName(String jobName) { this.jobName = jobName; }

    public String getProcessDate() { return processDate; }
    public void setProcessDate(String processDate) { this.processDate = processDate; }

    public int getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(int sequenceNumber) { this.sequenceNumber = sequenceNumber; }

    public BatchStatus getStatus() { return status; }
    public void setStatus(BatchStatus status) { this.status = status; }

    public String getStepName() { return stepName; }
    public void setStepName(String stepName) { this.stepName = stepName; }

    public String getProgramName() { return programName; }
    public void setProgramName(String programName) { this.programName = programName; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public int getPrerequisiteCount() { return prerequisiteCount; }
    public void setPrerequisiteCount(int prerequisiteCount) { this.prerequisiteCount = prerequisiteCount; }

    public List<PrerequisiteJob> getPrerequisites() { return prerequisites; }
    public void setPrerequisites(List<PrerequisiteJob> prerequisites) { this.prerequisites = prerequisites; }

    public int getReturnCode() { return returnCode; }
    public void setReturnCode(int returnCode) { this.returnCode = returnCode; }

    public String getErrorDescription() { return errorDescription; }
    public void setErrorDescription(String errorDescription) { this.errorDescription = errorDescription; }

    public int getRestartCount() { return restartCount; }
    public void setRestartCount(int restartCount) { this.restartCount = restartCount; }

    public LocalDateTime getAttemptTimestamp() { return attemptTimestamp; }
    public void setAttemptTimestamp(LocalDateTime attemptTimestamp) { this.attemptTimestamp = attemptTimestamp; }

    public LocalDateTime getCompleteTimestamp() { return completeTimestamp; }
    public void setCompleteTimestamp(LocalDateTime completeTimestamp) { this.completeTimestamp = completeTimestamp; }

    public String getCompositeKey() {
        return jobName + processDate + String.format("%04d", sequenceNumber);
    }

    @Override
    public String toString() {
        return "BatchControlRecord{" +
                "jobName='" + jobName + '\'' +
                ", processDate='" + processDate + '\'' +
                ", status=" + status +
                ", returnCode=" + returnCode +
                '}';
    }
}
