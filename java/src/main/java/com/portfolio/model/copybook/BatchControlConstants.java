package com.portfolio.model.copybook;

/**
 * Migrated from copybook {@code src/copybook/batch/BCHCON.cpy}
 * (01 BATCH-CONTROL-CONSTANTS).
 */
public final class BatchControlConstants {

    private BatchControlConstants() {}

    // Process status values (BCT-STAT-VALUES, PIC X(1))
    public static final String STAT_READY = "R";
    public static final String STAT_ACTIVE = "A";
    public static final String STAT_WAITING = "W";
    public static final String STAT_DONE = "D";
    public static final String STAT_ERROR = "E";

    // Return code thresholds (BCT-RC-THRESHOLDS, PIC S9(4) COMP)
    public static final int RC_SUCCESS = 0;
    public static final int RC_WARNING = 4;
    public static final int RC_ERROR = 8;
    public static final int RC_SEVERE = 12;
    public static final int RC_CRITICAL = 16;

    // Process control values (BCT-CTRL-VALUES, PIC 9(n) COMP)
    public static final int MAX_PREREQ = 10;
    public static final int MAX_RESTARTS = 3;
    public static final int WAIT_INTERVAL_SECONDS = 300;
    public static final int MAX_WAIT_TIME_SECONDS = 3600;

    // Process types (BCT-PROC-TYPES, PIC X(3))
    public static final String TYPE_INITIAL = "INI";
    public static final String TYPE_UPDATE = "UPD";
    public static final String TYPE_REPORT = "RPT";
    public static final String TYPE_CLEANUP = "CLN";

    // Dependency types (BCT-DEP-TYPES, PIC X(1))
    public static final String DEP_REQUIRED = "R";
    public static final String DEP_OPTIONAL = "O";
    public static final String DEP_EXCLUSIVE = "X";

    // Special process names (BCT-PROC-NAMES, PIC X(8))
    public static final String START_OF_DAY = "STARTDAY";
    public static final String END_OF_DAY = "ENDDAY";
    public static final String EMERGENCY = "EMERGENCY";

    // Control file record types (BCT-REC-TYPES, PIC X(1))
    public static final String REC_CONTROL = "C";
    public static final String REC_PROCESS = "P";
    public static final String REC_DEPEND = "D";
    public static final String REC_HISTORY = "H";

    // Standard messages (BCT-MESSAGES, PIC X(30))
    public static final String MSG_STARTING = "Process starting...";
    public static final String MSG_COMPLETE = "Process completed successfully";
    public static final String MSG_FAILED = "Process failed - check errors";
    public static final String MSG_WAITING = "Waiting for prerequisites";
}
