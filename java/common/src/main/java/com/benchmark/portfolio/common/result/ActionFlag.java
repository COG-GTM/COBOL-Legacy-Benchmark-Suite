package com.benchmark.portfolio.common.result;

/**
 * Action to take after an error has been recorded.
 *
 * <p>COBOL origin: the level-88 conditions on {@code ACTION-FLAG PIC X(1)} in
 * {@code src/copybook/common/RETHND.cpy}: {@code ACTION-CONTINUE 'C'},
 * {@code ACTION-ABORT 'A'}, {@code ACTION-RETRY 'R'}.
 */
public enum ActionFlag {

    /** COBOL {@code ACTION-CONTINUE VALUE 'C'} (RETHND.cpy). */
    CONTINUE('C', "Continue processing"),

    /** COBOL {@code ACTION-ABORT VALUE 'A'} (RETHND.cpy). */
    ABORT('A', "Abort processing"),

    /** COBOL {@code ACTION-RETRY VALUE 'R'} (RETHND.cpy). */
    RETRY('R', "Retry the operation");

    /** COBOL {@code MAX-RETRIES PIC 9(2) COMP VALUE 3} (RETHND.cpy). */
    public static final int MAX_RETRIES = 3;

    private final char flagChar;
    private final String description;

    ActionFlag(char flagChar, String description) {
        this.flagChar = flagChar;
        this.description = description;
    }

    public char getFlagChar() {
        return flagChar;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Resolves a COBOL action flag character to its enum constant.
     *
     * @throws IllegalArgumentException if the character is not a defined action flag
     */
    public static ActionFlag fromChar(char flagChar) {
        for (ActionFlag flag : values()) {
            if (flag.flagChar == flagChar) {
                return flag;
            }
        }
        throw new IllegalArgumentException("Unknown action flag character: " + flagChar);
    }
}
