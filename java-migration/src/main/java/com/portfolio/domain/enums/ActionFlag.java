package com.portfolio.domain.enums;

/**
 * Action flags from COBOL RETHND.cpy level-88 conditions.
 * ACTION-CONTINUE 'C', ACTION-ABORT 'A', ACTION-RETRY 'R'.
 */
public enum ActionFlag {
    CONTINUE('C'),
    ABORT('A'),
    RETRY('R');

    private final char code;

    ActionFlag(char code) {
        this.code = code;
    }

    public char getCode() {
        return code;
    }

    public static ActionFlag fromCode(char code) {
        for (ActionFlag af : values()) {
            if (af.code == code) {
                return af;
            }
        }
        throw new IllegalArgumentException("Unknown action flag code: " + code);
    }
}
