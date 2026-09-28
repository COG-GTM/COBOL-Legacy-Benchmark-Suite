package com.cognition.portfolio.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Readable statements of PORTVALD's headline behaviour and of the Java input contract. The full
 * behavioural evidence is {@link PortvaldParityTest}; these tests exist so a reviewer can see the
 * most consequential legacy behaviours in one place.
 */
class PortfolioValidatorTest {

    private final PortfolioValidator validator = new PortfolioValidator();

    @Test
    void wellFormedPortfolioIdIsRejectedAsInTheLegacyProgram() {
        assertEquals(new ValidationResult(1, PortvalConstants.VAL_ERR_ID), validator.validate('I', "PORT1234"));
    }

    @Test
    void tenDigitAccountIsRejectedAndOnlyFiftyNonZeroDigitsPass() {
        assertEquals(2, validator.validate('A', "1234567890").returnCode());
        assertEquals(0, validator.validate('A', "0".repeat(49) + "1").returnCode());
        assertEquals(2, validator.validate('A', "0".repeat(50)).returnCode());
    }

    @Test
    void investmentTypeIsAnExactCaseSensitiveMatch() {
        assertEquals(new ValidationResult(0, PortvalConstants.NO_ERROR), validator.validate('T', "ETF"));
        assertEquals(new ValidationResult(3, PortvalConstants.VAL_ERR_TYPE), validator.validate('T', "etf"));
    }

    @Test
    void amountValidationNeverRejects() {
        assertEquals(0, validator.validate('M', "not a number").returnCode());
        assertEquals(0, validator.validate('M', "99999999999999999999").returnCode());
    }

    @Test
    void unknownValidationTypeReusesInvalidIdCode() {
        assertEquals(new ValidationResult(1, PortvalConstants.ERR_INVALID_VALIDATION_TYPE),
                validator.validate('Z', "PORT1234"));
    }

    @Test
    void inputsThatCannotExistInThePicX50FieldAreRejected() {
        assertThrows(NullPointerException.class, () -> validator.validate('T', null));
        assertThrows(IllegalArgumentException.class, () -> validator.validate('T', "X".repeat(51)));
        assertThrows(IllegalArgumentException.class, () -> validator.validate('T', "STK\u20AC"));
        assertThrows(IllegalArgumentException.class, () -> validator.validate('\u0130', "STK"));
    }

    @Test
    void amountMoveMatchesLibcobForRepresentativeInputs() {
        assertEquals("-123.45", move("  -123.45"));
        assertEquals("123.45", move("123.459"));
        assertEquals("2345678901234.00", move("12345678901234"));
        assertEquals("0.00", move("12A34"));
        assertEquals("1.23", move("1.234X"));
    }

    private static String move(String input) {
        return DisplayNumericMove.alphanumericToSignedDisplay(
                AlphanumericField.of(input, PortvalConstants.FIELD_LENGTH),
                PortvalConstants.TEMP_NUM_INTEGER_DIGITS, PortvalConstants.TEMP_NUM_SCALE).toPlainString();
    }
}
