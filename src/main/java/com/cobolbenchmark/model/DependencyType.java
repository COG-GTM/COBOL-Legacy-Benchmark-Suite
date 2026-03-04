package com.cobolbenchmark.model;

/**
 * Dependency Type enum - from PRCSEQ.cpy level-88 conditions.
 * 88 PSR-DEP-HARD VALUE 'H'.
 * 88 PSR-DEP-SOFT VALUE 'S'.
 */
public enum DependencyType {
    HARD("H"),
    SOFT("S");

    private final String code;

    DependencyType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static DependencyType fromCode(String code) {
        for (DependencyType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown dependency type code: " + code);
    }
}
