package com.cobolbenchmark.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Checkpoint Control - migrated from CKPRST.cpy.
 * Manages checkpoint/restart for batch processing.
 * CK-FILE-STATUS OCCURS 5 TIMES → List<FileCheckpoint>.
 */
public class CheckpointControl {

    private String jobName;
    private String stepName;
    private CheckpointPhase phase;
    private int commitFrequency = 1000;
    private int maxErrors = 100;
    private int maxRestarts = 3;
    private int currentRestarts;
    private long recordsProcessed;
    private long lastCommitCount;
    private String lastCheckpointTime;
    private List<FileCheckpoint> fileStatuses = new ArrayList<>();

    public CheckpointControl() {
        // Initialize with 5 file status slots per COBOL OCCURS 5 TIMES
        for (int i = 0; i < 5; i++) {
            fileStatuses.add(new FileCheckpoint());
        }
    }

    /**
     * File checkpoint status - from CK-FILE-STATUS OCCURS 5 TIMES.
     */
    public static class FileCheckpoint {
        private String fileName;
        private long position;
        private String status;

        public FileCheckpoint() {
        }

        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }

        public long getPosition() { return position; }
        public void setPosition(long position) { this.position = position; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    // Getters and Setters

    public String getJobName() { return jobName; }
    public void setJobName(String jobName) { this.jobName = jobName; }

    public String getStepName() { return stepName; }
    public void setStepName(String stepName) { this.stepName = stepName; }

    public CheckpointPhase getPhase() { return phase; }
    public void setPhase(CheckpointPhase phase) { this.phase = phase; }

    public int getCommitFrequency() { return commitFrequency; }
    public void setCommitFrequency(int commitFrequency) { this.commitFrequency = commitFrequency; }

    public int getMaxErrors() { return maxErrors; }
    public void setMaxErrors(int maxErrors) { this.maxErrors = maxErrors; }

    public int getMaxRestarts() { return maxRestarts; }
    public void setMaxRestarts(int maxRestarts) { this.maxRestarts = maxRestarts; }

    public int getCurrentRestarts() { return currentRestarts; }
    public void setCurrentRestarts(int currentRestarts) { this.currentRestarts = currentRestarts; }

    public long getRecordsProcessed() { return recordsProcessed; }
    public void setRecordsProcessed(long recordsProcessed) { this.recordsProcessed = recordsProcessed; }

    public long getLastCommitCount() { return lastCommitCount; }
    public void setLastCommitCount(long lastCommitCount) { this.lastCommitCount = lastCommitCount; }

    public String getLastCheckpointTime() { return lastCheckpointTime; }
    public void setLastCheckpointTime(String lastCheckpointTime) { this.lastCheckpointTime = lastCheckpointTime; }

    public List<FileCheckpoint> getFileStatuses() { return fileStatuses; }
    public void setFileStatuses(List<FileCheckpoint> fileStatuses) { this.fileStatuses = fileStatuses; }
}
