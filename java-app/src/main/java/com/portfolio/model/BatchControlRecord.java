package com.portfolio.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Batch Control Record entity.
 * Migrated from: BCHCTL.cpy copybook (lines 9-39).
 * Status codes (level-88): R=Ready, A=Active, W=Waiting, D=Done, E=Error
 *
 * The BCT-PREREQ-JOBS OCCURS 10 TIMES array is stored in a separate
 * batch_control_prereqs table (see V5 migration).
 */
@Entity
@Table(name = "batch_control")
public class BatchControlRecord {

    @EmbeddedId
    private BatchControlKey key;

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

    @Column(name = "prereq_count", nullable = false)
    private int prereqCount;

    @Column(name = "return_code", nullable = false)
    private int returnCode;

    @Column(name = "error_desc", length = 80)
    private String errorDesc;

    @Column(name = "restart_count", nullable = false)
    private int restartCount;

    @Column(name = "attempt_ts")
    private LocalDateTime attemptTs;

    @Column(name = "complete_ts")
    private LocalDateTime completeTs;

    public BatchControlRecord() {
    }

    public BatchControlKey getKey() {
        return key;
    }

    public void setKey(BatchControlKey key) {
        this.key = key;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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

    public LocalDateTime getAttemptTs() {
        return attemptTs;
    }

    public void setAttemptTs(LocalDateTime attemptTs) {
        this.attemptTs = attemptTs;
    }

    public LocalDateTime getCompleteTs() {
        return completeTs;
    }

    public void setCompleteTs(LocalDateTime completeTs) {
        this.completeTs = completeTs;
    }
}
