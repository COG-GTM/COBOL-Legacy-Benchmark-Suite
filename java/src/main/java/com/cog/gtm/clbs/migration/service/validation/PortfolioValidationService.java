package com.cog.gtm.clbs.migration.service.validation;

import java.math.BigDecimal;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * Java port of the COBOL {@code PORTVALD} validation module.
 *
 * <p>Validation rules are derived from the {@code PORTVAL} copybook
 * and the {@code PORTVALD} program in {@code src/programs/portfolio/PORTVALD.cbl}.
 */
@Service
public class PortfolioValidationService {

    public static final int VAL_SUCCESS = 0;
    public static final int VAL_INVALID_ID = 1;
    public static final int VAL_INVALID_ACCT = 2;
    public static final int VAL_INVALID_TYPE = 3;
    public static final int VAL_INVALID_AMT = 4;

    public static final String ERR_ID = "Invalid Portfolio ID format";
    public static final String ERR_ACCT = "Invalid Account Number format";
    public static final String ERR_TYPE = "Invalid Investment Type";
    public static final String ERR_AMT = "Amount outside valid range";
    public static final String ERR_INVALID_TYPE = "Invalid validation type";

    public static final String ID_PREFIX = "PORT";

    public static final BigDecimal MIN_AMOUNT = new BigDecimal("-9999999999999.99");
    public static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999999.99");

    private static final Set<String> VALID_TYPES = Set.of("STK", "BND", "MMF", "ETF");

    /**
     * Validates the supplied value according to the requested validation type.
     *
     * @param validationType one of {@code I}, {@code A}, {@code T}, {@code M}
     * @param inputValue the value to validate
     * @return a {@link ValidationResult} with the COBOL-style return code and message
     */
    public ValidationResult validate(String validationType, String inputValue) {
        if (StringUtils.isBlank(validationType)) {
            return new ValidationResult(VAL_INVALID_ID, ERR_INVALID_TYPE);
        }

        String value = StringUtils.trimToEmpty(inputValue);
        return switch (validationType.toUpperCase()) {
            case "I" -> validatePortfolioId(value);
            case "A" -> validateAccountNumber(value);
            case "T" -> validateInvestmentType(value);
            case "M" -> validateAmount(value);
            default -> new ValidationResult(VAL_INVALID_ID, ERR_INVALID_TYPE);
        };
    }

    private ValidationResult validatePortfolioId(String value) {
        if (!StringUtils.startsWith(value, ID_PREFIX) || value.length() < ID_PREFIX.length() + 4) {
            return new ValidationResult(VAL_INVALID_ID, ERR_ID);
        }
        String digits = value.substring(ID_PREFIX.length(), ID_PREFIX.length() + 4);
        if (!StringUtils.isNumeric(digits)) {
            return new ValidationResult(VAL_INVALID_ID, ERR_ID);
        }
        return new ValidationResult(VAL_SUCCESS);
    }

    private ValidationResult validateAccountNumber(String value) {
        if (!StringUtils.isNumeric(value) || value.length() != 10 || isAllZeros(value)) {
            return new ValidationResult(VAL_INVALID_ACCT, ERR_ACCT);
        }
        return new ValidationResult(VAL_SUCCESS);
    }

    private ValidationResult validateInvestmentType(String value) {
        if (!VALID_TYPES.contains(value)) {
            return new ValidationResult(VAL_INVALID_TYPE, ERR_TYPE);
        }
        return new ValidationResult(VAL_SUCCESS);
    }

    private ValidationResult validateAmount(String value) {
        BigDecimal amount;
        try {
            amount = new BigDecimal(value);
        } catch (NumberFormatException | ArithmeticException e) {
            return new ValidationResult(VAL_INVALID_AMT, ERR_AMT);
        }

        if (amount.compareTo(MIN_AMOUNT) < 0 || amount.compareTo(MAX_AMOUNT) > 0) {
            return new ValidationResult(VAL_INVALID_AMT, ERR_AMT);
        }
        return new ValidationResult(VAL_SUCCESS);
    }

    private boolean isAllZeros(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) != '0') {
                return false;
            }
        }
        return true;
    }
}
