package com.benchmark.portfolio.common.result;

/**
 * Category of an error, recorded alongside the error code.
 *
 * <p>COBOL origin: the level-88 conditions on {@code ERROR-TYPE PIC X(1)} in
 * {@code src/copybook/common/RETHND.cpy}: {@code ERR-VALIDATION 'V'},
 * {@code ERR-PROCESSING 'P'}, {@code ERR-DATABASE 'D'}, {@code ERR-FILE 'F'},
 * {@code ERR-SECURITY 'S'}.
 */
public enum ErrorType {

    /** COBOL {@code ERR-VALIDATION VALUE 'V'} (RETHND.cpy). */
    VALIDATION('V', "Validation error"),

    /** COBOL {@code ERR-PROCESSING VALUE 'P'} (RETHND.cpy). */
    PROCESSING('P', "Processing error"),

    /** COBOL {@code ERR-DATABASE VALUE 'D'} (RETHND.cpy). */
    DATABASE('D', "Database error"),

    /** COBOL {@code ERR-FILE VALUE 'F'} (RETHND.cpy). */
    FILE('F', "File error"),

    /** COBOL {@code ERR-SECURITY VALUE 'S'} (RETHND.cpy). */
    SECURITY('S', "Security error");

    private final char typeChar;
    private final String description;

    ErrorType(char typeChar, String description) {
        this.typeChar = typeChar;
        this.description = description;
    }

    public char getTypeChar() {
        return typeChar;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Resolves a COBOL error type character to its enum constant.
     *
     * @throws IllegalArgumentException if the character is not a defined error type
     */
    public static ErrorType fromChar(char typeChar) {
        for (ErrorType type : values()) {
            if (type.typeChar == typeChar) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown error type character: " + typeChar);
    }
}
