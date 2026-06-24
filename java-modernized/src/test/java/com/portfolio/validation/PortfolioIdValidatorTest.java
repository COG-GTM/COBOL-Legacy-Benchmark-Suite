package com.portfolio.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for PortfolioIdValidator.
 * Verifies the validation rules from PORTVALD.cbl paragraph 1000-VALIDATE-ID:
 *   - Prefix must be 'PORT'
 *   - Followed by exactly 4 numeric digits
 */
class PortfolioIdValidatorTest {

    private PortfolioIdValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PortfolioIdValidator();
    }

    @Test
    void testValidId() {
        assertTrue(validator.isValid("PORT0001", null));
        assertTrue(validator.isValid("PORT9999", null));
        assertTrue(validator.isValid("PORT0000", null));
    }

    @Test
    void testInvalidPrefix() {
        assertFalse(validator.isValid("ACCT0001", null));
        assertFalse(validator.isValid("port0001", null));
        assertFalse(validator.isValid("XXXX0001", null));
    }

    @Test
    void testInvalidNumericPart() {
        assertFalse(validator.isValid("PORTABCD", null));
        assertFalse(validator.isValid("PORT00AB", null));
        assertFalse(validator.isValid("PORT 001", null));
    }

    @Test
    void testInvalidLength() {
        assertFalse(validator.isValid("PORT001", null));
        assertFalse(validator.isValid("PORT00001", null));
        assertFalse(validator.isValid("", null));
    }

    @Test
    void testNull() {
        assertFalse(validator.isValid(null, null));
    }
}
