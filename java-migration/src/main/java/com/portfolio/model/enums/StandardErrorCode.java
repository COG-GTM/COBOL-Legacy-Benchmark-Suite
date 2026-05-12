package com.portfolio.model.enums;

public enum StandardErrorCode {
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

    StandardErrorCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static StandardErrorCode fromCode(String code) {
        for (StandardErrorCode err : values()) {
            if (err.code.equals(code)) {
                return err;
            }
        }
        throw new IllegalArgumentException("Unknown StandardErrorCode: " + code);
    }
}
