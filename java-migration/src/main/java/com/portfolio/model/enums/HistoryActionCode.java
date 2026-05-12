package com.portfolio.model.enums;

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
        for (HistoryActionCode action : values()) {
            if (action.code == code) {
                return action;
            }
        }
        throw new IllegalArgumentException("Unknown HistoryActionCode: " + code);
    }
}
