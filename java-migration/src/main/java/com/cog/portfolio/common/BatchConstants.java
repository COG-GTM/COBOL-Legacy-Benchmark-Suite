package com.cog.portfolio.common;

/** BCHCON.cpy BATCH-CONTROL-CONSTANTS (values still meaningful with Spring Batch). */
public final class BatchConstants {

    public static final int MAX_RESTARTS = 3;
    public static final int WAIT_INTERVAL_SECONDS = 300;
    public static final int MAX_WAIT_TIME_SECONDS = 3600;

    public static final String MSG_START = "Process starting...";
    public static final String MSG_SUCCESS = "Process completed successfully";
    public static final String MSG_FAILURE = "Process failed - check errors";
    public static final String MSG_WAITING = "Waiting for prerequisites";

    public static final String JOB_DAILY = "dailyProcessingJob";
    public static final String JOB_POSITION_REPORT = "positionReportJob";
    public static final String JOB_AUDIT_REPORT = "auditReportJob";
    public static final String JOB_STATISTICS_REPORT = "statisticsReportJob";
    public static final String JOB_RETURN_CODE_ANALYSIS = "returnCodeAnalysisJob";

    /** BCHCON BCT-PROC-TYPES. */
    public enum ProcessType implements CodedValue {
        INITIAL("INI"), UPDATE("UPD"), REPORT("RPT"), CLEANUP("CLN");

        private final String code;

        ProcessType(String code) {
            this.code = code;
        }

        @Override
        public String code() {
            return code;
        }
    }

    private BatchConstants() {
    }
}
