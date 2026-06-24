package com.portfolio.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for Portfolio ID format.
 * Translated from PORTVALD.cbl paragraph 1000-VALIDATE-ID:
 * <p>
 * The ID must:
 * 1. Start with the prefix 'PORT' (VAL-ID-PREFIX from PORTVAL.cpy)
 * 2. Be followed by exactly 4 numeric digits
 * 3. Total length = 8 characters
 */
public class PortfolioIdValidator implements ConstraintValidator<ValidPortfolioId, String> {

    private static final String PREFIX = "PORT";
    private static final int EXPECTED_LENGTH = 8;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return false;
        }
        if (value.length() != EXPECTED_LENGTH) {
            return false;
        }
        if (!value.startsWith(PREFIX)) {
            return false;
        }
        String numericPart = value.substring(PREFIX.length());
        return numericPart.chars().allMatch(Character::isDigit);
    }
}
