package com.investment.portfolio.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Checkpoint/Restart Control - Java equivalent of CKPRST.cpy
 * Maps the COBOL CHECKPOINT-CONTROL structure.
 */
public class CheckpointControl {

    /** Header */
    private String programId;          // CK-PROGRAM-ID: PIC X(8)
    private String runDate;            // CK-RUN-DATE: PIC X(8)
    private String runTime;            // CK-RUN-TIME: PIC X(6)
    private CheckpointStatus status;   // CK-STATUS: PIC X(1)

    /** Counters */
    private long recordsRead;          // CK-RECORDS-READ: PIC 9(9) COMP
    private long recordsProcessed;     // CK-RECORDS-PROC: PIC 9(9) COMP
    private long recordsInError;       // CK-RECORDS-ERROR: PIC 9(9) COMP
    private int restartCount;          // CK-RESTART-COUNT: PIC 9(2) COMP

    /** Position tracking */
    private String lastKey;            // CK-LAST-KEY: PIC X(50)
    private LocalDateTime lastTime;    // CK-LAST-TIME: PIC X(26)
    private ProcessPhase phase;        // CK-PHASE: PIC X(2)

    /** File resource tracking */
    private List<FileStatus> fileStatuses; // CK-FILE-STATUS OCCURS 5 TIMES

    /** Control parameters */
    private int commitFrequency;       // CK-COMMIT-FREQ: PIC 9(5) COMP VALUE 1000
    private int maxErrors;             // CK-MAX-ERRORS: PIC 9(3) COMP VALUE 100
    private int maxRestarts;           // CK-MAX-RESTARTS: PIC 9(2) COMP VALUE 3
    private RestartMode restartMode;   // CK-RESTART-MODE: PIC X(1)

    public enum CheckpointStatus {
        INITIAL('I'),
        ACTIVE('A'),
        COMPLETE('C'),
        FAILED('F'),
        RESTARTED('R');

        private final char code;

        CheckpointStatus(char code) {
            this.code = code;
        }

        public char getCode() {
            return code;
        }

        public static CheckpointStatus fromCode(char code) {
            for (CheckpointStatus s : values()) {
                if (s.code == code) return s;
            }
            throw new IllegalArgumentException("Invalid checkpoint status: " + code);
        }
    }

    public enum ProcessPhase {
        INIT("00"),
        READ("10"),
        PROCESS("20"),
        UPDATE("30"),
        TERMINATE("40");

        private final String code;

        ProcessPhase(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        public static ProcessPhase fromCode(String code) {
            for (ProcessPhase p : values()) {
                if (p.code.equals(code)) return p;
            }
            throw new IllegalArgumentException("Invalid process phase: " + code);
        }
    }

    public enum RestartMode {
        NORMAL('N'),
        RESTART('R'),
        RECOVER('C');

        private final char code;

        RestartMode(char code) {
            this.code = code;
        }

        public char getCode() {
            return code;
        }

        public static RestartMode fromCode(char code) {
            for (RestartMode m : values()) {
                if (m.code == code) return m;
            }
            throw new IllegalArgumentException("Invalid restart mode: " + code);
        }
    }

    /**
     * File status tracking entry.
     * Maps CK-FILE-STATUS OCCURS 5 TIMES.
     */
    public static class FileStatus {
        private String fileName;       // CK-FILE-NAME: PIC X(8)
        private String filePosition;   // CK-FILE-POS: PIC X(50)
        private String fileStatusCode; // CK-FILE-STATUS: PIC X(2)

        public FileStatus() {}

        public FileStatus(String fileName, String filePosition, String fileStatusCode) {
            this.fileName = fileName;
            this.filePosition = filePosition;
            this.fileStatusCode = fileStatusCode;
        }

        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }

        public String getFilePosition() { return filePosition; }
        public void setFilePosition(String filePosition) { this.filePosition = filePosition; }

        public String getFileStatusCode() { return fileStatusCode; }
        public void setFileStatusCode(String fileStatusCode) { this.fileStatusCode = fileStatusCode; }
    }

    public CheckpointControl() {
        this.fileStatuses = new ArrayList<>();
        this.commitFrequency = 1000;
        this.maxErrors = 100;
        this.maxRestarts = 3;
        this.restartMode = RestartMode.NORMAL;
    }

    // --- Getters and Setters ---

    public String getProgramId() { return programId; }
    public void setProgramId(String programId) { this.programId = programId; }

    public String getRunDate() { return runDate; }
    public void setRunDate(String runDate) { this.runDate = runDate; }

    public String getRunTime() { return runTime; }
    public void setRunTime(String runTime) { this.runTime = runTime; }

    public CheckpointStatus getStatus() { return status; }
    public void setStatus(CheckpointStatus status) { this.status = status; }

    public long getRecordsRead() { return recordsRead; }
    public void setRecordsRead(long recordsRead) { this.recordsRead = recordsRead; }

    public long getRecordsProcessed() { return recordsProcessed; }
    public void setRecordsProcessed(long recordsProcessed) { this.recordsProcessed = recordsProcessed; }

    public long getRecordsInError() { return recordsInError; }
    public void setRecordsInError(long recordsInError) { this.recordsInError = recordsInError; }

    public int getRestartCount() { return restartCount; }
    public void setRestartCount(int restartCount) { this.restartCount = restartCount; }

    public String getLastKey() { return lastKey; }
    public void setLastKey(String lastKey) { this.lastKey = lastKey; }

    public LocalDateTime getLastTime() { return lastTime; }
    public void setLastTime(LocalDateTime lastTime) { this.lastTime = lastTime; }

    public ProcessPhase getPhase() { return phase; }
    public void setPhase(ProcessPhase phase) { this.phase = phase; }

    public List<FileStatus> getFileStatuses() { return fileStatuses; }
    public void setFileStatuses(List<FileStatus> fileStatuses) { this.fileStatuses = fileStatuses; }

    public int getCommitFrequency() { return commitFrequency; }
    public void setCommitFrequency(int commitFrequency) { this.commitFrequency = commitFrequency; }

    public int getMaxErrors() { return maxErrors; }
    public void setMaxErrors(int maxErrors) { this.maxErrors = maxErrors; }

    public int getMaxRestarts() { return maxRestarts; }
    public void setMaxRestarts(int maxRestarts) { this.maxRestarts = maxRestarts; }

    public RestartMode getRestartMode() { return restartMode; }
    public void setRestartMode(RestartMode restartMode) { this.restartMode = restartMode; }
}
