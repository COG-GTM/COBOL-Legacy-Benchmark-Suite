package com.cog.clbs.file;

/**
 * COBOL file status codes.
 *
 * <p>Mirrors the two-character FILE STATUS fields and their 88-levels in
 * {@code src/templates/program/file-handling.cbl}:
 * '00' success, '10' end of file, '22' duplicate key, '23' record not found.
 */
public enum FileStatus {
    SUCCESS("00"),
    END_OF_FILE("10"),
    DUPLICATE_KEY("22"),
    RECORD_NOT_FOUND("23"),
    FILE_NOT_OPEN("47"),
    IO_ERROR("30");

    private final String code;

    FileStatus(String code) {
        this.code = code;
    }

    /** The two-character COBOL file status code. */
    public String getCode() {
        return code;
    }

    public boolean isSuccess() {
        return this == SUCCESS;
    }
}
