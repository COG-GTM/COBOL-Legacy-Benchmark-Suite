package com.portfolio.domain;

/**
 * Batch control constants - migrated from COBOL BCHCON.cpy.
 */
public final class BatchConstants {

    private BatchConstants() {}

    // Process Status Values
    public static final String STATUS_READY = "R";
    public static final String STATUS_ACTIVE = "A";
    public static final String STATUS_WAITING = "W";
    public static final String STATUS_DONE = "D";
    public static final String STATUS_ERROR = "E";

    // Return Code Thresholds
    public static final int RC_SUCCESS = 0;
    public static final int RC_WARNING = 4;
    public static final int RC_ERROR = 8;
    public static final int RC_SEVERE = 12;
    public static final int RC_CRITICAL = 16;

    // Process Control Values
    public static final int MAX_PREREQ = 10;
    public static final int MAX_RESTARTS = 3;
    public static final int WAIT_INTERVAL = 300;
    public static final int MAX_WAIT_TIME = 3600;

    // Process Types
    public static final String TYPE_INITIAL = "INI";
    public static final String TYPE_UPDATE = "UPD";
    public static final String TYPE_REPORT = "RPT";
    public static final String TYPE_CLEANUP = "CLN";

    // Dependency Types
    public static final String DEP_REQUIRED = "R";
    public static final String DEP_OPTIONAL = "O";
    public static final String DEP_EXCLUSIVE = "X";

    // Special Process Names
    public static final String START_OF_DAY = "STARTDAY";
    public static final String END_OF_DAY = "ENDDAY";
    public static final String EMERGENCY = "EMERGENCY";

    // Standard Messages
    public static final String MSG_STARTING = "Process starting...";
    public static final String MSG_COMPLETE = "Process completed successfully";
    public static final String MSG_FAILED = "Process failed - check errors";
    public static final String MSG_WAITING = "Waiting for prerequisites";
}
