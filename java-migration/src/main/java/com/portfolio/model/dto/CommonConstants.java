package com.portfolio.model.dto;

public final class CommonConstants {

    private CommonConstants() {
    }

    // Return codes
    public static final int RC_SUCCESS = 0;
    public static final int RC_WARNING = 4;
    public static final int RC_ERROR = 8;
    public static final int RC_SEVERE = 12;
    public static final int RC_CRITICAL = 16;

    // Status codes
    public static final char STATUS_ACTIVE = 'A';
    public static final char STATUS_CLOSED = 'C';
    public static final char STATUS_PENDING = 'P';
    public static final char STATUS_SUSPENDED = 'S';
    public static final char STATUS_FAILED = 'F';
    public static final char STATUS_REVERSED = 'R';

    // Transaction types
    public static final String TRN_TYPE_BUY = "BU";
    public static final String TRN_TYPE_SELL = "SL";
    public static final String TRN_TYPE_TRANSFER = "TR";
    public static final String TRN_TYPE_FEE = "FE";

    // Currency codes
    public static final String CURR_USD = "USD";
    public static final String CURR_EUR = "EUR";
    public static final String CURR_GBP = "GBP";
    public static final String CURR_JPY = "JPY";
    public static final String CURR_CAD = "CAD";
}
