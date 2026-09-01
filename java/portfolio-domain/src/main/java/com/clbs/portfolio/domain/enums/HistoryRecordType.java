package com.clbs.portfolio.domain.enums;

public enum HistoryRecordType {
    PORTFOLIO("PT"),
    POSITION("PS"),
    TRANSACTION("TR");

    private final String code;

    HistoryRecordType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static HistoryRecordType fromCode(String code) {
        for (HistoryRecordType value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown history record type code: " + code);
    }
}
