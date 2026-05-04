package com.portfolio.util;

public final class CommonConstants {

    private CommonConstants() {}

    public static final String STATUS_ACTIVE = "A";
    public static final String STATUS_CLOSED = "C";
    public static final String STATUS_PENDING = "P";
    public static final String STATUS_SUSPENDED = "S";
    public static final String STATUS_FAILED = "F";
    public static final String STATUS_REVERSED = "R";

    public static final String TRN_TYPE_BUY = "BU";
    public static final String TRN_TYPE_SELL = "SL";
    public static final String TRN_TYPE_TRANSFER = "TR";
    public static final String TRN_TYPE_FEE = "FE";

    public static final String CURRENCY_USD = "USD";
    public static final String CURRENCY_EUR = "EUR";
    public static final String CURRENCY_GBP = "GBP";
    public static final String CURRENCY_JPY = "JPY";
    public static final String CURRENCY_CAD = "CAD";

    public static final String AUDIT_TYPE_TRANSACTION = "TRAN";
    public static final String AUDIT_TYPE_USER = "USER";
    public static final String AUDIT_TYPE_SYSTEM = "SYST";

    public static final String AUDIT_ACTION_CREATE = "CREATE";
    public static final String AUDIT_ACTION_UPDATE = "UPDATE";
    public static final String AUDIT_ACTION_DELETE = "DELETE";
    public static final String AUDIT_ACTION_INQUIRE = "INQUIRE";

    public static final String AUDIT_STATUS_SUCCESS = "SUCC";
    public static final String AUDIT_STATUS_FAILURE = "FAIL";
    public static final String AUDIT_STATUS_WARNING = "WARN";
}
