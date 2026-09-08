package com.clbs.domain;

/**
 * TRNREC.cpy / COMMON.cpy TRN-TYPE 88-levels.
 *
 * <p>The COBOL source uses two-character codes. {@code documentation/operations/test-data-specs.md}
 * documents single-character 'B'/'S' codes for the flat test files; {@link #fromCode(String)}
 * accepts the two-character source form only, while {@link #fromTestDataCode(char)} maps the
 * test-data form so both repository conventions can be loaded without changing behaviour.
 */
public enum TransactionType {
    BUY("BU"),
    SELL("SL"),
    TRANSFER("TR"),
    FEE("FE");

    private final String code;

    TransactionType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static TransactionType fromCode(String code) {
        for (TransactionType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }

    public static TransactionType fromTestDataCode(char code) {
        return switch (code) {
            case 'B' -> BUY;
            case 'S' -> SELL;
            case 'T' -> TRANSFER;
            case 'F' -> FEE;
            default -> null;
        };
    }
}
