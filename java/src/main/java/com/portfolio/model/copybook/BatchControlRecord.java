package com.portfolio.model.copybook;

import java.util.ArrayList;
import java.util.List;

/**
 * Migrated from copybook {@code src/copybook/batch/BCHCTL.cpy} (01 BATCH-CONTROL-RECORD).
 *
 * <p>Job-level control and process sequencing record stored on the VSAM batch
 * control file. Key = BCT-KEY (job name + process date + sequence no).
 */
public class BatchControlRecord {

    /** BCT-JOB-NAME PIC X(8). */
    private String jobName;

    /** BCT-PROCESS-DATE PIC X(8) — YYYYMMDD. */
    private String processDate;

    /** BCT-SEQUENCE-NO PIC 9(4). */
    private int sequenceNo;

    /** BCT-STATUS PIC X(1) — R=Ready, A=Active, W=Waiting, D=Done, E=Error (level-88s). */
    private String status;

    /** BCT-STEP-NAME PIC X(8). */
    private String stepName;

    /** BCT-PROGRAM-NAME PIC X(8). */
    private String programName;

    /** BCT-START-TIME PIC X(8). */
    private String startTime;

    /** BCT-END-TIME PIC X(8). */
    private String endTime;

    /** BCT-PREREQ-JOBS OCCURS 10 TIMES (BCT-PREREQ-COUNT PIC 9(2) COMP tracks count). */
    private List<PrerequisiteJob> prerequisiteJobs = new ArrayList<>();

    /** BCT-RETURN-CODE PIC S9(4) COMP. */
    private int returnCode;

    /** BCT-ERROR-DESC PIC X(80). */
    private String errorDesc;

    /** BCT-RESTART-COUNT PIC 9(2) COMP. */
    private int restartCount;

    /** BCT-ATTEMPT-TS PIC X(26). */
    private String attemptTimestamp;

    /** BCT-COMPLETE-TS PIC X(26). */
    private String completeTimestamp;

    /**
     * One entry of BCT-PREREQ-JOBS OCCURS 10 TIMES.
     */
    public static class PrerequisiteJob {
        /** BCT-PREREQ-NAME PIC X(8). */
        private String name;
        /** BCT-PREREQ-SEQ PIC 9(4). */
        private int sequence;
        /** BCT-PREREQ-RC PIC S9(4) COMP. */
        private int returnCode;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getSequence() { return sequence; }
        public void setSequence(int sequence) { this.sequence = sequence; }
        public int getReturnCode() { return returnCode; }
        public void setReturnCode(int returnCode) { this.returnCode = returnCode; }
    }

    public String getJobName() { return jobName; }
    public void setJobName(String jobName) { this.jobName = jobName; }
    public String getProcessDate() { return processDate; }
    public void setProcessDate(String processDate) { this.processDate = processDate; }
    public int getSequenceNo() { return sequenceNo; }
    public void setSequenceNo(int sequenceNo) { this.sequenceNo = sequenceNo; }
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
    public List<PrerequisiteJob> getPrerequisiteJobs() { return prerequisiteJobs; }
    public void setPrerequisiteJobs(List<PrerequisiteJob> prerequisiteJobs) { this.prerequisiteJobs = prerequisiteJobs; }
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
