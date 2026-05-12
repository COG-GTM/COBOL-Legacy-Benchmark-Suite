package com.portfolio.model.enums;

public enum ErrorSeverity {
    INFO(1),
    WARNING(2),
    ERROR(3),
    SEVERE(4);

    private final int level;

    ErrorSeverity(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    public static ErrorSeverity fromLevel(int level) {
        for (ErrorSeverity sev : values()) {
            if (sev.level == level) {
                return sev;
            }
        }
        throw new IllegalArgumentException("Unknown ErrorSeverity level: " + level);
    }
}
