package com.portfolio.portmstr.model.enums;

/**
 * Portfolio status codes.
 * Mapped from COBOL PORTFLIO.cpy PORT-STATUS level-88 conditions.
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
        for (PortfolioStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid portfolio status code: " + code);
    }
}
