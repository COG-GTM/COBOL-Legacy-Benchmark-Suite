package com.clbs.portfolio.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;

import static com.clbs.portfolio.service.CobolText.isNumeric;
import static com.clbs.portfolio.service.CobolText.pic;

/**
 * Port of the PORTVALD subroutine (PORTVAL.cpy rules). The input is handled as the 50-byte LS-INPUT-VALUE field,
 * so results match the COBOL program byte for byte (see parity/baseline/08-portvald.txt).
 */
@Component
public class PortfolioValidator {

    public static final char VAL_ID = 'I';
    public static final char VAL_ACCT = 'A';
    public static final char VAL_TYPE = 'T';
    public static final char VAL_AMT = 'M';

    static final String VAL_ERR_ID = "Invalid Portfolio ID format";
    static final String VAL_ERR_ACCT = "Invalid Account Number format";
    static final String VAL_ERR_TYPE = "Invalid Investment Type";
    static final String VAL_ERR_AMT = "Amount outside valid range";
    static final String VAL_ERR_REQUEST = "Invalid validation type";

    private static final int INPUT_LENGTH = 50;
    private static final String VAL_ID_PREFIX = "PORT";
    private static final Set<String> INVESTMENT_TYPES = Set.of("STK", "BND", "MMF", "ETF");
    private static final BigDecimal VAL_MIN_AMOUNT = new BigDecimal("-9999999999999.99");
    private static final BigDecimal VAL_MAX_AMOUNT = new BigDecimal("9999999999999.99");

    public ValidationResult validate(char validateType, String inputValue) {
        String input = pic(inputValue, INPUT_LENGTH);
        return switch (validateType) {
            case VAL_ID -> validateId(input);
            case VAL_ACCT -> validateAccount(input);
            case VAL_TYPE -> validateType(input);
            case VAL_AMT -> validateAmount(input);
            default -> new ValidationResult(ValidationResult.VAL_INVALID_ID, VAL_ERR_REQUEST);
        };
    }

    /**
     * 1000-VALIDATE-ID. The four digits are moved into the 10-byte VAL-NUMERIC-CHECK before the NUMERIC test, so
     * the six trailing spaces make the test fail for every input; this is reproduced deliberately.
     */
    private ValidationResult validateId(String input) {
        if (!input.startsWith(VAL_ID_PREFIX)) {
            return new ValidationResult(ValidationResult.VAL_INVALID_ID, VAL_ERR_ID);
        }
        String numericCheck = pic(input.substring(4, 8), 10);
        if (!isNumeric(numericCheck)) {
            return new ValidationResult(ValidationResult.VAL_INVALID_ID, VAL_ERR_ID);
        }
        return success();
    }

    /** 2000-VALIDATE-ACCOUNT: the whole 50-byte field must be numeric and not all zeros. */
    private ValidationResult validateAccount(String input) {
        if (!isNumeric(input) || input.chars().allMatch(c -> c == '0')) {
            return new ValidationResult(ValidationResult.VAL_INVALID_ACCT, VAL_ERR_ACCT);
        }
        return success();
    }

    /** 3000-VALIDATE-TYPE. */
    private ValidationResult validateType(String input) {
        if (!INVESTMENT_TYPES.contains(input.stripTrailing())) {
            return new ValidationResult(ValidationResult.VAL_INVALID_TYPE, VAL_ERR_TYPE);
        }
        return success();
    }

    /** 4000-VALIDATE-AMOUNT. */
    private ValidationResult validateAmount(String input) {
        BigDecimal amount = CobolText.toAmount(input);
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        if (amount.compareTo(VAL_MIN_AMOUNT) < 0 || amount.compareTo(VAL_MAX_AMOUNT) > 0) {
            return new ValidationResult(ValidationResult.VAL_INVALID_AMT, VAL_ERR_AMT);
        }
        return success();
    }

    private static ValidationResult success() {
        return new ValidationResult(ValidationResult.VAL_SUCCESS, "");
    }
}
