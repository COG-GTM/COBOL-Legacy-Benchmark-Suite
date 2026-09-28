package com.cognition.portfolio.validation;

import static com.cognition.portfolio.validation.PortvalConstants.ERR_INVALID_VALIDATION_TYPE;
import static com.cognition.portfolio.validation.PortvalConstants.FIELD_LENGTH;
import static com.cognition.portfolio.validation.PortvalConstants.NO_ERROR;
import static com.cognition.portfolio.validation.PortvalConstants.NUMERIC_CHECK_LENGTH;
import static com.cognition.portfolio.validation.PortvalConstants.TEMP_NUM_INTEGER_DIGITS;
import static com.cognition.portfolio.validation.PortvalConstants.TEMP_NUM_SCALE;
import static com.cognition.portfolio.validation.PortvalConstants.VAL_ERR_ACCT;
import static com.cognition.portfolio.validation.PortvalConstants.VAL_ERR_AMT;
import static com.cognition.portfolio.validation.PortvalConstants.VAL_ERR_ID;
import static com.cognition.portfolio.validation.PortvalConstants.VAL_ERR_TYPE;
import static com.cognition.portfolio.validation.PortvalConstants.VAL_ID_PREFIX;
import static com.cognition.portfolio.validation.PortvalConstants.VAL_INVALID_ACCT;
import static com.cognition.portfolio.validation.PortvalConstants.VAL_INVALID_AMT;
import static com.cognition.portfolio.validation.PortvalConstants.VAL_INVALID_ID;
import static com.cognition.portfolio.validation.PortvalConstants.VAL_INVALID_TYPE;
import static com.cognition.portfolio.validation.PortvalConstants.VAL_MAX_AMOUNT;
import static com.cognition.portfolio.validation.PortvalConstants.VAL_MIN_AMOUNT;
import static com.cognition.portfolio.validation.PortvalConstants.VAL_SUCCESS;

import java.math.BigDecimal;
import java.util.Arrays;

/**
 * Java port of COBOL subroutine {@code PORTVALD} (src/programs/portfolio/PORTVALD.cbl).
 *
 * <p>Each private method corresponds to one PORTVALD paragraph and reproduces its executed
 * behaviour, including behaviour that contradicts the paragraph's own comment (see
 * docs/modernization/PORTVALD-migration.md, "Observed behaviour vs. stated intent"). Rules are
 * deliberately not corrected here.
 *
 * <p>Stateless and thread-safe: PORTVALD re-initialises its work areas on every call.
 */
public final class PortfolioValidator {

    /** {@code 88 LS-VAL-ID VALUE 'I'}. */
    public static final char VALIDATE_PORTFOLIO_ID = 'I';
    /** {@code 88 LS-VAL-ACCT VALUE 'A'}. */
    public static final char VALIDATE_ACCOUNT_NUMBER = 'A';
    /** {@code 88 LS-VAL-TYPE VALUE 'T'}. */
    public static final char VALIDATE_INVESTMENT_TYPE = 'T';
    /** {@code 88 LS-VAL-AMT VALUE 'M'}. */
    public static final char VALIDATE_AMOUNT = 'M';

    private static final String[] VALID_INVESTMENT_TYPES = {"STK", "BND", "MMF", "ETF"};

    /**
     * Equivalent of {@code CALL 'PORTVALD' USING LS-VALIDATION-REQUEST}.
     *
     * @param validateType {@code LS-VALIDATE-TYPE PIC X(1)}; case-sensitive
     * @param inputValue   {@code LS-INPUT-VALUE PIC X(50)}; at most 50 single-byte characters,
     *                     right-padded with spaces to 50 as a COBOL caller's MOVE would
     * @return {@code LS-RETURN-CODE} and {@code LS-ERROR-MSG} as PORTVALD leaves them
     * @throws IllegalArgumentException if {@code validateType} or {@code inputValue} cannot be
     *                                  represented in the COBOL fields (see {@link AlphanumericField})
     */
    public ValidationResult validate(char validateType, String inputValue) {
        if (validateType > 0xFF) {
            throw new IllegalArgumentException(
                    String.format("validateType U+%04X is not a single-byte character", (int) validateType));
        }
        byte[] input = AlphanumericField.of(inputValue, FIELD_LENGTH);

        switch (validateType) {
            case VALIDATE_PORTFOLIO_ID:
                return validateId(input);
            case VALIDATE_ACCOUNT_NUMBER:
                return validateAccount(input);
            case VALIDATE_INVESTMENT_TYPE:
                return validateType(input);
            case VALIDATE_AMOUNT:
                return validateAmount(input);
            default:
                return ValidationResult.of(VAL_INVALID_ID, ERR_INVALID_VALIDATION_TYPE);
        }
    }

    /** 1000-VALIDATE-ID. */
    private static ValidationResult validateId(byte[] input) {
        if (!Arrays.equals(AlphanumericField.substring(input, 1, 4),
                AlphanumericField.of(VAL_ID_PREFIX, VAL_ID_PREFIX.length()))) {
            return ValidationResult.of(VAL_INVALID_ID, VAL_ERR_ID);
        }

        // MOVE LS-INPUT-VALUE(5:4) TO VAL-NUMERIC-CHECK (PIC X(10)): 4 bytes + 6 spaces of padding,
        // so the NUMERIC class test below can never be true.
        byte[] numericCheck = AlphanumericField.of(
                AlphanumericField.toText(AlphanumericField.substring(input, 5, 4)), NUMERIC_CHECK_LENGTH);
        if (!AlphanumericField.isNumeric(numericCheck)) {
            return ValidationResult.of(VAL_INVALID_ID, VAL_ERR_ID);
        }

        return ValidationResult.of(VAL_SUCCESS, NO_ERROR);
    }

    /** 2000-VALIDATE-ACCOUNT. */
    private static ValidationResult validateAccount(byte[] input) {
        if (!AlphanumericField.isNumeric(input) || AlphanumericField.isAllZeros(input)) {
            return ValidationResult.of(VAL_INVALID_ACCT, VAL_ERR_ACCT);
        }
        return ValidationResult.of(VAL_SUCCESS, NO_ERROR);
    }

    /** 3000-VALIDATE-TYPE. */
    private static ValidationResult validateType(byte[] input) {
        for (String validType : VALID_INVESTMENT_TYPES) {
            if (AlphanumericField.equalsLiteral(input, validType)) {
                return ValidationResult.of(VAL_SUCCESS, NO_ERROR);
            }
        }
        return ValidationResult.of(VAL_INVALID_TYPE, VAL_ERR_TYPE);
    }

    /** 4000-VALIDATE-AMOUNT. */
    private static ValidationResult validateAmount(byte[] input) {
        BigDecimal tempNum = DisplayNumericMove.alphanumericToSignedDisplay(
                input, TEMP_NUM_INTEGER_DIGITS, TEMP_NUM_SCALE);

        if (tempNum.compareTo(VAL_MIN_AMOUNT) < 0 || tempNum.compareTo(VAL_MAX_AMOUNT) > 0) {
            return ValidationResult.of(VAL_INVALID_AMT, VAL_ERR_AMT);
        }
        return ValidationResult.of(VAL_SUCCESS, NO_ERROR);
    }
}
