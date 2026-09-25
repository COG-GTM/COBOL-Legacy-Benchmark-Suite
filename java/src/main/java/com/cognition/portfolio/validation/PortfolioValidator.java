package com.cognition.portfolio.validation;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Behaviour preserving port of the COBOL subroutine
 * {@code src/programs/portfolio/PORTVALD.cbl}.
 *
 * <p>This is a port, not a redesign. Every rule below is the rule the COBOL
 * executes today, including the ones that are almost certainly not the rules
 * the comments in the COBOL describe. Two of them are latent defects that the
 * parity harness pins down rather than repairs:</p>
 * <ul>
 *   <li>{@link #validateId} can never succeed, because the four digit suffix
 *       is copied into a ten byte field and then class tested as a whole, so
 *       the six pad spaces always fail the numeric test;</li>
 *   <li>{@link #validateAmount} can never fail, because the range it checks
 *       is the full range of the field it checks.</li>
 * </ul>
 *
 * <p>Changing either one is a business decision, not a porting decision. See
 * docs/modernization/PORTVALD-migration.md.</p>
 *
 * <p>The class is stateless and therefore thread safe; the COBOL is not, and
 * would have needed {@code RECURSIVE} or a thread local copy of working
 * storage to be called concurrently.</p>
 */
public final class PortfolioValidator {

    /**
     * Entry point corresponding to
     * {@code PROCEDURE DIVISION USING LS-VALIDATION-REQUEST} and its
     * {@code EVALUATE TRUE} dispatch.
     *
     * @param validateType the byte in {@code LS-VALIDATE-TYPE}
     * @param inputValue   the caller's value for {@code LS-INPUT-VALUE};
     *                     it is padded or truncated to 50 bytes here, exactly
     *                     as the move into the linkage item would do
     */
    public ValidationResult validate(char validateType, String inputValue) {
        String field = CobolAlphanumeric.toFixedLength(inputValue, PortValConstants.INPUT_VALUE_LENGTH);
        Optional<ValidationType> type = ValidationType.fromCode(validateType);
        if (type.isEmpty()) {
            return new ValidationResult(
                    PortValConstants.VAL_INVALID_ID,
                    PortValConstants.ERR_UNKNOWN_VALIDATE_TYPE);
        }
        return switch (type.get()) {
            case PORTFOLIO_ID -> validateId(field);
            case ACCOUNT_NUMBER -> validateAccount(field);
            case INVESTMENT_TYPE -> validateInvestmentType(field);
            case AMOUNT -> validateAmount(field);
        };
    }

    /**
     * 1000-VALIDATE-ID. The comment in the COBOL says "must start with 'PORT'
     * and have 4 numeric digits". The code tests the prefix, then moves
     * {@code LS-INPUT-VALUE(5:4)} into {@code VAL-NUMERIC-CHECK PIC X(10)}
     * and class tests all ten bytes, six of which are pad spaces. The test
     * therefore always fails and the paragraph always returns
     * {@code VAL-INVALID-ID}.
     */
    ValidationResult validateId(String field) {
        String prefix = CobolAlphanumeric.refMod(field, 1, PortValConstants.VAL_ID_PREFIX.length());
        if (!prefix.equals(PortValConstants.VAL_ID_PREFIX)) {
            return new ValidationResult(PortValConstants.VAL_INVALID_ID, PortValConstants.VAL_ERR_ID);
        }

        String numericCheck = CobolAlphanumeric.toFixedLength(
                CobolAlphanumeric.refMod(field, 5, 4), PortValConstants.NUMERIC_CHECK_LENGTH);
        if (!CobolAlphanumeric.isNumeric(numericCheck)) {
            return new ValidationResult(PortValConstants.VAL_INVALID_ID, PortValConstants.VAL_ERR_ID);
        }

        return new ValidationResult(PortValConstants.VAL_SUCCESS, PortValConstants.SPACES);
    }

    /**
     * 2000-VALIDATE-ACCOUNT. The comment says "10 numeric digits"; the code
     * class tests the whole 50 byte field, so a ten digit account number
     * followed by forty pad spaces is rejected and only fifty digits are
     * accepted. Fifty zeros are rejected by the explicit
     * {@code = ZEROS} test.
     */
    ValidationResult validateAccount(String field) {
        if (!CobolAlphanumeric.isNumeric(field) || CobolAlphanumeric.equalsZeros(field)) {
            return new ValidationResult(PortValConstants.VAL_INVALID_ACCT, PortValConstants.VAL_ERR_ACCT);
        }
        return new ValidationResult(PortValConstants.VAL_SUCCESS, PortValConstants.SPACES);
    }

    /**
     * 3000-VALIDATE-TYPE. The 50 byte field is compared against three byte
     * literals, so COBOL pads the literals with spaces: the code must be in
     * the first three bytes and the remaining forty seven bytes must be
     * spaces. The comparison is byte exact, so lower case is rejected.
     */
    ValidationResult validateInvestmentType(String field) {
        boolean known = field.equals(CobolAlphanumeric.toFixedLength("STK", field.length()))
                || field.equals(CobolAlphanumeric.toFixedLength("BND", field.length()))
                || field.equals(CobolAlphanumeric.toFixedLength("MMF", field.length()))
                || field.equals(CobolAlphanumeric.toFixedLength("ETF", field.length()));
        if (!known) {
            return new ValidationResult(PortValConstants.VAL_INVALID_TYPE, PortValConstants.VAL_ERR_TYPE);
        }
        return new ValidationResult(PortValConstants.VAL_SUCCESS, PortValConstants.SPACES);
    }

    /**
     * 4000-VALIDATE-AMOUNT. The input is moved into
     * {@code VAL-TEMP-NUM PIC S9(13)V99} and then compared against
     * {@code VAL-MIN-AMOUNT} and {@code VAL-MAX-AMOUNT}, which are the
     * smallest and largest values that field can hold. The comparison can
     * therefore never be true: the paragraph always returns
     * {@code VAL-SUCCESS}, including for text that is not a number at all,
     * which the move silently turns into zero.
     */
    ValidationResult validateAmount(String field) {
        BigDecimal tempNum = amountAsMoved(field);
        if (tempNum.compareTo(PortValConstants.VAL_MIN_AMOUNT) < 0
                || tempNum.compareTo(PortValConstants.VAL_MAX_AMOUNT) > 0) {
            return new ValidationResult(PortValConstants.VAL_INVALID_AMT, PortValConstants.VAL_ERR_AMT);
        }
        return new ValidationResult(PortValConstants.VAL_SUCCESS, PortValConstants.SPACES);
    }

    /**
     * The value that {@code MOVE LS-INPUT-VALUE TO VAL-TEMP-NUM} produces.
     * PORTVALD never returns it, but it is the number any replacement rule
     * would be written against, so it is exposed and parity tested in its
     * own right.
     */
    public BigDecimal amountAsMoved(String inputValue) {
        String field = CobolAlphanumeric.toFixedLength(inputValue, PortValConstants.INPUT_VALUE_LENGTH);
        return CobolNumericMove.toSignedDisplay(
                field,
                PortValConstants.TEMP_NUM_INTEGER_DIGITS,
                PortValConstants.TEMP_NUM_FRACTION_DIGITS);
    }
}
