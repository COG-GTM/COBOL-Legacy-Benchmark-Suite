package com.portfolio.domain.model;

/**
 * Maps COBOL 88-level values from PORTFLIO.cpy PORT-STATUS.
 */
public enum PortfolioStatus {
    ACTIVE('A'),
    CLOSED('C'),
    SUSPENDED('S');

    private final char code;

    PortfolioStatus(char code) {
        this.code = code;
    }

    public char getCode() {
        return code;
    }

    public static PortfolioStatus fromCode(char code) {
        for (PortfolioStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("Unknown portfolio status code: " + code);
    }
}
