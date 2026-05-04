package com.portfolio.util;

public enum ReturnCode {
    SUCCESS(0, "Success"),
    WARNING(4, "Warning"),
    ERROR(8, "Error"),
    SEVERE(12, "Severe Error"),
    CRITICAL(16, "Critical Error");

    private final int code;
    private final String description;

    ReturnCode(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }

    public static ReturnCode fromCode(int code) {
        for (ReturnCode rc : values()) {
            if (rc.code == code) return rc;
        }
        return ERROR;
    }

    public boolean isSuccess() { return this == SUCCESS; }
    public boolean isWarning() { return this == WARNING; }
    public boolean isError() { return this == ERROR || this == SEVERE || this == CRITICAL; }
}
