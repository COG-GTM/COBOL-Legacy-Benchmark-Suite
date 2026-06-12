package com.benchmark.portfolio.common.result;

/**
 * Standard four-character error codes used by the error-handling framework.
 *
 * <p>COBOL origin: the {@code STD-ERROR-CODES} group
 * ({@code PIC X(4)} constants) in {@code src/copybook/common/RETHND.cpy}.
 */
public enum ErrorCode {

    /** COBOL {@code ERR-INVALID-DATA VALUE 'E001'} (RETHND.cpy). */
    INVALID_DATA("E001", "Invalid data"),

    /** COBOL {@code ERR-NOT-FOUND VALUE 'E002'} (RETHND.cpy). */
    NOT_FOUND("E002", "Record not found"),

    /** COBOL {@code ERR-DUPLICATE VALUE 'E003'} (RETHND.cpy). */
    DUPLICATE("E003", "Duplicate record"),

    /** COBOL {@code ERR-FILE-ERROR VALUE 'E004'} (RETHND.cpy). */
    FILE_ERROR("E004", "File error"),

    /** COBOL {@code ERR-DB-ERROR VALUE 'E005'} (RETHND.cpy). */
    DB_ERROR("E005", "Database error"),

    /** COBOL {@code ERR-SECURITY VALUE 'E006'} (RETHND.cpy). */
    SECURITY("E006", "Security violation"),

    /** COBOL {@code ERR-PROCESSING VALUE 'E007'} (RETHND.cpy). */
    PROCESSING("E007", "Processing error"),

    /** COBOL {@code ERR-VALIDATION VALUE 'E008'} (RETHND.cpy). */
    VALIDATION("E008", "Validation error"),

    /** COBOL {@code ERR-VERSION VALUE 'E009'} (RETHND.cpy). */
    VERSION("E009", "Version mismatch"),

    /** COBOL {@code ERR-TIMEOUT VALUE 'E010'} (RETHND.cpy). */
    TIMEOUT("E010", "Timeout");

    private final String code;
    private final String description;

    ErrorCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Resolves a COBOL error code string (e.g. {@code "E001"}) to its enum constant.
     *
     * @throws IllegalArgumentException if the code is not a defined error code
     */
    public static ErrorCode fromCode(String code) {
        for (ErrorCode ec : values()) {
            if (ec.code.equals(code)) {
                return ec;
            }
        }
        throw new IllegalArgumentException("Unknown error code: " + code);
    }
}
