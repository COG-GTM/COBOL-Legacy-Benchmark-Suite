package com.benchmark.portfolio.common.compare;

/**
 * Data type of a fixed-width field, mirroring the COBOL PICTURE/USAGE
 * combinations used by the copybook layouts.
 */
public enum FieldType {
    /** PIC X(n) — character data, compared as trimmed String. */
    CHAR,
    /** PIC 9(n) / S9(n)V9(m) DISPLAY — zoned decimal, compared as BigDecimal. */
    ZONED,
    /** PIC S9(n)V9(m) COMP-3 — packed decimal, compared as BigDecimal. */
    PACKED
}
