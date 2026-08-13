package com.ipms.domain;

/**
 * Standard return-code, status, transaction-type, currency and error constants from
 * {@code src/copybook/common/COMMON.cpy}, {@code RETHND.cpy} (STD-ERROR-CODES) and
 * {@code ERRHAND.cpy} (ERR-RETURN-CODES / ERR-VSAM-STATUSES).
 */
public final class ReturnCodes {

    private ReturnCodes() {
    }

    // RETURN-CODES (COMMON.cpy) / ERR-RETURN-CODES (ERRHAND.cpy)
    public static final int RC_SUCCESS = 0;
    public static final int RC_WARNING = 4;
    public static final int RC_ERROR = 8;
    public static final int RC_SEVERE = 12;
    public static final int RC_CRITICAL = 16; // aka ERR-TERMINAL

    // STATUS-CODES (COMMON.cpy)
    public static final String STATUS_ACTIVE = "A";
    public static final String STATUS_CLOSED = "C";
    public static final String STATUS_PENDING = "P";
    public static final String STATUS_SUSPENDED = "S";
    public static final String STATUS_FAILED = "F";
    public static final String STATUS_REVERSED = "R";

    // CURRENCY-CODES (COMMON.cpy)
    public static final String CURR_USD = "USD";
    public static final String CURR_EUR = "EUR";
    public static final String CURR_GBP = "GBP";
    public static final String CURR_JPY = "JPY";
    public static final String CURR_CAD = "CAD";

    // STD-ERROR-CODES (RETHND.cpy)
    public static final String ERR_INVALID_DATA = "E001";
    public static final String ERR_NOT_FOUND = "E002";
    public static final String ERR_DUPLICATE = "E003";
    public static final String ERR_FILE_ERROR = "E004";
    public static final String ERR_DB_ERROR = "E005";
    public static final String ERR_SECURITY = "E006";
    public static final String ERR_PROCESSING = "E007";
    public static final String ERR_VALIDATION = "E008";
    public static final String ERR_VERSION = "E009";
    public static final String ERR_TIMEOUT = "E010";

    // ERR-VSAM-STATUSES (ERRHAND.cpy)
    public static final String VSAM_SUCCESS = "00";
    public static final String VSAM_EOF = "10";
    public static final String VSAM_DUPKEY = "22";
    public static final String VSAM_NOTFND = "23";
}
