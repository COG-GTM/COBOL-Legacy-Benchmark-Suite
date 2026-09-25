package com.cognition.portfolio.validation;

import java.util.Optional;

/**
 * The level 88 condition names declared on {@code LS-VALIDATE-TYPE PIC X(1)}.
 * The comparison is on the exact byte, so lower case letters match nothing,
 * exactly as in the COBOL.
 */
public enum ValidationType {

    /** {@code 88 LS-VAL-ID VALUE 'I'} - 1000-VALIDATE-ID. */
    PORTFOLIO_ID('I'),

    /** {@code 88 LS-VAL-ACCT VALUE 'A'} - 2000-VALIDATE-ACCOUNT. */
    ACCOUNT_NUMBER('A'),

    /** {@code 88 LS-VAL-TYPE VALUE 'T'} - 3000-VALIDATE-TYPE. */
    INVESTMENT_TYPE('T'),

    /** {@code 88 LS-VAL-AMT VALUE 'M'} - 4000-VALIDATE-AMOUNT. */
    AMOUNT('M');

    private final char code;

    ValidationType(char code) {
        this.code = code;
    }

    public char code() {
        return code;
    }

    /**
     * @return the branch selected by {@code EVALUATE TRUE}, or empty for the
     *         {@code WHEN OTHER} branch
     */
    public static Optional<ValidationType> fromCode(char code) {
        for (ValidationType type : values()) {
            if (type.code == code) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
