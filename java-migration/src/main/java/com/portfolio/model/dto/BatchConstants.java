package com.portfolio.model.dto;

public final class BatchConstants {

    private BatchConstants() {
    }

    // Process Status Values
    public static final char BCT_STAT_READY = 'R';
    public static final char BCT_STAT_ACTIVE = 'A';
    public static final char BCT_STAT_WAITING = 'W';
    public static final char BCT_STAT_DONE = 'D';
    public static final char BCT_STAT_ERROR = 'E';

    // Return Code Thresholds
    public static final int BCT_RC_SUCCESS = 0;
    public static final int BCT_RC_WARNING = 4;
    public static final int BCT_RC_ERROR = 8;
    public static final int BCT_RC_SEVERE = 12;
    public static final int BCT_RC_CRITICAL = 16;

    // Process Control Values
    public static final int BCT_MAX_PREREQ = 10;
    public static final int BCT_MAX_RESTARTS = 3;
    public static final int BCT_WAIT_INTERVAL = 300;
    public static final int BCT_MAX_WAIT_TIME = 3600;

    // Process Types
    public static final String BCT_TYPE_INITIAL = "INI";
    public static final String BCT_TYPE_UPDATE = "UPD";
    public static final String BCT_TYPE_REPORT = "RPT";
    public static final String BCT_TYPE_CLEANUP = "CLN";

    // Dependency Types
    public static final char BCT_DEP_REQUIRED = 'R';
    public static final char BCT_DEP_OPTIONAL = 'O';
    public static final char BCT_DEP_EXCLUSIVE = 'X';

    // Standard Messages
    public static final String BCT_MSG_STARTING = "Process starting...";
    public static final String BCT_MSG_COMPLETE = "Process completed successfully";
    public static final String BCT_MSG_FAILED = "Process failed - check errors";
    public static final String BCT_MSG_WAITING = "Waiting for prerequisites";
}
