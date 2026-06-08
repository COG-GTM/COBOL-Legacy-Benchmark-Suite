package com.portfolio.domain.model;

/**
 * Maps COBOL 88-level values from HISTREC.cpy HIST-RECORD-TYPE.
 */
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
        for (HistoryRecordType t : values()) {
            if (t.code.equals(code)) return t;
        }
        throw new IllegalArgumentException("Unknown history record type: " + code);
    }
}
