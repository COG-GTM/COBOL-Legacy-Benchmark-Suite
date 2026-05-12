package com.portfolio.model.enums;

public enum CheckpointStatus {
    INITIAL('I'),
    ACTIVE('A'),
    COMPLETE('C'),
    FAILED('F'),
    RESTARTED('R');

    private final char code;

    CheckpointStatus(char code) {
        this.code = code;
    }

    public char getCode() {
        return code;
    }

    public static CheckpointStatus fromCode(char code) {
        for (CheckpointStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown CheckpointStatus code: " + code);
    }
}
