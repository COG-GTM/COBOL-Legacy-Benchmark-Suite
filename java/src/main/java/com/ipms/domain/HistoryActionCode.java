package com.ipms.domain;

/** HIST-ACTION-CODE level-88 values from HISTREC.cpy (A=ADD, C=CHANGE, D=DELETE). */
public enum HistoryActionCode {
    ADD("A"),
    CHANGE("C"),
    DELETE("D");

    private final String code;

    HistoryActionCode(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static HistoryActionCode fromCode(String code) {
        for (HistoryActionCode a : values()) {
            if (a.code.equals(code)) {
                return a;
            }
        }
        throw new IllegalArgumentException("Unknown history action code: " + code);
    }
}
