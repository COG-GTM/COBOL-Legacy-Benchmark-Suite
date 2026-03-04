package com.cobolbenchmark.common;

/**
 * Batch Constants - migrated from BCHCON.cpy.
 * Return code thresholds and batch processing constants.
 */
public final class BatchConstants {

    private BatchConstants() {
        // Utility class
    }

    // Return code thresholds from BCHCON.cpy
    public static final int RC_SUCCESS = 0;
    public static final int RC_WARNING = 4;
    public static final int RC_ERROR = 8;
    public static final int RC_SEVERE = 12;
    public static final int RC_CRITICAL = 16;

    // Batch processing limits from CKPRST.cpy
    public static final int DEFAULT_COMMIT_INTERVAL = 1000;
    public static final int DEFAULT_MAX_ERRORS = 100;
    public static final int DEFAULT_MAX_RESTARTS = 3;

    // Process table limits from PRCSEQ.cpy
    public static final int MAX_PROCESS_TABLE_ENTRIES = 100;
    public static final int MAX_DEPENDENCIES_PER_PROCESS = 10;

    // File status slots from CKPRST.cpy
    public static final int MAX_FILE_STATUS_ENTRIES = 5;

    /**
     * Return Code Classification enum - from BCHCON.cpy.
     * SUCCESS(0), WARNING(4), ERROR(8), SEVERE(12), CRITICAL(16).
     */
    public enum ReturnCode {
        SUCCESS(0),
        WARNING(4),
        ERROR(8),
        SEVERE(12),
        CRITICAL(16);

        private final int threshold;

        ReturnCode(int threshold) {
            this.threshold = threshold;
        }

        public int getThreshold() {
            return threshold;
        }

        /**
         * Classify a numeric return code into a ReturnCode category.
         * 0 = SUCCESS, 1-4 = WARNING, 5-8 = ERROR, 9-12 = SEVERE, 13+ = CRITICAL.
         */
        public static ReturnCode classify(int code) {
            if (code == 0) return SUCCESS;
            if (code <= 4) return WARNING;
            if (code <= 8) return ERROR;
            if (code <= 12) return SEVERE;
            return CRITICAL;
        }
    }
}
