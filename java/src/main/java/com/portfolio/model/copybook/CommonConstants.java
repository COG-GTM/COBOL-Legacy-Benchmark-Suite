package com.portfolio.model.copybook;

/**
 * Migrated from copybook {@code src/copybook/common/COMMON.cpy}
 * (RETURN-CODES, STATUS-CODES, TRANSACTION-TYPES, CURRENCY-CODES).
 */
public final class CommonConstants {

    private CommonConstants() {}

    // Return codes (PIC S9(4))
    public static final int RC_SUCCESS = 0;
    public static final int RC_WARNING = 4;
    public static final int RC_ERROR = 8;
    public static final int RC_SEVERE = 12;
    public static final int RC_CRITICAL = 16;

    // Status codes (PIC X(01))
    public static final String STATUS_ACTIVE = "A";
    public static final String STATUS_CLOSED = "C";
    public static final String STATUS_PENDING = "P";
    public static final String STATUS_SUSPENDED = "S";
    public static final String STATUS_FAILED = "F";
    public static final String STATUS_REVERSED = "R";

    // Transaction types (PIC X(02))
    public static final String TRN_TYPE_BUY = "BU";
    public static final String TRN_TYPE_SELL = "SL";
    public static final String TRN_TYPE_TRANSFER = "TR";
    public static final String TRN_TYPE_FEE = "FE";

    // Currency codes (PIC X(03))
    public static final String CURR_USD = "USD";
    public static final String CURR_EUR = "EUR";
    public static final String CURR_GBP = "GBP";
    public static final String CURR_JPY = "JPY";
    public static final String CURR_CAD = "CAD";
}
