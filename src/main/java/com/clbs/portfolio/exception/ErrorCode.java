package com.clbs.portfolio.exception;

/** STD-ERROR-CODES from RETHND.cpy. */
public enum ErrorCode {
    INVALID_DATA("E001"),
    NOT_FOUND("E002"),
    DUPLICATE("E003"),
    FILE_ERROR("E004"),
    DB_ERROR("E005"),
    SECURITY("E006"),
    PROCESSING("E007"),
    VALIDATION("E008"),
    VERSION("E009"),
    TIMEOUT("E010");

    private final String code;

    ErrorCode(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
