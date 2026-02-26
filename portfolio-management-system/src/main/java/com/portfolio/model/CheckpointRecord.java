package com.portfolio.model;

import java.time.LocalDateTime;

/**
 * Checkpoint Record.
 * Migrated from COBOL CKPRST copybook.
 * Used for checkpoint/restart functionality in batch processing.
 * In Java, Spring Batch JobRepository handles this natively.
 */
public class CheckpointRecord {

    private String programId;
    private String runDate;
    private String runTime;
    private String status;

    // Counters from CKPRST copybook
    private long recordsRead;
    private long recordsProcessed;
    private long recordsError;
    private int restartCount;

    // Position tracking
    private String lastKey;
    private LocalDateTime lastTime;
    private String phase;

    // Control info from CKPRST copybook
    private int commitFrequency = 1000;
    private int maxErrors = 100;
    private int maxRestarts = 3;
    private String restartMode;

    // Status constants from CKPRST copybook
    public static final String STATUS_INITIAL = "I";
    public static final String STATUS_ACTIVE = "A";
    public static final String STATUS_COMPLETE = "C";
    public static final String STATUS_FAILED = "F";
    public static final String STATUS_RESTARTED = "R";

    // Phase constants from CKPRST copybook
    public static final String PHASE_INIT = "00";
    public static final String PHASE_READ = "10";
    public static final String PHASE_PROC = "20";
    public static final String PHASE_UPDATE = "30";
    public static final String PHASE_TERM = "40";

    // Restart mode constants
    public static final String MODE_NORMAL = "N";
    public static final String MODE_RESTART = "R";
    public static final String MODE_RECOVER = "C";

    public CheckpointRecord() {}

    // Getters and setters
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

    public LocalDateTime getLastTime() { return lastTime; }
    public void setLastTime(LocalDateTime lastTime) { this.lastTime = lastTime; }

    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }

    public int getCommitFrequency() { return commitFrequency; }
    public void setCommitFrequency(int commitFrequency) { this.commitFrequency = commitFrequency; }

    public int getMaxErrors() { return maxErrors; }
    public void setMaxErrors(int maxErrors) { this.maxErrors = maxErrors; }

    public int getMaxRestarts() { return maxRestarts; }
    public void setMaxRestarts(int maxRestarts) { this.maxRestarts = maxRestarts; }

    public String getRestartMode() { return restartMode; }
    public void setRestartMode(String restartMode) { this.restartMode = restartMode; }
}
