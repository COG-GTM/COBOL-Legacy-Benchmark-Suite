package com.portfolio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(name = "batch_control")
@IdClass(BatchControlId.class)
public class BatchControl {

    @Id
    @Column(name = "job_name", length = 8, nullable = false)
    private String jobName;

    @Id
    @Column(name = "process_date", length = 8, nullable = false)
    private String processDate;

    @Id
    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 10, nullable = false)
    private BatchStatus status;

    @Column(name = "step_name", length = 8)
    @Size(max = 8)
    private String stepName;

    @Column(name = "program_name", length = 8)
    @Size(max = 8)
    private String programName;

    @Column(name = "start_time", length = 8)
    @Size(max = 8)
    private String startTime;

    @Column(name = "end_time", length = 8)
    @Size(max = 8)
    private String endTime;

    @Column(name = "prereq_count")
    private int prereqCount;

    @Column(name = "prereq_jobs", length = 1000)
    @Size(max = 1000)
    private String prereqJobs;

    @Column(name = "return_code")
    private int returnCode;

    @Column(name = "error_desc", length = 80)
    @Size(max = 80)
    private String errorDesc;

    @Column(name = "restart_count")
    private int restartCount;

    @Column(name = "attempt_timestamp")
    private LocalDateTime attemptTimestamp;

    @Column(name = "complete_timestamp")
    private LocalDateTime completeTimestamp;

    public BatchControl() {
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

    public BatchStatus getStatus() {
        return status;
    }

    public void setStatus(BatchStatus status) {
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

    public int getPrereqCount() {
        return prereqCount;
    }

    public void setPrereqCount(int prereqCount) {
        this.prereqCount = prereqCount;
    }

    public String getPrereqJobs() {
        return prereqJobs;
    }

    public void setPrereqJobs(String prereqJobs) {
        this.prereqJobs = prereqJobs;
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
}
