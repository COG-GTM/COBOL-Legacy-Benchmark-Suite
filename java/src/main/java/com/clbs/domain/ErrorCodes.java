package com.clbs.domain;

/** ERRHAND.cpy standard error codes and VSAM file statuses. */
public final class ErrorCodes {

    public static final String INVALID_DATA = "E001";
    public static final String NOT_FOUND = "E002";
    public static final String DUPLICATE = "E003";
    public static final String FILE_ERROR = "E004";
    public static final String DB_ERROR = "E005";
    public static final String SECURITY = "E006";
    public static final String PROCESSING = "E007";
    public static final String VALIDATION = "E008";
    public static final String VERSION = "E009";
    public static final String TIMEOUT = "E010";

    /** ERR-CATEGORY values. */
    public static final String CAT_VSAM = "VS";
    public static final String CAT_VALIDATION = "VL";
    public static final String CAT_PROCESSING = "PR";
    public static final String CAT_SYSTEM = "SY";

    private ErrorCodes() {
    }
}
