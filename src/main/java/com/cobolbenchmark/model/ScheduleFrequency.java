package com.cobolbenchmark.model;

/**
 * Schedule Frequency enum - from PRCSEQ.cpy level-88 conditions.
 * 88 PSR-DAILY   VALUE 'D'.
 * 88 PSR-WEEKLY  VALUE 'W'.
 * 88 PSR-MONTHLY VALUE 'M'.
 */
public enum ScheduleFrequency {
    DAILY("D"),
    WEEKLY("W"),
    MONTHLY("M");

    private final String code;

    ScheduleFrequency(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static ScheduleFrequency fromCode(String code) {
        for (ScheduleFrequency freq : values()) {
            if (freq.code.equals(code)) {
                return freq;
            }
        }
        throw new IllegalArgumentException("Unknown schedule frequency code: " + code);
    }
}
