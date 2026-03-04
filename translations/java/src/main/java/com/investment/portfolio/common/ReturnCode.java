package com.investment.portfolio.common;

/**
 * Return Code Constants - Java equivalent of COMMON.cpy return codes
 * and RTNCODE.cpy return code management.
 *
 * Maps standard z/OS batch return code conventions.
 */
public final class ReturnCode {

    /** Successful completion */
    public static final int SUCCESS = 0;

    /** Warning - processing completed with warnings */
    public static final int WARNING = 4;

    /** Error - processing completed with errors */
    public static final int ERROR = 8;

    /** Severe error - critical failure */
    public static final int SEVERE = 12;

    /** Critical error - environment/system failure */
    public static final int CRITICAL = 16;

    private int currentCode;
    private int highestCode;
    private String programId;
    private String message;

    public ReturnCode(String programId) {
        this.programId = programId;
        this.currentCode = SUCCESS;
        this.highestCode = SUCCESS;
    }

    /**
     * Sets the current return code and tracks the highest code seen.
     */
    public void setCode(int code) {
        this.currentCode = code;
        if (code > highestCode) {
            highestCode = code;
        }
    }

    /**
     * Returns human-readable status based on current code.
     */
    public String getStatus() {
        if (currentCode == SUCCESS) return "SUCCESS";
        if (currentCode <= WARNING) return "WARNING";
        if (currentCode <= ERROR) return "ERROR";
        if (currentCode <= SEVERE) return "SEVERE";
        return "CRITICAL";
    }

    public boolean isSuccess() { return currentCode == SUCCESS; }
    public boolean isWarningOrBetter() { return currentCode <= WARNING; }
    public boolean hasErrors() { return currentCode >= ERROR; }

    public int getCurrentCode() { return currentCode; }
    public int getHighestCode() { return highestCode; }
    public String getProgramId() { return programId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    @Override
    public String toString() {
        return "ReturnCode{program='" + programId + "', code=" + currentCode +
                ", highest=" + highestCode + ", status='" + getStatus() + "'}";
    }
}
