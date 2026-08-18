package com.cog.clbs.error;

/**
 * Error recovery actions.
 *
 * <p>Mirrors the ERR-ACTION 88-levels in {@code src/copybook/online/ERRHND.cpy}:
 * 'R' return to caller, 'C' continue processing, 'A' abend.
 */
public enum ErrorAction {
    RETURN('R'),
    CONTINUE('C'),
    ABEND('A');

    private final char code;

    ErrorAction(char code) {
        this.code = code;
    }

    public char getCode() {
        return code;
    }

    public static ErrorAction fromCode(char code) {
        for (ErrorAction a : values()) {
            if (a.code == code) {
                return a;
            }
        }
        throw new IllegalArgumentException("Unknown action code: " + code);
    }
}
