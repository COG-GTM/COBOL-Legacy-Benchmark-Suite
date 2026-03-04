package com.cobolbenchmark.model;

/**
 * Checkpoint Phase enum - from CKPRST.cpy level-88 conditions.
 * 88 CK-PHASE-INIT VALUE 'I'.
 * 88 CK-PHASE-READ VALUE 'R'.
 * 88 CK-PHASE-PROC VALUE 'P'.
 * 88 CK-PHASE-UPDT VALUE 'U'.
 * 88 CK-PHASE-TERM VALUE 'T'.
 */
public enum CheckpointPhase {
    INIT("I"),
    READ("R"),
    PROCESS("P"),
    UPDATE("U"),
    TERMINATE("T");

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
        throw new IllegalArgumentException("Unknown checkpoint phase code: " + code);
    }
}
