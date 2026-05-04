package com.portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Batch Control Record entity - migrated from COBOL BCHCTL.cpy.
 *
 * COBOL level-88 mappings:
 * - BCT-STATUS: R=Ready, A=Active, W=Waiting, D=Done, E=Error
 * - BCT-PREREQ-JOBS OCCURS 10 TIMES -> List&lt;PrerequisiteJob&gt;
 */
@Entity
@Table(name = "batch_control")
@IdClass(BatchControlId.class)
public class BatchControlRecord {

    @Id
    @Column(name = "job_name", length = 8, nullable = false)
    private String jobName;

    @Id
    @Column(name = "process_date", nullable = false)
    private LocalDate processDate;

    @Id
    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;

    @Column(name = "status", length = 1, nullable = false)
    private String status;

    @Column(name = "step_name", length = 8)
    private String stepName;

    @Column(name = "program_name", length = 8)
    private String programName;

    @Column(name = "start_time", length = 8)
    private String startTime;

    @Column(name = "end_time", length = 8)
    private String endTime;

    @Column(name = "prereq_count")
    private int prereqCount;

    @Column(name = "return_code")
    private int returnCode;

    @Column(name = "error_desc", length = 80)
    private String errorDesc;

    @Column(name = "restart_count")
    private int restartCount;

    @Column(name = "attempt_ts")
    private LocalDateTime attemptTs;

    @Column(name = "complete_ts")
    private LocalDateTime completeTs;

    @Transient
    private List<PrerequisiteJob> prerequisiteJobs = new ArrayList<>();

    public BatchControlRecord() {
        this.status = "R";
        this.processDate = LocalDate.now();
    }

    public String getJobName() { return jobName; }
    public void setJobName(String jobName) { this.jobName = jobName; }
    public LocalDate getProcessDate() { return processDate; }
    public void setProcessDate(LocalDate processDate) { this.processDate = processDate; }
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
    public int getPrereqCount() { return prereqCount; }
    public void setPrereqCount(int prereqCount) { this.prereqCount = prereqCount; }
    public int getReturnCode() { return returnCode; }
    public void setReturnCode(int returnCode) { this.returnCode = returnCode; }
    public String getErrorDesc() { return errorDesc; }
    public void setErrorDesc(String errorDesc) { this.errorDesc = errorDesc; }
    public int getRestartCount() { return restartCount; }
    public void setRestartCount(int restartCount) { this.restartCount = restartCount; }
    public LocalDateTime getAttemptTs() { return attemptTs; }
    public void setAttemptTs(LocalDateTime attemptTs) { this.attemptTs = attemptTs; }
    public LocalDateTime getCompleteTs() { return completeTs; }
    public void setCompleteTs(LocalDateTime completeTs) { this.completeTs = completeTs; }
    public List<PrerequisiteJob> getPrerequisiteJobs() { return prerequisiteJobs; }
    public void setPrerequisiteJobs(List<PrerequisiteJob> prerequisiteJobs) { this.prerequisiteJobs = prerequisiteJobs; }

    public boolean isReady() { return "R".equals(status); }
    public boolean isActive() { return "A".equals(status); }
    public boolean isWaiting() { return "W".equals(status); }
    public boolean isDone() { return "D".equals(status); }
    public boolean isError() { return "E".equals(status); }

    /**
     * Represents BCT-PREREQ-JOBS OCCURS 10 TIMES from BCHCTL.cpy.
     */
    public static class PrerequisiteJob {
        private String prereqName;
        private int prereqSeq;
        private int prereqRc;

        public PrerequisiteJob() {}

        public PrerequisiteJob(String prereqName, int prereqSeq, int prereqRc) {
            this.prereqName = prereqName;
            this.prereqSeq = prereqSeq;
            this.prereqRc = prereqRc;
        }

        public String getPrereqName() { return prereqName; }
        public void setPrereqName(String prereqName) { this.prereqName = prereqName; }
        public int getPrereqSeq() { return prereqSeq; }
        public void setPrereqSeq(int prereqSeq) { this.prereqSeq = prereqSeq; }
        public int getPrereqRc() { return prereqRc; }
        public void setPrereqRc(int prereqRc) { this.prereqRc = prereqRc; }
    }
}
