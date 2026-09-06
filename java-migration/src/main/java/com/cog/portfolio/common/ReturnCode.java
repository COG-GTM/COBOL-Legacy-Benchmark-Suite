package com.cog.portfolio.common;

import org.springframework.batch.core.ExitStatus;

/** COMMON.cpy RETURN-CODES / BCHCON.cpy BCT-RC-THRESHOLDS. */
public enum ReturnCode {
    SUCCESS(0),
    WARNING(4),
    ERROR(8),
    SEVERE(12),
    CRITICAL(16);

    /** PRCSEQ / BCHCTL continue the daily flow while RC <= 4. */
    public static final int MAX_CONTINUE_CODE = 4;

    private final int code;

    ReturnCode(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public boolean allowsContinuation() {
        return code <= MAX_CONTINUE_CODE;
    }

    public ExitStatus toExitStatus() {
        return switch (this) {
            case SUCCESS -> ExitStatus.COMPLETED;
            case WARNING -> new ExitStatus("COMPLETED_WITH_WARNINGS", "RC=4");
            default -> new ExitStatus("FAILED", "RC=" + code);
        };
    }

    public static ReturnCode fromCode(int code) {
        for (ReturnCode rc : values()) {
            if (rc.code == code) {
                return rc;
            }
        }
        throw new IllegalArgumentException("Unknown return code " + code);
    }

    public static ReturnCode max(ReturnCode a, ReturnCode b) {
        return a.code >= b.code ? a : b;
    }
}
