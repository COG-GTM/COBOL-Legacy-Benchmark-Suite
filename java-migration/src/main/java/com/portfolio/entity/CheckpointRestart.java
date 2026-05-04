package com.portfolio.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "checkpoint_restart")
public class CheckpointRestart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "program_id", length = 8, nullable = false)
    private String programId;

    @Column(name = "run_date", length = 8)
    private String runDate;

    @Column(name = "run_time", length = 6)
    private String runTime;

    @Column(name = "status", length = 1)
    private String status;

    @Column(name = "records_read")
    private long recordsRead;

    @Column(name = "records_processed")
    private long recordsProcessed;

    @Column(name = "records_error")
    private long recordsError;

    @Column(name = "restart_count")
    private int restartCount;

    @Column(name = "last_key", length = 50)
    private String lastKey;

    @Column(name = "last_checkpoint_time")
    private LocalDateTime lastCheckpointTime;

    @Column(name = "phase", length = 2)
    private String phase;

    @Column(name = "commit_frequency")
    private int commitFrequency;

    @Column(name = "max_errors")
    private int maxErrors;

    public CheckpointRestart() {
        this.commitFrequency = 1000;
        this.maxErrors = 100;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProgramId() { return programId; }
    public void setProgramId(String programId) { this.programId = programId; }

    public String getRunDate() { return runDate; }
    public void setRunDate(String runDate) { this.runDate = runDate; }

    public String getRunTime() { return runTime; }
    public void setRunTime(String runTime) { this.runTime = runTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getRecordsRead() { return recordsRead; }
    public void setRecordsRead(long recordsRead) { this.recordsRead = recordsRead; }

    public long getRecordsProcessed() { return recordsProcessed; }
    public void setRecordsProcessed(long recordsProcessed) { this.recordsProcessed = recordsProcessed; }

    public long getRecordsError() { return recordsError; }
    public void setRecordsError(long recordsError) { this.recordsError = recordsError; }

    public int getRestartCount() { return restartCount; }
    public void setRestartCount(int restartCount) { this.restartCount = restartCount; }

    public String getLastKey() { return lastKey; }
    public void setLastKey(String lastKey) { this.lastKey = lastKey; }

    public LocalDateTime getLastCheckpointTime() { return lastCheckpointTime; }
    public void setLastCheckpointTime(LocalDateTime lastCheckpointTime) { this.lastCheckpointTime = lastCheckpointTime; }

    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }

    public int getCommitFrequency() { return commitFrequency; }
    public void setCommitFrequency(int commitFrequency) { this.commitFrequency = commitFrequency; }

    public int getMaxErrors() { return maxErrors; }
    public void setMaxErrors(int maxErrors) { this.maxErrors = maxErrors; }
}
