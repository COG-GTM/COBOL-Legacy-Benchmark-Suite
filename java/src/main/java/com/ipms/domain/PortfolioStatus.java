package com.ipms.domain;

/** PORT-STATUS level-88 values from PORTFLIO.cpy (A=ACTIVE, C=CLOSED, S=SUSPENDED). */
public enum PortfolioStatus {
    ACTIVE("A"),
    CLOSED("C"),
    SUSPENDED("S");

    private final String code;

    PortfolioStatus(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static PortfolioStatus fromCode(String code) {
        for (PortfolioStatus s : values()) {
            if (s.code.equals(code)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown portfolio status: " + code);
    }
}
