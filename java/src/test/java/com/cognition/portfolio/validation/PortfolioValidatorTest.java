package com.cognition.portfolio.validation;

import java.math.BigDecimal;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Rule level tests. These state the behaviour the port is required to have in
 * the same terms as the migration document; the parity test proves the same
 * behaviour against the compiled COBOL.
 */
class PortfolioValidatorTest {

    private final PortfolioValidator validator = new PortfolioValidator();

    @Nested
    class Dispatch {

        @ParameterizedTest
        @ValueSource(chars = {'X', 'i', 'a', 't', 'm', ' ', '1'})
        void unknownValidationTypeReturnsInvalidIdCode(char type) {
            ValidationResult result = validator.validate(type, "PORT1234");
            assertEquals(PortValConstants.VAL_INVALID_ID, result.returnCode());
            assertEquals("Invalid validation type", result.trimmedErrorMessage());
        }
    }

    @Nested
    class PortfolioId {

        @ParameterizedTest
        @ValueSource(strings = {"PORT1234", "PORT0000", "PORTABCD", "XXXX1234", "port1234", "", "PORT"})
        void alwaysRejects(String input) {
            ValidationResult result = validator.validate('I', input);
            assertEquals(PortValConstants.VAL_INVALID_ID, result.returnCode());
            assertEquals("Invalid Portfolio ID format", result.trimmedErrorMessage());
        }

        @Test
        void becauseTheFourDigitSuffixIsClassTestedInsideATenByteField() {
            assertFalse(CobolAlphanumeric.isNumeric(
                    CobolAlphanumeric.toFixedLength("1234", PortValConstants.NUMERIC_CHECK_LENGTH)));
            assertTrue(CobolAlphanumeric.isNumeric("1234567890"));
        }
    }

    @Nested
    class AccountNumber {

        @Test
        void acceptsFiftyDigits() {
            ValidationResult result = validator.validate('A', "1".repeat(50));
            assertEquals(PortValConstants.VAL_SUCCESS, result.returnCode());
            assertEquals("", result.trimmedErrorMessage());
        }

        @Test
        void rejectsTheDocumentedTenDigitAccountNumberBecauseOfThePadSpaces() {
            assertEquals(PortValConstants.VAL_INVALID_ACCT, validator.validate('A', "1234567890").returnCode());
        }

        @Test
        void rejectsFiftyZeros() {
            assertEquals(PortValConstants.VAL_INVALID_ACCT, validator.validate('A', "0".repeat(50)).returnCode());
        }

        @Test
        void acceptsFiftyDigitsThatDifferFromZerosInOneByte() {
            assertEquals(PortValConstants.VAL_SUCCESS,
                    validator.validate('A', "0".repeat(49) + "1").returnCode());
        }

        @Test
        void truncatesInputLongerThanTheField() {
            assertEquals(PortValConstants.VAL_SUCCESS, validator.validate('A', "9".repeat(51)).returnCode());
        }
    }

    @Nested
    class InvestmentType {

        @ParameterizedTest
        @ValueSource(strings = {"STK", "BND", "MMF", "ETF"})
        void acceptsTheFourCodes(String code) {
            assertEquals(PortValConstants.VAL_SUCCESS, validator.validate('T', code).returnCode());
        }

        @ParameterizedTest
        @ValueSource(strings = {"stk", " STK", "STKX", "", "OPT", "Stk"})
        void rejectsEverythingElse(String code) {
            ValidationResult result = validator.validate('T', code);
            assertEquals(PortValConstants.VAL_INVALID_TYPE, result.returnCode());
            assertEquals("Invalid Investment Type", result.trimmedErrorMessage());
        }
    }

    @Nested
    class Amount {

        @ParameterizedTest
        @ValueSource(strings = {"1000.00", "-9999999999999.99", "9999999999999.99", "ABC", "", "$100"})
        void alwaysAccepts(String input) {
            ValidationResult result = validator.validate('M', input);
            assertEquals(PortValConstants.VAL_SUCCESS, result.returnCode());
            assertEquals("", result.trimmedErrorMessage());
        }

        @ParameterizedTest
        @CsvSource(delimiter = ';', value = {
                "'1000.00'      ; 1000.00",
                "'1000.999'     ; 1000.99",
                "'0.001'        ; 0.00",
                "'-500.25'      ; -500.25",
                "'1,000.00'     ; 1000.00",
                "' 42'          ; 42.00",
                "'4 2'          ; 42.00",
                "'.75'          ; 0.75",
                "'12.3-'        ; 0.00",
                "'1.2.3'        ; 0.00",
                "'1E3'          ; 0.00",
                "'99999999999999.99' ; 9999999999999.99",
        })
        void movesTheInputIntoS9x13V99LikeTheCobol(String input, String expected) {
            assertEquals(new BigDecimal(expected), validator.amountAsMoved(input));
        }

        @Test
        void keepsTheLowOrderDigitsWhenTheIntegerPartOverflows() {
            assertEquals(new BigDecimal("4567890123456.00"),
                    validator.amountAsMoved("1234567890123456"));
        }

        @Test
        void treatsAllSpacesAsZero() {
            assertEquals(new BigDecimal("0.00"), validator.amountAsMoved(""));
        }
    }
}
