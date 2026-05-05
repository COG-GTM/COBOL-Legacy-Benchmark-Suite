package com.portfolio.portmstr.model;

import com.portfolio.portmstr.model.enums.BatchControlStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Batch Control Record entity.
 * Mapped from COBOL copybook BCHCTL.cpy (BATCH-CONTROL-RECORD).
 * Manages job-level sequencing and dependencies.
 */
@Entity
@Table(name = "BATCH_CONTROL")
public class BatchControlRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "JOB_NAME", length = 8, nullable = false)
    private String jobName;

    @Column(name = "PROCESS_DATE", nullable = false)
    private LocalDate processDate;

    @Column(name = "SEQUENCE_NO", nullable = false)
    private Integer sequenceNo;

    @Column(name = "STATUS", length = 1, nullable = false)
    @Enumerated(EnumType.STRING)
    private BatchControlStatus status;

    @Column(name = "STEP_NAME", length = 8)
    private String stepName;

    @Column(name = "PROGRAM_NAME", length = 8)
    private String programName;

    @Column(name = "START_TIME")
    private LocalDateTime startTime;

    @Column(name = "END_TIME")
    private LocalDateTime endTime;

    @Column(name = "RETURN_CODE")
    private Integer returnCode;

    @Column(name = "ERROR_DESC", length = 80)
    private String errorDesc;

    @Column(name = "RESTART_COUNT")
    private Integer restartCount;

    @Column(name = "ATTEMPT_TIMESTAMP")
    private LocalDateTime attemptTimestamp;

    @Column(name = "COMPLETE_TIMESTAMP")
    private LocalDateTime completeTimestamp;

    public BatchControlRecord() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public LocalDate getProcessDate() {
        return processDate;
    }

    public void setProcessDate(LocalDate processDate) {
        this.processDate = processDate;
    }

    public Integer getSequenceNo() {
        return sequenceNo;
    }

    public void setSequenceNo(Integer sequenceNo) {
        this.sequenceNo = sequenceNo;
    }

    public BatchControlStatus getStatus() {
        return status;
    }

    public void setStatus(BatchControlStatus status) {
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

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Integer getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(Integer returnCode) {
        this.returnCode = returnCode;
    }

    public String getErrorDesc() {
        return errorDesc;
    }

    public void setErrorDesc(String errorDesc) {
        this.errorDesc = errorDesc;
    }

    public Integer getRestartCount() {
        return restartCount;
    }

    public void setRestartCount(Integer restartCount) {
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
