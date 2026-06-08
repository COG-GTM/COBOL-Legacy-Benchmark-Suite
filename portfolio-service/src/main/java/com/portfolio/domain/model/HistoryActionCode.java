package com.portfolio.domain.model;

/**
 * Maps COBOL 88-level values from HISTREC.cpy HIST-ACTION-CODE.
 */
public enum HistoryActionCode {
    ADD('A'),
    CHANGE('C'),
    DELETE('D');

    private final char code;

    HistoryActionCode(char code) {
        this.code = code;
    }

    public char getCode() {
        return code;
    }

    public static HistoryActionCode fromCode(char code) {
        for (HistoryActionCode a : values()) {
            if (a.code == code) return a;
        }
        throw new IllegalArgumentException("Unknown history action code: " + code);
    }
}
