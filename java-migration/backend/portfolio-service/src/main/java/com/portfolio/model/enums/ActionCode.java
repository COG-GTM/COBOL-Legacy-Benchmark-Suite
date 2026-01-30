package com.portfolio.model.enums;

public enum ActionCode {
    ADD('A'),
    CHANGE('C'),
    DELETE('D');

    private final char code;

    ActionCode(char code) {
        this.code = code;
    }

    public char getCode() {
        return code;
    }

    public static ActionCode fromCode(char code) {
        for (ActionCode action : values()) {
            if (action.code == code) {
                return action;
            }
        }
        throw new IllegalArgumentException("Unknown action code: " + code);
    }
}
