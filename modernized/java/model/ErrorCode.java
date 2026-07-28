package com.clbs.portfolio.model;

/**
 * The error codes catalogued in {@code documentation/technical/data-dictionary.md} section 6.
 *
 * <p>No program in the translated slice populates {@code ERR-CODE}: {@code PORTTRAN} sets only
 * {@code ERR-TEXT}, {@code ERR-CATEGORY} and {@code ERR-PROGRAM} before calling {@code ERRPROC}, so
 * the field reaches the error log as spaces. These codes are carried in the model as the documented
 * catalogue that the test oracle asserts against, not as behaviour of the translated program.
 */
public enum ErrorCode {

    /** E001 - Invalid Account Number, error, reject. */
    INVALID_ACCOUNT("E001", "Invalid Account Number", ErrorSeverity.ERROR),
    /** E002 - Invalid Fund ID, error, reject. */
    INVALID_FUND("E002", "Invalid Fund ID", ErrorSeverity.ERROR),
    /** E003 - Invalid Transaction Type, error, reject. */
    INVALID_TRANSACTION_TYPE("E003", "Invalid Transaction Type", ErrorSeverity.ERROR),
    /** E004 - Insufficient Position Balance, error, reject. */
    INSUFFICIENT_BALANCE("E004", "Insufficient Position Balance", ErrorSeverity.ERROR),
    /** W001 - Zero Dollar Transaction, warning, process. */
    ZERO_DOLLAR_TRANSACTION("W001", "Zero Dollar Transaction", ErrorSeverity.WARNING),
    /** W002 - Duplicate Transaction ID, warning, log. */
    DUPLICATE_TRANSACTION("W002", "Duplicate Transaction ID", ErrorSeverity.WARNING);

    public static final int LENGTH = 4;

    private final String code;
    private final String description;
    private final ErrorSeverity severity;

    ErrorCode(String code, String description, ErrorSeverity severity) {
        this.code = code;
        this.description = description;
        this.severity = severity;
    }

    public String code() {
        return code;
    }

    public String description() {
        return description;
    }

    public ErrorSeverity severity() {
        return severity;
    }

    /** The matching code, or {@code null} when the field holds an uncatalogued value. */
    public static ErrorCode fromCode(String code) {
        String stored = CobolText.picX(code, LENGTH);
        for (ErrorCode errorCode : values()) {
            if (errorCode.code.equals(stored)) {
                return errorCode;
            }
        }
        return null;
    }
}
