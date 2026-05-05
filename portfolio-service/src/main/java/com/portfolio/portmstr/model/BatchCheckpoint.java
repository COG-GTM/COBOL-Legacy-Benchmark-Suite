package com.portfolio.portmstr.model;

import com.portfolio.portmstr.model.enums.CheckpointStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Batch Checkpoint entity.
 * Mapped from COBOL copybook CKPRST.cpy (CHECKPOINT-CONTROL / CHECKPOINT-RECORD).
 * Preserves the checkpoint/restart mechanism from mainframe batch processing.
 */
@Entity
@Table(name = "BATCH_CHECKPOINT")
@IdClass(BatchCheckpointId.class)
public class BatchCheckpoint {

    @Id
    @Column(name = "PROGRAM_ID", length = 8, nullable = false)
    private String programId;

    @Id
    @Column(name = "RUN_DATE", nullable = false)
    private LocalDate runDate;

    @Column(name = "RUN_TIME")
    private LocalTime runTime;

    @Column(name = "STATUS", length = 1, nullable = false)
    @Enumerated(EnumType.STRING)
    private CheckpointStatus status;

    @Column(name = "RECORDS_READ")
    private Long recordsRead;

    @Column(name = "RECORDS_PROCESSED")
    private Long recordsProcessed;

    @Column(name = "RECORDS_ERROR")
    private Long recordsError;

    @Column(name = "RESTART_COUNT")
    private Integer restartCount;

    @Column(name = "LAST_KEY", length = 50)
    private String lastKey;

    @Column(name = "LAST_CHECKPOINT_TIME")
    private LocalDateTime lastCheckpointTime;

    @Column(name = "PHASE", length = 2)
    private String phase;

    @Column(name = "COMMIT_FREQUENCY")
    private Integer commitFrequency;

    @Column(name = "MAX_ERRORS")
    private Integer maxErrors;

    @Column(name = "MAX_RESTARTS")
    private Integer maxRestarts;

    @Column(name = "RESTART_MODE", length = 1)
    private Character restartMode;

    public BatchCheckpoint() {
    }

    public String getProgramId() {
        return programId;
    }

    public void setProgramId(String programId) {
        this.programId = programId;
    }

    public LocalDate getRunDate() {
        return runDate;
    }

    public void setRunDate(LocalDate runDate) {
        this.runDate = runDate;
    }

    public LocalTime getRunTime() {
        return runTime;
    }

    public void setRunTime(LocalTime runTime) {
        this.runTime = runTime;
    }

    public CheckpointStatus getStatus() {
        return status;
    }

    public void setStatus(CheckpointStatus status) {
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

    public LocalDateTime getLastCheckpointTime() {
        return lastCheckpointTime;
    }

    public void setLastCheckpointTime(LocalDateTime lastCheckpointTime) {
        this.lastCheckpointTime = lastCheckpointTime;
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
