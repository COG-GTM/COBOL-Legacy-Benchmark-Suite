package com.benchmark.portfolio.common.result;

/**
 * Single-character status classification of a return code.
 *
 * <p>COBOL origin: the level-88 conditions on {@code RC-STATUS PIC X} in
 * {@code src/copybook/common/RTNCODE.cpy}: {@code RC-STATUS-SUCCESS 'S'},
 * {@code RC-STATUS-WARNING 'W'}, {@code RC-STATUS-ERROR 'E'},
 * {@code RC-STATUS-SEVERE 'F'}.
 */
public enum ReturnStatus {

    /** COBOL {@code RC-STATUS-SUCCESS VALUE 'S'} (RTNCODE.cpy). */
    SUCCESS('S', "Success"),

    /** COBOL {@code RC-STATUS-WARNING VALUE 'W'} (RTNCODE.cpy). */
    WARNING('W', "Warning"),

    /** COBOL {@code RC-STATUS-ERROR VALUE 'E'} (RTNCODE.cpy). */
    ERROR('E', "Error"),

    /** COBOL {@code RC-STATUS-SEVERE VALUE 'F'} (RTNCODE.cpy). */
    SEVERE('F', "Severe failure");

    private final char statusChar;
    private final String description;

    ReturnStatus(char statusChar, String description) {
        this.statusChar = statusChar;
        this.description = description;
    }

    public char getStatusChar() {
        return statusChar;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Resolves a COBOL status character to its enum constant.
     *
     * @throws IllegalArgumentException if the character is not a defined status
     */
    public static ReturnStatus fromChar(char statusChar) {
        for (ReturnStatus status : values()) {
            if (status.statusChar == statusChar) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status character: " + statusChar);
    }
}
