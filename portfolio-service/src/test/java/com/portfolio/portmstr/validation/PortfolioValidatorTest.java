package com.portfolio.portmstr.validation;

import com.portfolio.portmstr.dto.PortfolioRequest;
import com.portfolio.portmstr.dto.TransactionRequest;
import com.portfolio.portmstr.exception.PortfolioValidationException;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for PortfolioValidator.
 * Validates all rules from COBOL PORTVALD.cbl paragraphs 1000-4000.
 */
class PortfolioValidatorTest {

    private PortfolioValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PortfolioValidator();
    }

    @Nested
    @DisplayName("1000-VALIDATE-ID Tests")
    class ValidatePortfolioIdTests {

        @Test
        @DisplayName("Valid ID format PORT0001 passes")
        void validId() {
            assertDoesNotThrow(() -> validator.validatePortfolioId("PORT0001"));
        }

        @Test
        @DisplayName("Valid ID with max digits PORT9999")
        void validIdMax() {
            assertDoesNotThrow(() -> validator.validatePortfolioId("PORT9999"));
        }

        @Test
        @DisplayName("Null ID fails with code 1")
        void nullId() {
            PortfolioValidationException ex = assertThrows(PortfolioValidationException.class,
                    () -> validator.validatePortfolioId(null));
            assertEquals(1, ex.getValidationCode());
        }

        @Test
        @DisplayName("Short ID fails with code 1")
        void shortId() {
            PortfolioValidationException ex = assertThrows(PortfolioValidationException.class,
                    () -> validator.validatePortfolioId("PORT01"));
            assertEquals(1, ex.getValidationCode());
        }

        @Test
        @DisplayName("Wrong prefix fails with code 1")
        void wrongPrefix() {
            PortfolioValidationException ex = assertThrows(PortfolioValidationException.class,
                    () -> validator.validatePortfolioId("ACCT0001"));
            assertEquals(1, ex.getValidationCode());
        }

        @Test
        @DisplayName("Non-numeric suffix fails with code 1")
        void nonNumericSuffix() {
            PortfolioValidationException ex = assertThrows(PortfolioValidationException.class,
                    () -> validator.validatePortfolioId("PORTABCD"));
            assertEquals(1, ex.getValidationCode());
        }
    }

    @Nested
    @DisplayName("2000-VALIDATE-ACCOUNT Tests")
    class ValidateAccountTests {

        @Test
        @DisplayName("Valid 10-digit account number passes")
        void validAccount() {
            assertDoesNotThrow(() -> validator.validateAccountNumber("1234567890"));
        }

        @Test
        @DisplayName("Null account is allowed (optional field)")
        void nullAccount() {
            assertDoesNotThrow(() -> validator.validateAccountNumber(null));
        }

        @Test
        @DisplayName("Blank account is allowed (optional field)")
        void blankAccount() {
            assertDoesNotThrow(() -> validator.validateAccountNumber(""));
        }

        @Test
        @DisplayName("Short account number fails with code 2")
        void shortAccount() {
            PortfolioValidationException ex = assertThrows(PortfolioValidationException.class,
                    () -> validator.validateAccountNumber("12345"));
            assertEquals(2, ex.getValidationCode());
        }

        @Test
        @DisplayName("Non-numeric account fails with code 2")
        void nonNumericAccount() {
            PortfolioValidationException ex = assertThrows(PortfolioValidationException.class,
                    () -> validator.validateAccountNumber("123456789A"));
            assertEquals(2, ex.getValidationCode());
        }

        @Test
        @DisplayName("All-zeros account fails with code 2")
        void allZerosAccount() {
            PortfolioValidationException ex = assertThrows(PortfolioValidationException.class,
                    () -> validator.validateAccountNumber("0000000000"));
            assertEquals(2, ex.getValidationCode());
        }
    }

    @Nested
    @DisplayName("3000-VALIDATE-TYPE Tests")
    class ValidateInvestmentTypeTests {

        @Test
        @DisplayName("STK (Stock) is valid")
        void validStock() {
            assertDoesNotThrow(() -> validator.validateInvestmentType("STK"));
        }

        @Test
        @DisplayName("BND (Bond) is valid")
        void validBond() {
            assertDoesNotThrow(() -> validator.validateInvestmentType("BND"));
        }

        @Test
        @DisplayName("MMF (Money Market Fund) is valid")
        void validMoneyMarket() {
            assertDoesNotThrow(() -> validator.validateInvestmentType("MMF"));
        }

        @Test
        @DisplayName("ETF (Exchange Traded Fund) is valid")
        void validEtf() {
            assertDoesNotThrow(() -> validator.validateInvestmentType("ETF"));
        }

        @Test
        @DisplayName("Invalid type fails with code 3")
        void invalidType() {
            PortfolioValidationException ex = assertThrows(PortfolioValidationException.class,
                    () -> validator.validateInvestmentType("XYZ"));
            assertEquals(3, ex.getValidationCode());
        }

        @Test
        @DisplayName("Null type fails with code 3")
        void nullType() {
            PortfolioValidationException ex = assertThrows(PortfolioValidationException.class,
                    () -> validator.validateInvestmentType(null));
            assertEquals(3, ex.getValidationCode());
        }
    }

    @Nested
    @DisplayName("4000-VALIDATE-AMOUNT Tests")
    class ValidateAmountTests {

        @Test
        @DisplayName("Valid positive amount passes")
        void validPositiveAmount() {
            assertDoesNotThrow(() -> validator.validateAmount(new BigDecimal("1000.00")));
        }

        @Test
        @DisplayName("Valid negative amount passes")
        void validNegativeAmount() {
            assertDoesNotThrow(() -> validator.validateAmount(new BigDecimal("-500.00")));
        }

        @Test
        @DisplayName("Zero amount passes")
        void zeroAmount() {
            assertDoesNotThrow(() -> validator.validateAmount(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("Null amount passes (optional)")
        void nullAmount() {
            assertDoesNotThrow(() -> validator.validateAmount(null));
        }

        @Test
        @DisplayName("Max boundary amount passes")
        void maxBoundaryAmount() {
            assertDoesNotThrow(() -> validator.validateAmount(
                    new BigDecimal("9999999999999.99")));
        }

        @Test
        @DisplayName("Over-max amount fails with code 4")
        void overMaxAmount() {
            PortfolioValidationException ex = assertThrows(PortfolioValidationException.class,
                    () -> validator.validateAmount(new BigDecimal("10000000000000.00")));
            assertEquals(4, ex.getValidationCode());
        }

        @Test
        @DisplayName("Under-min amount fails with code 4")
        void underMinAmount() {
            PortfolioValidationException ex = assertThrows(PortfolioValidationException.class,
                    () -> validator.validateAmount(new BigDecimal("-10000000000000.00")));
            assertEquals(4, ex.getValidationCode());
        }
    }

    @Nested
    @DisplayName("Portfolio Request Validation Tests")
    class PortfolioRequestValidationTests {

        @Test
        @DisplayName("Valid portfolio request passes")
        void validRequest() {
            PortfolioRequest request = new PortfolioRequest(
                    "PORT0001", "1000000001", "John Doe", "I", "A",
                    new BigDecimal("100000"), new BigDecimal("50000"), "USD");
            assertDoesNotThrow(() -> validator.validatePortfolioRequest(request));
        }

        @Test
        @DisplayName("Missing client name fails")
        void missingClientName() {
            PortfolioRequest request = new PortfolioRequest(
                    "PORT0001", "1000000001", "", "I", "A",
                    BigDecimal.ZERO, BigDecimal.ZERO, "USD");
            assertThrows(PortfolioValidationException.class,
                    () -> validator.validatePortfolioRequest(request));
        }

        @Test
        @DisplayName("Invalid status code fails")
        void invalidStatus() {
            PortfolioRequest request = new PortfolioRequest(
                    "PORT0001", "1000000001", "John Doe", "I", "X",
                    BigDecimal.ZERO, BigDecimal.ZERO, "USD");
            assertThrows(PortfolioValidationException.class,
                    () -> validator.validatePortfolioRequest(request));
        }
    }

    @Nested
    @DisplayName("Transaction Request Validation Tests")
    class TransactionRequestValidationTests {

        @Test
        @DisplayName("Valid buy request passes")
        void validBuyRequest() {
            TransactionRequest request = new TransactionRequest(
                    "PORT0001", "INV0000001", "BU",
                    new BigDecimal("100"), new BigDecimal("50.00"),
                    new BigDecimal("5000.00"), "USD");
            assertDoesNotThrow(() -> validator.validateTransactionRequest(request));
        }

        @Test
        @DisplayName("Invalid transaction type fails")
        void invalidTransType() {
            TransactionRequest request = new TransactionRequest(
                    "PORT0001", "INV0000001", "XX",
                    new BigDecimal("100"), new BigDecimal("50.00"),
                    new BigDecimal("5000.00"), "USD");
            assertThrows(PortfolioValidationException.class,
                    () -> validator.validateTransactionRequest(request));
        }

        @Test
        @DisplayName("Zero quantity fails")
        void zeroQuantity() {
            TransactionRequest request = new TransactionRequest(
                    "PORT0001", "INV0000001", "BU",
                    BigDecimal.ZERO, new BigDecimal("50.00"),
                    new BigDecimal("5000.00"), "USD");
            assertThrows(PortfolioValidationException.class,
                    () -> validator.validateTransactionRequest(request));
        }

        @Test
        @DisplayName("Missing portfolio ID fails")
        void missingPortfolioId() {
            TransactionRequest request = new TransactionRequest(
                    "", "INV0000001", "BU",
                    new BigDecimal("100"), new BigDecimal("50.00"),
                    new BigDecimal("5000.00"), "USD");
            assertThrows(PortfolioValidationException.class,
                    () -> validator.validateTransactionRequest(request));
        }
    }
}
