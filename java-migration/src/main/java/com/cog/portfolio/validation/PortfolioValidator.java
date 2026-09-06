package com.cog.portfolio.validation;

import static com.cog.portfolio.common.ValidationConstants.ID_PREFIX;
import static com.cog.portfolio.common.ValidationConstants.INVESTMENT_TYPES;
import static com.cog.portfolio.common.ValidationConstants.MAX_AMOUNT;
import static com.cog.portfolio.common.ValidationConstants.MIN_AMOUNT;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * PORTVALD.cbl. Each method is one paragraph (1000/2000/3000/4000-VALIDATE-*).
 * Inputs are the raw text the COBOL program received in LS-INPUT-VALUE (PIC X(50)).
 */
@Component
public class PortfolioValidator {

    /** 1000-VALIDATE-ID: 'PORT' + 4 numeric digits. */
    public ValidationResult validatePortfolioId(String value) {
        if (value == null || value.length() != 8 || !value.startsWith(ID_PREFIX)) {
            return new ValidationResult(ValidationResult.Code.INVALID_ID, ValidationResult.ERR_ID);
        }
        String numericPart = value.substring(4, 8);
        if (!isNumeric(numericPart)) {
            return new ValidationResult(ValidationResult.Code.INVALID_ID, ValidationResult.ERR_ID);
        }
        return ValidationResult.success();
    }

    /** 2000-VALIDATE-ACCOUNT: numeric and not all zeros. */
    public ValidationResult validateAccountNo(String value) {
        if (value == null || value.isEmpty() || !isNumeric(value) || isAllZeros(value)) {
            return new ValidationResult(ValidationResult.Code.INVALID_ACCOUNT, ValidationResult.ERR_ACCOUNT);
        }
        return ValidationResult.success();
    }

    /** 3000-VALIDATE-TYPE: STK / BND / MMF / ETF. */
    public ValidationResult validateInvestmentType(String value) {
        if (value == null || !INVESTMENT_TYPES.contains(value.trim())) {
            return new ValidationResult(ValidationResult.Code.INVALID_TYPE, ValidationResult.ERR_TYPE);
        }
        return ValidationResult.success();
    }

    /** 4000-VALIDATE-AMOUNT: VAL-MIN-AMOUNT <= amount <= VAL-MAX-AMOUNT. */
    public ValidationResult validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(MIN_AMOUNT) < 0 || amount.compareTo(MAX_AMOUNT) > 0) {
            return new ValidationResult(ValidationResult.Code.INVALID_AMOUNT, ValidationResult.ERR_AMOUNT);
        }
        return ValidationResult.success();
    }

    public ValidationResult validateAmount(String value) {
        try {
            return validateAmount(new BigDecimal(value.trim()));
        } catch (NumberFormatException | NullPointerException e) {
            return new ValidationResult(ValidationResult.Code.INVALID_AMOUNT, ValidationResult.ERR_AMOUNT);
        }
    }

    /** COBOL IS NUMERIC on a PIC X field: every character is a digit. */
    static boolean isNumeric(String value) {
        if (value.isEmpty()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAllZeros(String value) {
        return value.chars().allMatch(c -> c == '0');
    }
}
