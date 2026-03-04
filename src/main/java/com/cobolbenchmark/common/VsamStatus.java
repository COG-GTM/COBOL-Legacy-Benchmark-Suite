package com.cobolbenchmark.common;

/**
 * VSAM Status enum - from ERRHAND.cpy.
 * Converts VSAM file status codes to meaningful enum values.
 */
public enum VsamStatus {
    SUCCESS("00", "Successful completion"),
    DUPKEY("22", "Duplicate key"),
    NOTFND("23", "Record not found"),
    EOF("10", "End of file"),
    IOERR("37", "I/O error"),
    LOCKED("68", "Record locked"),
    NOSPACE("24", "No space available");

    private final String code;
    private final String description;

    VsamStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static VsamStatus fromCode(String code) {
        for (VsamStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown VSAM status code: " + code);
    }

    public boolean isSuccess() {
        return this == SUCCESS;
    }

    public boolean isError() {
        return this != SUCCESS && this != EOF;
    }
}
