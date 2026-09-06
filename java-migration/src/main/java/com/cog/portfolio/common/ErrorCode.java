package com.cog.portfolio.common;

/** RETHND.cpy STD-ERROR-CODES. */
public enum ErrorCode implements CodedValue {
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

    @Override
    public String code() {
        return code;
    }
}
