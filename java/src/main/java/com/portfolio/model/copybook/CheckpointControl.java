package com.portfolio.model.copybook;

import java.util.ArrayList;
import java.util.List;

/**
 * Migrated from copybook {@code src/copybook/batch/CKPRST.cpy}
 * (01 CHECKPOINT-CONTROL and 01 CHECKPOINT-RECORD).
 *
 * <p>Program-level checkpoint/restart control structure. In the Spring Batch
 * migration, most of this responsibility is handled by the Spring Batch
 * JobRepository/ExecutionContext, but the structure is preserved for programs
 * that carry explicit checkpoint state.
 */
public class CheckpointControl {

    /** CK-PROGRAM-ID PIC X(8). */
    private String programId;

    /** CK-RUN-DATE PIC X(8) — YYYYMMDD. */
    private String runDate;

    /** CK-RUN-TIME PIC X(6) — HHMMSS. */
    private String runTime;

    /** CK-STATUS PIC X(1) — I=Initial, A=Active, C=Complete, F=Failed, R=Restarted (level-88s). */
    private String status;

    /** CK-RECORDS-READ PIC 9(9) COMP. */
    private long recordsRead;

    /** CK-RECORDS-PROC PIC 9(9) COMP. */
    private long recordsProcessed;

    /** CK-RECORDS-ERROR PIC 9(9) COMP. */
    private long recordsError;

    /** CK-RESTART-COUNT PIC 9(2) COMP. */
    private int restartCount;

    /** CK-LAST-KEY PIC X(50). */
    private String lastKey;

    /** CK-LAST-TIME PIC X(26). */
    private String lastTime;

    /** CK-PHASE PIC X(2) — 00=Init, 10=Read, 20=Proc, 30=Updt, 40=Term (level-88s). */
    private String phase;

    /** CK-FILE-STATUS OCCURS 5 TIMES. */
    private List<FileStatusEntry> fileStatuses = new ArrayList<>();

    /** CK-COMMIT-FREQ PIC 9(5) COMP VALUE 1000. */
    private int commitFrequency = 1000;

    /** CK-MAX-ERRORS PIC 9(3) COMP VALUE 100. */
    private int maxErrors = 100;

    /** CK-MAX-RESTARTS PIC 9(2) COMP VALUE 3. */
    private int maxRestarts = 3;

    /** CK-RESTART-MODE PIC X(1) — N=Normal, R=Restart, C=Recover (level-88s). */
    private String restartMode = "N";

    /** One entry of CK-FILE-STATUS OCCURS 5 TIMES. */
    public static class FileStatusEntry {
        /** CK-FILE-NAME PIC X(8). */
        private String fileName;
        /** CK-FILE-POS PIC X(50). */
        private String filePosition;
        /** CK-FILE-STATUS PIC X(2). */
        private String fileStatus;

        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getFilePosition() { return filePosition; }
        public void setFilePosition(String filePosition) { this.filePosition = filePosition; }
        public String getFileStatus() { return fileStatus; }
        public void setFileStatus(String fileStatus) { this.fileStatus = fileStatus; }
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
    public List<FileStatusEntry> getFileStatuses() { return fileStatuses; }
    public void setFileStatuses(List<FileStatusEntry> fileStatuses) { this.fileStatuses = fileStatuses; }
    public int getCommitFrequency() { return commitFrequency; }
    public void setCommitFrequency(int commitFrequency) { this.commitFrequency = commitFrequency; }
    public int getMaxErrors() { return maxErrors; }
    public void setMaxErrors(int maxErrors) { this.maxErrors = maxErrors; }
    public int getMaxRestarts() { return maxRestarts; }
    public void setMaxRestarts(int maxRestarts) { this.maxRestarts = maxRestarts; }
    public String getRestartMode() { return restartMode; }
    public void setRestartMode(String restartMode) { this.restartMode = restartMode; }
}
