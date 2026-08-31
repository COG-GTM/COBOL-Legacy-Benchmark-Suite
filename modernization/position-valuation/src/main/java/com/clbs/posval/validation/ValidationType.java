package com.clbs.posval.validation;

import java.util.Optional;

/**
 * {@code LS-VALIDATE-TYPE PIC X(1)} of {@code PORTVALD}'s linkage section and its 88-levels:
 * {@code LS-VAL-ID 'I'}, {@code LS-VAL-ACCT 'A'}, {@code LS-VAL-TYPE 'T'}, {@code LS-VAL-AMT 'M'}.
 */
public enum ValidationType {
    PORTFOLIO_ID('I'),
    ACCOUNT_NUMBER('A'),
    INVESTMENT_TYPE('T'),
    AMOUNT('M');

    private final char code;

    ValidationType(char code) {
        this.code = code;
    }

    public char code() {
        return code;
    }

    /** Empty for any character outside the four 88-levels, which {@code PORTVALD} rejects. */
    public static Optional<ValidationType> fromCode(char code) {
        for (ValidationType type : values()) {
            if (type.code == code) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
