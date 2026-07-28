package com.clbs.portfolio.model;

/**
 * Level-88 conditions on {@code PORT-STATUS PIC X(1)} in {@code PORTFLIO.cpy}.
 *
 * <p>Note that {@code documentation/operations/test-data-specs.md} section 3.3 documents the status
 * domain as {@code A}/{@code I}/{@code C} ("Inactive" instead of "Suspended"). The copybook is the
 * authority for the translated model; the documentation discrepancy is recorded in
 * {@code TRANSLATION-NOTES.md}.
 */
public enum PortfolioStatus {

    /** {@code 88 PORT-ACTIVE VALUE 'A'}. */
    ACTIVE("A"),
    /** {@code 88 PORT-CLOSED VALUE 'C'}. */
    CLOSED("C"),
    /** {@code 88 PORT-SUSPENDED VALUE 'S'}. */
    SUSPENDED("S");

    public static final int LENGTH = 1;

    private final String code;

    PortfolioStatus(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    /** The matching status, or {@code null} when the buffer holds an uncovered value. */
    public static PortfolioStatus fromCode(String code) {
        String stored = CobolText.picX(code, LENGTH);
        for (PortfolioStatus status : values()) {
            if (status.code.equals(stored)) {
                return status;
            }
        }
        return null;
    }
}
