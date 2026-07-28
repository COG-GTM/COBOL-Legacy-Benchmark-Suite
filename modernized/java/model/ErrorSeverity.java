package com.clbs.portfolio.model;

/**
 * {@code 01 ERR-RETURN-CODES} in {@code src/copybook/common/ERRHAND.cpy}, matching the batch return
 * codes documented in {@code documentation/technical/data-dictionary.md} section 8.2.
 *
 * <p>{@code ERR-SEVERITY} is a {@code PIC S9(4) COMP} field, so {@link ErrorMessage} stores the raw
 * numeric value; this enum names the five values the copybook defines.
 */
public enum ErrorSeverity {

    /** {@code ERR-SUCCESS VALUE +0} - successful completion. */
    SUCCESS(0),
    /** {@code ERR-WARNING VALUE +4} - warning, processing complete. */
    WARNING(4),
    /** {@code ERR-ERROR VALUE +8} - errors, processing complete. */
    ERROR(8),
    /** {@code ERR-SEVERE VALUE +12} - critical error, abend. */
    SEVERE(12),
    /** {@code ERR-TERMINAL VALUE +16} - environment error. */
    TERMINAL(16);

    private final int value;

    ErrorSeverity(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    /** The matching severity, or {@code null} when the field holds an undocumented value. */
    public static ErrorSeverity fromValue(int value) {
        for (ErrorSeverity severity : values()) {
            if (severity.value == value) {
                return severity;
            }
        }
        return null;
    }
}
