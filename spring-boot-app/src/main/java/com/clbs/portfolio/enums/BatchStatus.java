package com.clbs.portfolio.enums;

public enum BatchStatus {
    READY("R"),
    ACTIVE("A"),
    WAITING("W"),
    DONE("D"),
    ERROR("E");

    private final String code;

    BatchStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static BatchStatus fromCode(String code) {
        for (BatchStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown batch status code: " + code);
    }
}
