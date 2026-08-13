package com.ipms.domain;

/** HIST-RECORD-TYPE level-88 values from HISTREC.cpy (PT=PORTFOLIO, PS=POSITION, TR=TRANSACTION). */
public enum HistoryRecordType {
    PORTFOLIO("PT"),
    POSITION("PS"),
    TRANSACTION("TR");

    private final String code;

    HistoryRecordType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static HistoryRecordType fromCode(String code) {
        for (HistoryRecordType t : values()) {
            if (t.code.equals(code)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown history record type: " + code);
    }
}
