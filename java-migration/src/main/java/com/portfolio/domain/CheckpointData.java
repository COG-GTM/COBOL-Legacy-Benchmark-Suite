package com.portfolio.domain;

/**
 * Checkpoint data - migrated from COBOL CKPRST.cpy.
 * In Spring Batch, most checkpoint state is managed by ExecutionContext.
 * This DTO preserves the COBOL structure for reference and interop.
 */
public class CheckpointData {

    private String programId;
    private String runDate;
    private String runTime;
    private String status;
    private long recordsRead;
    private long recordsProcessed;
    private long recordsError;
    private int restartCount;
    private String lastKey;
    private String lastTime;
    private String phase;
    private int commitFrequency;
    private int maxErrors;
    private int maxRestarts;
    private String restartMode;

    public CheckpointData() {
        this.commitFrequency = 1000;
        this.maxErrors = 100;
        this.maxRestarts = 3;
        this.restartMode = "N";
    }

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
    public String getLastTime() { return lastTime; }
    public void setLastTime(String lastTime) { this.lastTime = lastTime; }
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
