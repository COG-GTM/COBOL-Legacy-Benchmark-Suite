package com.portfolio.model.enums;

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
        for (HistoryRecordType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown HistoryRecordType code: " + code);
    }
}
