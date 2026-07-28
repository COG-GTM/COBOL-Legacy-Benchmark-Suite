package com.clbs.portfolio.model;

/** {@code 01 ERR-CATEGORIES} in {@code src/copybook/common/ERRHAND.cpy}. */
public enum ErrorCategory {

    /** {@code ERR-CAT-VSAM VALUE 'VS'}. */
    VSAM("VS"),
    /** {@code ERR-CAT-VALID VALUE 'VL'}. */
    VALIDATION("VL"),
    /** {@code ERR-CAT-PROC VALUE 'PR'} - the category {@code PORTTRAN} logs every error under. */
    PROCESSING("PR"),
    /** {@code ERR-CAT-SYSTEM VALUE 'SY'}. */
    SYSTEM("SY");

    public static final int LENGTH = 2;

    private final String code;

    ErrorCategory(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    /** The matching category, or {@code null} when the buffer holds an uncovered value. */
    public static ErrorCategory fromCode(String code) {
        String stored = CobolText.picX(code, LENGTH);
        for (ErrorCategory category : values()) {
            if (category.code.equals(stored)) {
                return category;
            }
        }
        return null;
    }
}
