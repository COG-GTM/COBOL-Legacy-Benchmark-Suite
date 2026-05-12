package com.portfolio.model.enums;

public enum CheckpointPhase {
    INIT("00"),
    READ("10"),
    PROCESS("20"),
    UPDATE("30"),
    TERMINATE("40");

    private final String code;

    CheckpointPhase(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static CheckpointPhase fromCode(String code) {
        for (CheckpointPhase phase : values()) {
            if (phase.code.equals(code)) {
                return phase;
            }
        }
        throw new IllegalArgumentException("Unknown CheckpointPhase code: " + code);
    }
}
