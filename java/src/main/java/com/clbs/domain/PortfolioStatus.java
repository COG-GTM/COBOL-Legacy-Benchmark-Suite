package com.clbs.domain;

/** PORTFLIO.cpy PORT-STATUS 88-levels (A/C/S). */
public enum PortfolioStatus {
    ACTIVE('A'),
    CLOSED('C'),
    SUSPENDED('S');

    private final char code;

    PortfolioStatus(char code) {
        this.code = code;
    }

    public char code() {
        return code;
    }

    public static PortfolioStatus fromCode(char code) {
        for (PortfolioStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }

    public static boolean isValid(char code) {
        return fromCode(code) != null;
    }
}
