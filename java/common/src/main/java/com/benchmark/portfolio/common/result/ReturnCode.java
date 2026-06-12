package com.benchmark.portfolio.common.result;

/**
 * Standard numeric return codes used across the legacy COBOL system.
 *
 * <p>COBOL origin: the {@code RETURN-CODES} group in
 * {@code src/copybook/common/COMMON.cpy} and the level-88 conditions on
 * {@code RETURN-CODE} in {@code src/copybook/common/RETHND.cpy}
 * ({@code PIC S9(4) COMP}). Both copybooks define the same five values.
 */
public enum ReturnCode {

    /** COBOL {@code RC-SUCCESS VALUE +0} (COMMON.cpy / RETHND.cpy): successful completion. */
    SUCCESS(0, "Successful completion"),

    /** COBOL {@code RC-WARNING VALUE +4} (COMMON.cpy / RETHND.cpy): completed with warnings. */
    WARNING(4, "Completed with warnings"),

    /** COBOL {@code RC-ERROR VALUE +8} (COMMON.cpy / RETHND.cpy): error encountered. */
    ERROR(8, "Error encountered"),

    /** COBOL {@code RC-SEVERE VALUE +12} (COMMON.cpy / RETHND.cpy): severe error. */
    SEVERE(12, "Severe error"),

    /** COBOL {@code RC-CRITICAL VALUE +16} (COMMON.cpy / RETHND.cpy): critical failure. */
    CRITICAL(16, "Critical failure");

    private final int code;
    private final String description;

    ReturnCode(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public boolean isSuccess() {
        return this == SUCCESS;
    }

    /**
     * Resolves a numeric return code to its enum constant.
     *
     * @throws IllegalArgumentException if the value is not a defined return code
     */
    public static ReturnCode fromCode(int code) {
        for (ReturnCode rc : values()) {
            if (rc.code == code) {
                return rc;
            }
        }
        throw new IllegalArgumentException("Unknown return code: " + code);
    }

    /**
     * Classifies an arbitrary numeric return code into a {@link ReturnStatus}.
     *
     * <p>COBOL origin: the {@code EVALUATE RC-NEW-CODE} ranges in paragraph
     * {@code P200-SET-RETURN-CODE} of {@code src/programs/batch/RTNCDE00.cbl}:
     * 0 = success, 1 thru 4 = warning, 5 thru 8 = error, other = severe.
     */
    public static ReturnStatus statusFor(int code) {
        if (code == 0) {
            return ReturnStatus.SUCCESS;
        }
        if (code >= 1 && code <= 4) {
            return ReturnStatus.WARNING;
        }
        if (code >= 5 && code <= 8) {
            return ReturnStatus.ERROR;
        }
        return ReturnStatus.SEVERE;
    }

    public ReturnStatus toStatus() {
        return statusFor(code);
    }
}
