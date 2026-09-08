package com.clbs.batch;

import java.util.ArrayList;
import java.util.List;

/** BCHCTL.cpy BATCH-CONTROL-RECORD. */
public class BatchControlRecord {

    /** BCHCON.cpy BCT-STAT-* process statuses. */
    public static final char READY = 'R';
    public static final char ACTIVE = 'A';
    public static final char WAITING = 'W';
    public static final char DONE = 'D';
    public static final char ERROR = 'E';

    private String jobName = "";
    private String processDate = "";
    private String sequenceNo = "";
    private char status = READY;
    private String stepName = "";
    private String programName = "";
    private String startTime = "";
    private String endTime = "";
    private int returnCode;
    private String errorDesc = "";
    private int restartCount;
    private int maxRestarts = 3;
    private String attemptTimestamp = "";
    private String completionTimestamp = "";
    /**
     * HISTLD00 checkpoints into BCT-RECORDS-READ / BCT-RECORDS-WRITTEN, which BCHCTL.cpy does not
     * define; the counters are carried here so the checkpoint step is representable.
     */
    private long recordsRead;
    private long recordsWritten;
    private final List<Prerequisite> prerequisites = new ArrayList<>();

    /** BCT-PREREQ-JOBS occurrence (max BCT-MAX-PREREQ = 10). */
    public record Prerequisite(String jobName, String sequenceNo, int maxReturnCode) {
    }

    public List<Prerequisite> getPrerequisites() {
        return prerequisites;
    }

    /** BCT-KEY = BCT-JOB-NAME + BCT-PROCESS-DATE + BCT-SEQUENCE-NO. */
    public String key() {
        return pad(jobName, 8) + pad(processDate, 8) + pad(sequenceNo, 4);
    }

    private static String pad(String value, int length) {
        String v = value == null ? "" : value;
        return v.length() >= length ? v.substring(0, length) : v + " ".repeat(length - v.length());
    }

    public boolean isDone() {
        return status == DONE;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getProcessDate() {
        return processDate;
    }

    public void setProcessDate(String processDate) {
        this.processDate = processDate;
    }

    public String getSequenceNo() {
        return sequenceNo;
    }

    public void setSequenceNo(String sequenceNo) {
        this.sequenceNo = sequenceNo;
    }

    public char getStatus() {
        return status;
    }

    public void setStatus(char status) {
        this.status = status;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public String getProgramName() {
        return programName;
    }

    public void setProgramName(String programName) {
        this.programName = programName;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public int getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }

    public String getErrorDesc() {
        return errorDesc;
    }

    public void setErrorDesc(String errorDesc) {
        this.errorDesc = errorDesc;
    }

    public int getRestartCount() {
        return restartCount;
    }

    public void setRestartCount(int restartCount) {
        this.restartCount = restartCount;
    }

    public int getMaxRestarts() {
        return maxRestarts;
    }

    public void setMaxRestarts(int maxRestarts) {
        this.maxRestarts = maxRestarts;
    }

    public String getAttemptTimestamp() {
        return attemptTimestamp;
    }

    public void setAttemptTimestamp(String attemptTimestamp) {
        this.attemptTimestamp = attemptTimestamp;
    }

    public String getCompletionTimestamp() {
        return completionTimestamp;
    }

    public void setCompletionTimestamp(String completionTimestamp) {
        this.completionTimestamp = completionTimestamp;
    }

    public long getRecordsRead() {
        return recordsRead;
    }

    public void setRecordsRead(long recordsRead) {
        this.recordsRead = recordsRead;
    }

    public long getRecordsWritten() {
        return recordsWritten;
    }

    public void setRecordsWritten(long recordsWritten) {
        this.recordsWritten = recordsWritten;
    }
}
