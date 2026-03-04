package com.cobolbenchmark.model;

/**
 * Error Severity enum - from DBTBLS.cpy / ERRLOG level-88 conditions.
 * 88 EL-SEV-INFO   VALUE 'I'.
 * 88 EL-SEV-WARN   VALUE 'W'.
 * 88 EL-SEV-ERROR  VALUE 'E'.
 * 88 EL-SEV-SEVERE VALUE 'F'.
 */
public enum ErrorSeverity {
    INFO("I", 1),
    WARNING("W", 2),
    ERROR("E", 3),
    SEVERE("F", 4);

    private final String code;
    private final int level;

    ErrorSeverity(String code, int level) {
        this.code = code;
        this.level = level;
    }

    public String getCode() {
        return code;
    }

    public int getLevel() {
        return level;
    }

    public static ErrorSeverity fromCode(String code) {
        for (ErrorSeverity severity : values()) {
            if (severity.code.equals(code)) {
                return severity;
            }
        }
        throw new IllegalArgumentException("Unknown error severity code: " + code);
    }
}
