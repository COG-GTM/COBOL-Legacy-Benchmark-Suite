package com.clbs.batch;

/** BCHCON.cpy BATCH-CONTROL-CONSTANTS. */
public final class BatchConstants {

    public static final int RC_SUCCESS = 0;
    public static final int RC_WARNING = 4;
    public static final int RC_ERROR = 8;
    public static final int RC_SEVERE = 12;
    public static final int RC_CRITICAL = 16;

    public static final int MAX_PREREQ = 10;
    public static final int MAX_RESTARTS = 3;
    public static final int WAIT_INTERVAL = 300;
    public static final int MAX_WAIT_TIME = 3600;

    public static final String TYPE_INITIAL = "INI";
    public static final String TYPE_UPDATE = "UPD";
    public static final String TYPE_REPORT = "RPT";
    public static final String TYPE_CLEANUP = "CLN";

    public static final String DEP_REQUIRED = "R";
    public static final String DEP_OPTIONAL = "O";
    public static final String DEP_EXCLUSIVE = "X";

    public static final String START_OF_DAY = "STARTDAY";
    public static final String END_OF_DAY = "ENDDAY  ";
    public static final String EMERGENCY = "EMERGENCY";

    public static final String MSG_STARTING = "Process starting...           ";
    public static final String MSG_COMPLETE = "Process completed successfully";
    public static final String MSG_FAILED = "Process failed - check errors ";
    public static final String MSG_WAITING = "Waiting for prerequisites     ";

    private BatchConstants() {
    }
}
