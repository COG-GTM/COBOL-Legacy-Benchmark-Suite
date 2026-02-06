package com.clbs.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Java equivalent of COBOL BATCH-CONTROL-RECORD from BCHCTL.cpy
 * 
 * COBOL Original:
 * <pre>
 *  01  BATCH-CONTROL-RECORD.
 *      05  BCT-KEY.
 *          10  BCT-JOB-NAME      PIC X(8).
 *          10  BCT-PROCESS-DATE  PIC X(8).
 *          10  BCT-SEQUENCE-NO   PIC 9(4).
 *      05  BCT-DATA.
 *          10  BCT-STATUS        PIC X(1).
 *              88  BCT-STATUS-READY    VALUE 'R'.
 *              88  BCT-STATUS-ACTIVE   VALUE 'A'.
 *              88  BCT-STATUS-WAITING  VALUE 'W'.
 *              88  BCT-STATUS-DONE     VALUE 'D'.
 *              88  BCT-STATUS-ERROR    VALUE 'E'.
 *          10  BCT-PROCESS-CONTROL.
 *              15  BCT-STEP-NAME    PIC X(8).
 *              15  BCT-PROGRAM-NAME PIC X(8).
 *              15  BCT-START-TIME   PIC X(8).
 *              15  BCT-END-TIME     PIC X(8).
 *          10  BCT-DEPENDENCIES.
 *              15  BCT-PREREQ-COUNT PIC 9(2) COMP.
 *              15  BCT-PREREQ-JOBS  OCCURS 10 TIMES.
 *                  20  BCT-PREREQ-NAME  PIC X(8).
 *                  20  BCT-PREREQ-SEQ   PIC 9(4).
 *                  20  BCT-PREREQ-RC    PIC S9(4) COMP.
 *          10  BCT-RETURN-INFO.
 *              15  BCT-RETURN-CODE  PIC S9(4) COMP.
 *              15  BCT-ERROR-DESC   PIC X(80).
 *      05  BCT-STATISTICS.
 *          10  BCT-RESTART-COUNT  PIC 9(2) COMP.
 *          10  BCT-ATTEMPT-TS     PIC X(26).
 *          10  BCT-COMPLETE-TS    PIC X(26).
 *      05  BCT-FILLER            PIC X(50).
 * </pre>
 * 
 * Migration Notes:
 * - 88-level condition names converted to enum
 * - OCCURS 10 TIMES converted to List with max size validation
 * - Composite key preserved as separate fields
 * - Added checkpoint tracking fields for restart capability
 */
public class BatchControlRecord {

    public enum Status {
        READY('R'),
        ACTIVE('A'),
        WAITING('W'),
        DONE('D'),
        ERROR('E');

        private final char code;

        Status(char code) {
            this.code = code;
        }

        public char getCode() {
            return code;
        }

        public static Status fromCode(char code) {
            for (Status status : values()) {
                if (status.code == code) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Unknown status code: " + code);
        }
    }

    public static class PrerequisiteJob {
        private String jobName;
        private int sequenceNo;
        private int returnCode;

        public PrerequisiteJob() {}

        public PrerequisiteJob(String jobName, int sequenceNo, int returnCode) {
            this.jobName = jobName;
            this.sequenceNo = sequenceNo;
            this.returnCode = returnCode;
        }

        public String getJobName() {
            return jobName;
        }

        public void setJobName(String jobName) {
            this.jobName = jobName;
        }

        public int getSequenceNo() {
            return sequenceNo;
        }

        public void setSequenceNo(int sequenceNo) {
            this.sequenceNo = sequenceNo;
        }

        public int getReturnCode() {
            return returnCode;
        }

        public void setReturnCode(int returnCode) {
            this.returnCode = returnCode;
        }
    }

    private static final int MAX_PREREQUISITES = 10;

    private String jobName;
    private String processDate;
    private int sequenceNo;
    private Status status;
    private String stepName;
    private String programName;
    private String startTime;
    private String endTime;
    private List<PrerequisiteJob> prerequisites;
    private int returnCode;
    private String errorDescription;
    private int restartCount;
    private LocalDateTime attemptTimestamp;
    private LocalDateTime completeTimestamp;
    
    private long recordsRead;
    private long recordsWritten;

    public BatchControlRecord() {
        this.status = Status.READY;
        this.prerequisites = new ArrayList<>();
        this.returnCode = 0;
        this.restartCount = 0;
        this.recordsRead = 0;
        this.recordsWritten = 0;
    }

    public String getCompositeKey() {
        return jobName + processDate + String.format("%04d", sequenceNo);
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

    public int getSequenceNo() {
        return sequenceNo;
    }

    public void setSequenceNo(int sequenceNo) {
        this.sequenceNo = sequenceNo;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
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

    public List<PrerequisiteJob> getPrerequisites() {
        return prerequisites;
    }

    public void addPrerequisite(PrerequisiteJob prereq) {
        if (prerequisites.size() >= MAX_PREREQUISITES) {
            throw new IllegalStateException("Maximum prerequisites (" + MAX_PREREQUISITES + ") exceeded");
        }
        prerequisites.add(prereq);
    }

    public int getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public int getRestartCount() {
        return restartCount;
    }

    public void setRestartCount(int restartCount) {
        this.restartCount = restartCount;
    }

    public LocalDateTime getAttemptTimestamp() {
        return attemptTimestamp;
    }

    public void setAttemptTimestamp(LocalDateTime attemptTimestamp) {
        this.attemptTimestamp = attemptTimestamp;
    }

    public LocalDateTime getCompleteTimestamp() {
        return completeTimestamp;
    }

    public void setCompleteTimestamp(LocalDateTime completeTimestamp) {
        this.completeTimestamp = completeTimestamp;
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
