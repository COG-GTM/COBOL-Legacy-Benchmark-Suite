package com.portfolio.domain.enums;

/**
 * Portfolio status from COBOL PORTFLIO.cpy level-88 conditions.
 * PORT-ACTIVE VALUE 'A', PORT-CLOSED VALUE 'C', PORT-SUSPENDED VALUE 'S'.
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
        for (PortfolioStatus ps : values()) {
            if (ps.code == code) {
                return ps;
            }
        }
        throw new IllegalArgumentException("Unknown portfolio status code: " + code);
    }
}
