package com.portfolio.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "checkpoint_control")
public class CheckpointControl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "program_id", length = 8, nullable = false)
    private String programId;

    @Column(name = "run_date", length = 8)
    private String runDate;

    @Column(name = "run_time", length = 6)
    private String runTime;

    @Column(name = "status", length = 1, nullable = false)
    private Character status;

    @Column(name = "records_read")
    private Long recordsRead;

    @Column(name = "records_processed")
    private Long recordsProcessed;

    @Column(name = "records_error")
    private Long recordsError;

    @Column(name = "restart_count")
    private Integer restartCount;

    @Column(name = "last_key", length = 50)
    private String lastKey;

    @Column(name = "last_time")
    private LocalDateTime lastTime;

    @Column(name = "phase", length = 2)
    private String phase;

    @Column(name = "commit_frequency")
    private Integer commitFrequency;

    @Column(name = "max_errors")
    private Integer maxErrors;

    @Column(name = "max_restarts")
    private Integer maxRestarts;

    @Column(name = "restart_mode", length = 1)
    private Character restartMode;

    public CheckpointControl() {
        this.commitFrequency = 1000;
        this.maxErrors = 100;
        this.maxRestarts = 3;
        this.restartMode = 'N';
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProgramId() {
        return programId;
    }

    public void setProgramId(String programId) {
        this.programId = programId;
    }

    public String getRunDate() {
        return runDate;
    }

    public void setRunDate(String runDate) {
        this.runDate = runDate;
    }

    public String getRunTime() {
        return runTime;
    }

    public void setRunTime(String runTime) {
        this.runTime = runTime;
    }

    public Character getStatus() {
        return status;
    }

    public void setStatus(Character status) {
        this.status = status;
    }

    public Long getRecordsRead() {
        return recordsRead;
    }

    public void setRecordsRead(Long recordsRead) {
        this.recordsRead = recordsRead;
    }

    public Long getRecordsProcessed() {
        return recordsProcessed;
    }

    public void setRecordsProcessed(Long recordsProcessed) {
        this.recordsProcessed = recordsProcessed;
    }

    public Long getRecordsError() {
        return recordsError;
    }

    public void setRecordsError(Long recordsError) {
        this.recordsError = recordsError;
    }

    public Integer getRestartCount() {
        return restartCount;
    }

    public void setRestartCount(Integer restartCount) {
        this.restartCount = restartCount;
    }

    public String getLastKey() {
        return lastKey;
    }

    public void setLastKey(String lastKey) {
        this.lastKey = lastKey;
    }

    public LocalDateTime getLastTime() {
        return lastTime;
    }

    public void setLastTime(LocalDateTime lastTime) {
        this.lastTime = lastTime;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public Integer getCommitFrequency() {
        return commitFrequency;
    }

    public void setCommitFrequency(Integer commitFrequency) {
        this.commitFrequency = commitFrequency;
    }

    public Integer getMaxErrors() {
        return maxErrors;
    }

    public void setMaxErrors(Integer maxErrors) {
        this.maxErrors = maxErrors;
    }

    public Integer getMaxRestarts() {
        return maxRestarts;
    }

    public void setMaxRestarts(Integer maxRestarts) {
        this.maxRestarts = maxRestarts;
    }

    public Character getRestartMode() {
        return restartMode;
    }

    public void setRestartMode(Character restartMode) {
        this.restartMode = restartMode;
    }
}
