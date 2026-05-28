package com.clbs.portfolio.common;

import lombok.Getter;

/**
 * Checkpoint processing phases from COBOL CKPRST.cpy (CK-PHASE level 88s).
 */
@Getter
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

    public static CheckpointPhase fromCode(String code) {
        for (CheckpointPhase p : values()) {
            if (p.code.equals(code)) {
                return p;
            }
        }
        throw new IllegalArgumentException("Unknown checkpoint phase code: " + code);
    }
}
